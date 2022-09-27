package org.nitb.orchestrator2.tests.task

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Value
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.awaitility.Awaitility.await
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.api.getColumn
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.jetbrains.kotlinx.dataframe.type
import org.junit.jupiter.api.Assertions.*
import org.nitb.orchestrator2.task.enums.ExecutionStatus
import org.nitb.orchestrator2.task.enums.TaskStatus
import org.nitb.orchestrator2.task.model.StatusMessage
import org.nitb.orchestrator2.task.mq.impl.MQManager
import org.nitb.orchestrator2.task.mq.impl.rabbitmq.RabbitMQMessage
import org.nitb.orchestrator2.task.mq.model.MQMessage
import org.nitb.orchestrator2.task.util.TaskBuilder
import java.io.ByteArrayOutputStream
import java.lang.IllegalArgumentException
import java.math.BigInteger
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.GZIPOutputStream
import javax.jms.BytesMessage
import javax.jms.TextMessage
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

@Singleton
class CommonTaskTests {

    private val taskName = "task"

    private val testConsumerName = "test-consumer"

    private fun createTestConsumer(processed: AtomicBoolean, received: AtomicBoolean, customChecks: ((MQMessage<DataFrame<*>>) -> Unit)?) {
        queueManager.createQueue(testConsumerName)
        queueManager.createConsumers(testConsumerName, 1) { msg ->
            try {
                assertNotNull(msg)

                val message = when (msg) {
                    is TextMessage -> jsonMapper.readValue(msg.text, jsonMapper.typeFactory.constructParametricType(
                        MQMessage::class.java, DataFrame::class.java)) as MQMessage<DataFrame<*>>
                    is BytesMessage -> jsonMapper.readValue(msg.readUTF(), jsonMapper.typeFactory.constructParametricType(
                        MQMessage::class.java, DataFrame::class.java)) as MQMessage<DataFrame<*>>
                    is RabbitMQMessage -> jsonMapper.readValue(msg.body, jsonMapper.typeFactory.constructParametricType(
                        MQMessage::class.java, DataFrame::class.java)) as MQMessage<DataFrame<*>>
                    else -> throw IllegalArgumentException("Invalid message type received: ${msg!!::class.java.name}")
                }

                assertInstanceOf(DataFrame::class.java, message.message)

                customChecks?.invoke(message)

                processed.set(true)
            } finally {
                received.set(true)
            }
        }
    }

    private fun createManagerConsumer(executionStatus: AtomicReference<ExecutionStatus>) {
        queueManager.createQueue(managerName)
        queueManager.createConsumers(managerName, 1) { msg ->
            val message = when (msg) {
                is TextMessage -> jsonMapper.readValue(msg.text, StatusMessage::class.java)
                is BytesMessage -> jsonMapper.readValue(msg.readUTF(), StatusMessage::class.java)
                is RabbitMQMessage -> jsonMapper.readValue(msg.body, StatusMessage::class.java)
                else -> throw IllegalArgumentException("Invalid message type received: ${msg!!::class.java.name}")
            }

            assertInstanceOf(StatusMessage::class.java, message)
            executionStatus.set(message.executionStatus)
        }
    }

    private fun createMultiManagerConsumer(executionStatusList: MutableList<ExecutionStatus>) {
        queueManager.createQueue(managerName)
        queueManager.createConsumers(managerName, 1) { msg ->
            val message = when (msg) {
                is TextMessage -> jsonMapper.readValue(msg.text, StatusMessage::class.java)
                is BytesMessage -> jsonMapper.readValue(msg.readUTF(), StatusMessage::class.java)
                is RabbitMQMessage -> jsonMapper.readValue(msg.body, StatusMessage::class.java)
                else -> throw IllegalArgumentException("Invalid message type received: ${msg!!::class.java.name}")
            }

            assertInstanceOf(StatusMessage::class.java, message)
            executionStatusList.add(message.executionStatus)
        }
    }

    private fun purgeBefore() {
        try {
            queueManager.cancelConsumers()
            queueManager.purge(taskName)
            queueManager.purge(testConsumerName)
        } catch (e: IllegalArgumentException) {
            // do nothing, it's possible the first time you run tests queues have not yet been created
        }
    }

    private fun purgeBefore(vararg names: String) {
        try {
            queueManager.cancelConsumers()
            names.forEach { name ->
                queueManager.purge(name)
            }
            queueManager.purge(testConsumerName)
        } catch (e: IllegalArgumentException) {
            // do nothing, it's possible the first time you run tests queues have not yet been created
        }
    }

    private fun consumerTest(type: String, parameters: Map<String, Any?>, expectedExecutionStatus: ExecutionStatus, messageToSend: Any, customChecks: ((MQMessage<DataFrame<*>>) -> Unit)? = null) {
        purgeBefore()

        val received = AtomicBoolean(false)
        val processed = AtomicBoolean(false)
        val executionStatus = AtomicReference<ExecutionStatus>()

        createTestConsumer(processed, received, customChecks)
        createManagerConsumer(executionStatus)

        val task = taskBuilder.newTask(type, taskName, parameters)
        var destroyed = false

        try {
            await().until {
                task.status == TaskStatus.IDLE
            }

            queueManager.send(testConsumerName, taskName, messageToSend, UUID.randomUUID().toString())

            await().until {
                executionStatus.get() != null
            }

            assertEquals(expectedExecutionStatus, executionStatus.get())

            applicationContext.destroyBean(task)
            destroyed = true

            await().timeout(Duration.ofSeconds(30)).until {
                task.status == TaskStatus.STOPPED
            }
        } catch (e: Exception) {
            if (!destroyed) {
                applicationContext.destroyBean(task)
            }
            throw e
        }
    }

    private fun cyclicalTest(type: String, parameters: Map<String, Any?>, expectedExecutionStatus: ExecutionStatus, expectedSuccessExecutions: BigInteger, expectedErrorExecutions: BigInteger, customChecks: ((MQMessage<DataFrame<*>>) -> Unit)? = null) {
        purgeBefore()

        val received = AtomicBoolean(false)
        val processed = AtomicBoolean(false)
        val executionStatus = AtomicReference<ExecutionStatus>()

        createTestConsumer(processed, received, customChecks)
        createManagerConsumer(executionStatus)

        val task = taskBuilder.newTask(type, taskName, parameters)
        var destroyed = false

        try {
            await().timeout(Duration.ofSeconds(30)).until {
                task.totalLaunches >= (expectedSuccessExecutions + expectedErrorExecutions)
            }

            assertTrue(task.successLaunches >= expectedSuccessExecutions)
            assertTrue(task.errorLaunches >= expectedErrorExecutions)

            assertEquals(expectedExecutionStatus, executionStatus.get())

            applicationContext.destroyBean(task)
            destroyed = true

            await().until {
                task.status == TaskStatus.STOPPED
            }
        } catch (e: Exception) {
            if (!destroyed) {
                applicationContext.destroyBean(task)
            }
            throw e
        }
    }

    private fun multipleTaskTest(type: String, parameters: Map<String, Any?>, messageToSend: Any, customChecks: ((MQMessage<DataFrame<*>>) -> Unit)? = null) {
        val taskNames = arrayOf(1, 2).map { "$taskName $it" }

        purgeBefore(*taskNames.toTypedArray())

        val tasks = taskNames.map { name ->
            taskBuilder.newTask(type, name,  parameters)
        }
        var destroyed = false

        val received = AtomicBoolean(false)
        val processed = AtomicBoolean(false)
        val executionStatusList = Collections.synchronizedList<ExecutionStatus>(mutableListOf())

        createTestConsumer(processed, received, customChecks)
        createMultiManagerConsumer(executionStatusList)

        try {

            await().until {
                tasks.all { it.status == TaskStatus.IDLE }
            }

            taskNames.forEach { name ->
                queueManager.send(testConsumerName, name, messageToSend, UUID.randomUUID().toString())
            }

            await().until {
                executionStatusList.size >= 2
            }

            tasks.forEach {
                applicationContext.destroyBean(it)
            }
            destroyed = true

            await().timeout(Duration.ofSeconds(30)).until {
                tasks.all { it.status == TaskStatus.STOPPED }
            }
        } catch (e: Exception) {
            if (!destroyed) {
                tasks.forEach {
                    applicationContext.destroyBean(it)
                }
            }
            throw e
        }
    }

    fun jsonConsumerTest() {
        val messageToSend = dataFrameOf(
            Pair("col", listOf(1, 2, 3, 4, 5, 6, 7, 8)),
            Pair("col1", listOf("A", "B", "C", "D", "E", "F", "G", "H")),
            Pair("col2", listOf(3.4, 5.6, 7.9, 2.3, 5.6, 2.4, 5.4, 0.3)),
            Pair("col3", listOf(3.4f, 5.6f, 7.9f, 2.3f, 5.6f, 2.4f, 5.4f, 0.3f)),
            Pair("col4", listOf(arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5))),
            Pair("col5", listOf(listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5))),
            Pair("col6", listOf(mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1)))
        )

        consumerTest("JSON-CONSUMER", mapOf("mq.internal" to true, "task.receivers" to arrayOf(testConsumerName)),
            ExecutionStatus.FINISHED, messageToSend) { message ->
            assertNotNull(message.message)
            assertEquals(7, message.message?.columnsCount())
            assertEquals(8, message.message?.rowsCount())
            assertTrue(message.message?.containsColumn("col") ?: false)
            assertTrue(message.message?.containsColumn("col1") ?: false)
            assertTrue(message.message?.containsColumn("col2") ?: false)
            assertTrue(message.message?.containsColumn("col3") ?: false)
            assertTrue(message.message?.containsColumn("col4") ?: false)
            assertTrue(message.message?.containsColumn("col5") ?: false)
            assertTrue(message.message?.containsColumn("col6") ?: false)
            assertTrue(message.message?.getColumn("col")?.type == typeOf<Int>())
            assertTrue(message.message?.getColumn("col1")?.type == typeOf<String>())
            assertTrue(message.message?.getColumn("col2")?.type == typeOf<Double>())
            assertTrue(message.message?.getColumn("col3")?.type == typeOf<Float>())
            assertTrue(message.message?.getColumn("col4")?.type?.isSubtypeOf(typeOf<Array<*>>()) ?: false)
            assertTrue(message.message?.getColumn("col5")?.type?.isSubtypeOf(typeOf<List<*>>()) ?: false)
            assertTrue(message.message?.getColumn("col6")?.type?.isSubtypeOf(typeOf<Map<*, *>>()) ?: false)
            assertArrayEquals(arrayOf(1, 2, 3, 4, 5, 6, 7, 8), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf("A", "B", "C", "D", "E", "F", "G", "H"), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(3.4, 5.6, 7.9, 2.3, 5.6, 2.4, 5.4, 0.3), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(3.4f, 5.6f, 7.9f, 2.3f, 5.6f, 2.4f, 5.4f, 0.3f), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5)), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5)), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1)), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
        }
    }

    fun scriptableConsumerTest() {
        data class Example(
            val names: List<String>,
            val ages: List<Int>,
            val heights: List<Double>
        )

        val messageToSend = jsonMapper.writeValueAsString(Example(
            listOf("Jason", "Mary", "Ann", "Gregory"),
            listOf(15, 34, 76, 21),
            listOf(1.4, 1.7, 1.6, 1.9)
        ))

        val script = """
            logger.info("New message received from: " + sender);
            logger.info(inputMessage);
            
            var parsedMessage = JSON.parse(inputMessage);
          
            if (inputMessage === null) {
                throw "Invalid null message received"
            }
            
            var result = InteroperabilityUtils.createDataFrameFromColumns(['names', 'ages', 'heights'], [['John', 'Mary', 'George', 'Ann'], [18, 23, 17, 21], [1.7, 1.6, 1.9, 1.5]]);
        """.trimIndent()

        consumerTest("SCRIPTABLE-CONSUMER", mapOf("mq.internal" to true, "task.receivers" to arrayOf(testConsumerName),
            "mq.script" to script), ExecutionStatus.FINISHED, messageToSend) { message ->
            assertNotNull(message.message)
            assertEquals(3, message.message?.columnsCount())
            assertEquals(4, message.message?.rowsCount())
            assertTrue(message.message?.containsColumn("names") ?: false)
            assertTrue(message.message?.containsColumn("ages") ?: false)
            assertTrue(message.message?.containsColumn("heights") ?: false)
            assertTrue(message.message?.getColumn("names")?.type == typeOf<String>())
            assertTrue(message.message?.getColumn("ages")?.type == typeOf<Int>())
            assertTrue(message.message?.getColumn("heights")?.type == typeOf<Double>())
            assertArrayEquals(arrayOf("John", "Mary", "George", "Ann"), message.message?.getColumn("names")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(18, 23, 17, 21), message.message?.getColumn("ages")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(1.7, 1.6, 1.9, 1.5), message.message?.getColumn("heights")?.values()?.toList()?.toTypedArray())
        }
    }

    fun scriptableCyclicalTest() {
        val script = """
            var result = InteroperabilityUtils.createDataFrameFromColumns(["names", "ages", "heights"], [[], [], []]);
        """.trimIndent()

        cyclicalTest("SCRIPTABLE-CYCLICAL", mapOf("task.receivers" to arrayOf("tests-result"),
            "cyclical.fixed-delay" to 500, "cyclical.script" to script), ExecutionStatus.FINISHED, BigInteger.valueOf(5), BigInteger.valueOf(0)) { message ->
            assertNotNull(message.message)
            assertEquals(3, message.message?.columnsCount())
            assertEquals(4, message.message?.rowsCount())
            assertTrue(message.message?.containsColumn("names") ?: false)
            assertTrue(message.message?.containsColumn("ages") ?: false)
            assertTrue(message.message?.containsColumn("heights") ?: false)
            assertTrue(message.message?.getColumn("names")?.type == typeOf<String>())
            assertTrue(message.message?.getColumn("ages")?.type == typeOf<Int>())
            assertTrue(message.message?.getColumn("heights")?.type == typeOf<Double>())
            assertArrayEquals(arrayOf("John", "Mary", "George", "Ann"), message.message?.getColumn("names")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(18, 23, 17, 21), message.message?.getColumn("ages")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(1.7, 1.6, 1.9, 1.5), message.message?.getColumn("heights")?.values()?.toList()?.toTypedArray())
        }
    }

    fun scriptableCyclicalFailedTest() {
        val script = """
            var result = InteroperabilityUtils.createDataFrameFromColumns(["names", "ages", "heights"], [[], [], []]);
        """.trimIndent()

        val checkScript = """
            var result = executionResult?.rowsCount() > 0
        """.trimIndent()

        cyclicalTest("SCRIPTABLE-CYCLICAL", mapOf("task.receivers" to arrayOf("tests-result"),
            "cyclical.fixed-delay" to 500, "cyclical.script" to script, "cyclical.check-result-script" to checkScript),
            ExecutionStatus.FINISHED_WITH_ERRORS, BigInteger.valueOf(0), BigInteger.valueOf(5))
    }

    fun csvConsumerTest(compressed: Boolean = false) {
        val messageToSend = """
            name;age;salary
            Adolfa;24;15000.0
            Emerico;35;28000
            Indalecia;20;13000
            Nepomuceno;60;60000
        """.trimIndent().let {  msg ->
            if (compressed) {
                ByteArrayOutputStream().use { stream ->
                    GZIPOutputStream(stream).bufferedWriter(Charsets.UTF_8).use { gzip ->
                        gzip.write(msg)
                    }
                    stream.toByteArray()
                }
            } else msg.toByteArray()
        }

        consumerTest("CSV-CONSUMER", mapOf("mq.internal" to true, "task.receivers" to arrayOf(testConsumerName),
            "csv.delimiter" to ';', "csv.language-code" to "us", "csv.compressed" to compressed), ExecutionStatus.FINISHED, messageToSend) { message ->
            assertNotNull(message.message)
            assertEquals(3, message.message?.columnsCount())
            assertEquals(4, message.message?.rowsCount())
            assertTrue(message.message?.containsColumn("name") ?: false)
            assertTrue(message.message?.containsColumn("age") ?: false)
            assertTrue(message.message?.containsColumn("salary") ?: false)
            assertTrue(message.message?.getColumn("name")?.type == typeOf<String>())
            assertTrue(message.message?.getColumn("age")?.type == typeOf<Int>())
            assertTrue(message.message?.getColumn("salary")?.type == typeOf<Double>())
            assertArrayEquals(arrayOf("Adolfa", "Emerico", "Indalecia", "Nepomuceno"), message.message?.getColumn("name")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(24, 35, 20, 60), message.message?.getColumn("age")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(15000.0, 28000.0, 13000.0, 60000.0), message.message?.getColumn("salary")?.values()?.toList()?.toTypedArray())
        }
    }

    fun multiJsonConsumerTest(){
        val messageToSend = dataFrameOf(
            Pair("col", listOf(1, 2, 3, 4, 5, 6, 7, 8)),
            Pair("col1", listOf("A", "B", "C", "D", "E", "F", "G", "H")),
            Pair("col2", listOf(3.4, 5.6, 7.9, 2.3, 5.6, 2.4, 5.4, 0.3)),
            Pair("col3", listOf(3.4f, 5.6f, 7.9f, 2.3f, 5.6f, 2.4f, 5.4f, 0.3f)),
            Pair("col4", listOf(arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5))),
            Pair("col5", listOf(listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5))),
            Pair("col6", listOf(mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1)))
        )

        multipleTaskTest("JSON-CONSUMER", mapOf("mq.internal" to true, "task.receivers" to arrayOf(testConsumerName)), messageToSend) { message ->
            assertNotNull(message.message)
            assertEquals(7, message.message?.columnsCount())
            assertEquals(8, message.message?.rowsCount())
            assertTrue(message.message?.containsColumn("col") ?: false)
            assertTrue(message.message?.containsColumn("col1") ?: false)
            assertTrue(message.message?.containsColumn("col2") ?: false)
            assertTrue(message.message?.containsColumn("col3") ?: false)
            assertTrue(message.message?.containsColumn("col4") ?: false)
            assertTrue(message.message?.containsColumn("col5") ?: false)
            assertTrue(message.message?.containsColumn("col6") ?: false)
            assertTrue(message.message?.getColumn("col")?.type == typeOf<Int>())
            assertTrue(message.message?.getColumn("col1")?.type == typeOf<String>())
            assertTrue(message.message?.getColumn("col2")?.type == typeOf<Double>())
            assertTrue(message.message?.getColumn("col3")?.type == typeOf<Float>())
            assertTrue(message.message?.getColumn("col4")?.type?.isSubtypeOf(typeOf<Array<*>>()) ?: false)
            assertTrue(message.message?.getColumn("col5")?.type?.isSubtypeOf(typeOf<List<*>>()) ?: false)
            assertTrue(message.message?.getColumn("col6")?.type?.isSubtypeOf(typeOf<Map<*, *>>()) ?: false)
            assertArrayEquals(arrayOf(1, 2, 3, 4, 5, 6, 7, 8), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf("A", "B", "C", "D", "E", "F", "G", "H"), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(3.4, 5.6, 7.9, 2.3, 5.6, 2.4, 5.4, 0.3), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(3.4f, 5.6f, 7.9f, 2.3f, 5.6f, 2.4f, 5.4f, 0.3f), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5)), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5)), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1)), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
        }
    }

    fun excelConsumerTest() {
        val df = dataFrameOf(
            Pair("col", listOf(1, 2, 3, 4, 5, 6, 7, 8)),
            Pair("col1", listOf("A", "B", "C", "D", "E", "F", "G", "H")),
            Pair("col2", listOf(3.4, 5.6, 7.9, 2.3, 5.6, 2.4, 5.4, 0.3)),
            Pair("col3", listOf(3.4f, 5.6f, 7.9f, 2.3f, 5.6f, 2.4f, 5.4f, 0.3f)),
            Pair("col4", listOf(arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5))),
            Pair("col5", listOf(listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5))),
            Pair("col6", listOf(mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1)))
        )

        val messageToSend = ByteArrayOutputStream().let { stream ->
            df.writeExcel(stream, factory = { WorkbookFactory.create(true) })
            stream.toByteArray()
        }

        consumerTest("EXCEL-CONSUMER", mapOf("mq.internal" to true, "task.receivers" to arrayOf(testConsumerName)),
            ExecutionStatus.FINISHED, messageToSend) { message ->
            assertNotNull(message.message)
            assertEquals(7, message.message?.columnsCount())
            assertEquals(8, message.message?.rowsCount())
            assertTrue(message.message?.containsColumn("col") ?: false)
            assertTrue(message.message?.containsColumn("col1") ?: false)
            assertTrue(message.message?.containsColumn("col2") ?: false)
            assertTrue(message.message?.containsColumn("col3") ?: false)
            assertTrue(message.message?.containsColumn("col4") ?: false)
            assertTrue(message.message?.containsColumn("col5") ?: false)
            assertTrue(message.message?.containsColumn("col6") ?: false)
            assertTrue(message.message?.getColumn("col")?.type == typeOf<Int>())
            assertTrue(message.message?.getColumn("col1")?.type == typeOf<String>())
            assertTrue(message.message?.getColumn("col2")?.type == typeOf<Double>())
            assertTrue(message.message?.getColumn("col3")?.type == typeOf<Float>())
            assertTrue(message.message?.getColumn("col4")?.type?.isSubtypeOf(typeOf<Array<*>>()) ?: false)
            assertTrue(message.message?.getColumn("col5")?.type?.isSubtypeOf(typeOf<List<*>>()) ?: false)
            assertTrue(message.message?.getColumn("col6")?.type?.isSubtypeOf(typeOf<Map<*, *>>()) ?: false)
            assertArrayEquals(arrayOf(1, 2, 3, 4, 5, 6, 7, 8), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf("A", "B", "C", "D", "E", "F", "G", "H"), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(3.4, 5.6, 7.9, 2.3, 5.6, 2.4, 5.4, 0.3), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(3.4f, 5.6f, 7.9f, 2.3f, 5.6f, 2.4f, 5.4f, 0.3f), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5)), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5)), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
            assertArrayEquals(arrayOf(mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1)), message.message?.getColumn("col")?.values()?.toList()?.toTypedArray())
        }
    }

    @Inject
    private lateinit var applicationContext: ApplicationContext

    @Inject
    private lateinit var jsonMapper: ObjectMapper

    @Inject
    private lateinit var queueManager: MQManager<*, *, *>

    @Inject
    private lateinit var taskBuilder: TaskBuilder

    @Value("\${orchestrator.mq.manager.queue}")
    private lateinit var managerName: String
}
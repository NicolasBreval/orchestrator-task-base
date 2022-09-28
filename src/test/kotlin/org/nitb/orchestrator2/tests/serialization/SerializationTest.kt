package org.nitb.orchestrator2.tests.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ParserOptions
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.api.getColumn
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.nitb.orchestrator2.task.mq.model.MQMessage
import org.nitb.orchestrator2.task.parameters.TaskParameters
import org.nitb.orchestrator2.task.parameters.consumer.ConsumerTaskParameters
import org.nitb.orchestrator2.task.parameters.consumer.ScriptableConsumerTaskParameters
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.*
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

@MicronautTest
class SerializationTest {

    // region DATAFRAME

    @Test
    @OptIn(ExperimentalStdlibApi::class)
    fun dataframeSerializationTest() {
        val df = dataFrameOf(
            Pair("col", listOf(1, 2, 3, 4, 5, 6, 7, 8)),
            Pair("col1", listOf("A", "B", "C", "D", "E", "F", "G", "H")),
            Pair("col3", listOf(3.4, 5.6, 7.9, 2.3, 5.6, 2.4, 5.4, 0.3)),
            Pair("col4", listOf(3.4f, 5.6f, 7.9f, 2.3f, 5.6f, 2.4f, 5.4f, 0.3f)),
            Pair("col5", listOf(arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5), arrayOf(1,2,3,4,5))),
            Pair("col6", listOf(listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5), listOf(1,2,3,4,5))),
            Pair("col7", listOf(mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1), mapOf("A" to 1)))
        )
        val serialized = jsonMapper.writeValueAsString(df)
        val deserialized = jsonMapper.readValue(serialized, DataFrame::class.java)

        assertEquals(deserialized.columnTypes()[0], Int::class.createType())
        assertEquals(deserialized.columnTypes()[1], String::class.createType())
        assertEquals(deserialized.columnTypes()[2], Double::class.createType())
        assertEquals(deserialized.columnTypes()[3], Float::class.createType())
        assertTrue((deserialized.columnTypes()[4].javaType as Class<*>).isArray)
        assertTrue(deserialized.columnTypes()[5].isSubtypeOf(typeOf<List<*>>()))
        assertTrue(deserialized.columnTypes()[6].isSubtypeOf(typeOf<Map<*, *>>()))
    }

    @Test
    fun csvDataframeDeserializationTest() {
        val serialized = """
            A,B,C,D
            12,tuv,0.12,true
            41,xyz,3.6,
            89,abc,7.1,false
        """.trimIndent()

        val df = serialized.byteInputStream().use {
            DataFrame.readCSV(it, parserOptions = ParserOptions(locale = Locale("us")))
        }

        assertEquals(4, df.columnsCount())
        assertEquals(3, df.rowsCount())
        assertTrue(df.containsColumn("A"))
        assertTrue(df.containsColumn("B"))
        assertTrue(df.containsColumn("C"))
        assertTrue(df.containsColumn("D"))
        assertArrayEquals(arrayOf(12, 41, 89), df.getColumn("A").values().toList().toTypedArray())
        assertArrayEquals(arrayOf("tuv", "xyz", "abc"), df.getColumn("B").values().toList().toTypedArray())
        assertArrayEquals(arrayOf(0.12, 3.6, 7.1), df.getColumn("C").values().toList().toTypedArray())
        assertArrayEquals(arrayOf(true, null, false), df.getColumn("D").values().toList().toTypedArray())
    }

    // endregion

    //region TASK PARAMETERS

    @Test
    fun taskParametersAllPropertiesDeserializationTest() {
        val serialized = """
            {
                "task.timeout": 5000,
                "task.receivers": ["receiver1", "receiver2"],
                "task.concurrency": 4
            }
        """.trimIndent()

        val deserialized = jsonMapper.readValue(serialized, TaskParameters::class.java)

        assertEquals(5000, deserialized.timeout)
        assertArrayEquals(arrayOf("receiver1", "receiver2"), deserialized.receivers)
        assertEquals(4, deserialized.concurrency)
    }

    @Test
    fun taskParametersNonPropertiesDeserializationTest() {
        val serialized = """
            {}
        """.trimIndent()

        val deserialized = jsonMapper.readValue(serialized, TaskParameters::class.java)

        assertEquals(-1, deserialized.timeout)
        assertArrayEquals(emptyArray(), deserialized.receivers)
        assertEquals(1, deserialized.concurrency)
    }

    @Test
    fun taskParametersDefaultPropertiesSerializationTest() {
        val original = TaskParameters()

        val serialized = jsonMapper.writeValueAsString(original)

        assertTrue(""""task\.timeout" *: *-1""".toRegex().containsMatchIn(serialized))
        assertTrue(""""task\.concurrency" *: *-*1""".toRegex().containsMatchIn(serialized))
    }

    @Test
    fun taskParametersAllPropertiesSetSerializationTest() {
        val original = TaskParameters(6000, arrayOf("receiver1", "receiver2"), 7)

        val serialized = jsonMapper.writeValueAsString(original)

        assertTrue(""""task\.timeout" *: *6000""".toRegex().containsMatchIn(serialized))
        assertTrue(""""task\.receivers" *: *\["receiver1" *, *"receiver2"]""".toRegex().containsMatchIn(serialized))
        assertTrue(""""task\.concurrency" *: *7""".toRegex().containsMatchIn(serialized))
    }

    // endregion

    // region MQ CONSUMER TASK PARAMETERS

    @Test
    fun mqConsumerTaskParametersAllPropertiesDeserializationTest() {
        val serialized = """
            {
                "task.timeout": 5000,
                "task.receivers": ["receiver1", "receiver2"],
                "task.concurrency": 4,
                "mq.internal": false,
                "mq.type": "activemq",
                "mq.uri": "failover(tcp://localhost:61616)",
                "mq.username": "admin",
                "mq.password": "admin"
            }
        """.trimIndent()

        val deserialized = jsonMapper.readValue(serialized, ConsumerTaskParameters::class.java)

        assertEquals(5000, deserialized.timeout)
        assertArrayEquals(arrayOf("receiver1", "receiver2"), deserialized.receivers)
        assertEquals(4, deserialized.concurrency)
        assertFalse(deserialized.internal)
        assertEquals("activemq", deserialized.type)
        assertEquals("failover(tcp://localhost:61616)", deserialized.uri)
        assertEquals("admin", deserialized.username)
        assertEquals("admin", deserialized.password)
    }

    @Test
    fun mqConsumerTaskParametersNonPropertiesDeserializationTest() {
        val serialized = """
            {}
        """.trimIndent()

        val deserialized = jsonMapper.readValue(serialized, ConsumerTaskParameters::class.java)

        assertEquals(-1, deserialized.timeout)
        assertArrayEquals(emptyArray(), deserialized.receivers)
        assertEquals(1, deserialized.concurrency)
        assertTrue(deserialized.internal)
        assertNull(deserialized.type)
        assertNull(deserialized.uri)
        assertNull(deserialized.username)
        assertNull(deserialized.password)
    }

    @Test
    fun mqConsumerTaskParametersDefaultPropertiesSerializationTest() {
        val original = ConsumerTaskParameters()

        val serialized = jsonMapper.writeValueAsString(original)

        assertTrue(""""task\.timeout" *: *-1""".toRegex().containsMatchIn(serialized))
        assertTrue(""""task\.concurrency" *: *-*1""".toRegex().containsMatchIn(serialized))
        assertTrue(""""mq\.internal" *: *true""".toRegex().containsMatchIn(serialized))
    }

    @Test
    fun mqConsumerTaskParametersAllPropertiesSetSerializationTest() {
        val original = ConsumerTaskParameters(6000, arrayOf("receiver1", "receiver2"), 7, false, "rabbitmq", "amqp://localhost:5672/vhost", "guest", "guest")

        val serialized = jsonMapper.writeValueAsString(original)

        assertTrue(""""task\.timeout" *: *6000""".toRegex().containsMatchIn(serialized))
        assertTrue(""""task\.receivers" *: *\["receiver1" *, *"receiver2"]""".toRegex().containsMatchIn(serialized))
        assertTrue(""""task\.concurrency" *: *7""".toRegex().containsMatchIn(serialized))
        assertTrue(""""mq\.internal" *: *false""".toRegex().containsMatchIn(serialized))
        assertTrue(""""mq\.type" *: *"rabbitmq"""".toRegex().containsMatchIn(serialized))
        assertTrue(""""mq\.uri" *: *"amqp://localhost:5672/vhost"""".toRegex().containsMatchIn(serialized))
        assertTrue(""""mq\.username" *: *"guest"""".toRegex().containsMatchIn(serialized))
        assertTrue(""""mq\.password" *: *"guest"""".toRegex().containsMatchIn(serialized))
    }

    //endregion

    // region MQ SCRIPTABLE CONSUMER TASK PARAMETERS

    @Test
    fun mqScriptableConsumerTaskParametersAllPropertiesDeserializationTest() {
        val serialized = """
            {
                "task.timeout": 5000,
                "task.receivers": ["receiver1", "receiver2"],
                "task.concurrency": 4,
                "mq.internal": false,
                "mq.type": "activemq",
                "mq.uri": "failover(tcp://localhost:61616)",
                "mq.username": "admin",
                "mq.password": "admin",
                "mq.script": "MY JS SCRIPT"
            }
        """.trimIndent()

        val deserialized = jsonMapper.readValue(serialized, ScriptableConsumerTaskParameters::class.java)

        assertEquals(5000, deserialized.timeout)
        assertArrayEquals(arrayOf("receiver1", "receiver2"), deserialized.receivers)
        assertEquals(4, deserialized.concurrency)
        assertFalse(deserialized.internal)
        assertEquals("activemq", deserialized.type)
        assertEquals("failover(tcp://localhost:61616)", deserialized.uri)
        assertEquals("admin", deserialized.username)
        assertEquals("admin", deserialized.password)
        assertEquals("MY JS SCRIPT", deserialized.script)
    }

    @Test
    fun mqScriptableConsumerTaskParametersNonPropertiesDeserializationTest() {
        val serialized = """
            {}
        """.trimIndent()

        org.junit.jupiter.api.assertThrows<MissingKotlinParameterException> {
            jsonMapper.readValue(serialized, ScriptableConsumerTaskParameters::class.java)
        }
    }

    @Test
    fun mqScriptableConsumerTaskParametersDefaultPropertiesSerializationTest() {
        val original = ScriptableConsumerTaskParameters(script = "MY JS SCRIPT")

        val serialized = jsonMapper.writeValueAsString(original)

        assertTrue(""""task\.timeout" *: *-1""".toRegex().containsMatchIn(serialized))
        assertTrue(""""task\.concurrency" *: *-*1""".toRegex().containsMatchIn(serialized))
        assertTrue(""""mq\.internal" *: *true""".toRegex().containsMatchIn(serialized))
        assertTrue(""""mq\.script" *: *"MY JS SCRIPT"""".toRegex().containsMatchIn(serialized))
    }

    @Test
    fun mqScriptableConsumerTaskParametersAllPropertiesSetSerializationTest() {
        val original = ScriptableConsumerTaskParameters(6000, arrayOf("receiver1", "receiver2"), 7, false, "rabbitmq", "amqp://localhost:5672/vhost", "guest", "guest", "MY JS SCRIPT")

        val serialized = jsonMapper.writeValueAsString(original)

        assertTrue(""""task\.timeout" *: *6000""".toRegex().containsMatchIn(serialized))
        assertTrue(""""task\.receivers" *: *\["receiver1" *, *"receiver2"]""".toRegex().containsMatchIn(serialized))
        assertTrue(""""task\.concurrency" *: *7""".toRegex().containsMatchIn(serialized))
        assertTrue(""""mq\.internal" *: *false""".toRegex().containsMatchIn(serialized))
        assertTrue(""""mq\.type" *: *"rabbitmq"""".toRegex().containsMatchIn(serialized))
        assertTrue(""""mq\.uri" *: *"amqp://localhost:5672/vhost"""".toRegex().containsMatchIn(serialized))
        assertTrue(""""mq\.username" *: *"guest"""".toRegex().containsMatchIn(serialized))
        assertTrue(""""mq\.password" *: *"guest"""".toRegex().containsMatchIn(serialized))
        assertTrue(""""mq\.script" *: *"MY JS SCRIPT"""".toRegex().containsMatchIn(serialized))
    }

    // endregion

    // region MQ MESSAGE

    @Test
    fun mqMessageSerializationTest() {
        val original = MQMessage("Hello!!!", "test", OffsetDateTime.now())

        val serialized = jsonMapper.writeValueAsString(original)

        LoggerFactory.getLogger(this::class.java).info(serialized)
    }

    // endregion

    // region COMMON

    @Inject
    private lateinit var jsonMapper: ObjectMapper

    // endregion
}
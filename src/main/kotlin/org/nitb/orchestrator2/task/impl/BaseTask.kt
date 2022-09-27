package org.nitb.orchestrator2.task.impl

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Prototype
import io.micronaut.context.annotation.Requires
import io.micronaut.core.reflect.GenericTypeUtils
import kotlinx.coroutines.*
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.nitb.orchestrator2.task.enums.ExecutionStatus
import org.nitb.orchestrator2.task.exception.InitializationException
import org.nitb.orchestrator2.task.model.TaskInfo
import org.nitb.orchestrator2.task.mq.impl.MQManager
import org.nitb.orchestrator2.task.mq.impl.rabbitmq.RabbitMQMessage
import org.nitb.orchestrator2.task.mq.model.MQMessage
import org.nitb.orchestrator2.task.parameters.TaskParameters
import org.nitb.orchestrator2.task.enums.TaskStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.time.LocalDateTime
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.jms.BytesMessage
import javax.jms.TextMessage

/**
 * Basic definition of a task. A task is an object with a custom logic that can receive a message from a MQ queue, produce
 * messages periodically, or more possible implementations based on developer's needs
 */
@Prototype
@Requires(bean = MQManager::class)
abstract class BaseTask<P: TaskParameters, T>(
    /**
     * Name of task, it's used to distinguish two tasks and to name some external elements, like MQ queues
     */
    @Parameter
    val name: String,
    /**
     * List of parameters that task needs
     */
    @Parameter
    private val parameters: Map<String, Any>,
    /**
     * Class of parameters, to construct parameters object correctly
     */
    parametersClass: Class<P>,
    /**
     * ApplicationContext object used to access to Micronaut beans. This argument is automatically added by Micronaut beans constructor
     */
    applicationContext: ApplicationContext,
    /**
     * If is true, don't create queues to receive data. This option is commonly used if your task is periodical,
     * or if your task receives data from another external component, like a database, a web service, ...
     */
    private val disableInputQueue: Boolean = false
) {

    /**
     * Number of times that this task has been started. Is considered that a task has been started when changes their status from STOPPED to IDLE or RUNNING
     */
    val starts: BigInteger get() = _starts

    /**
     * Number of times that this task has been stopped. Is that a task has been stopped when changes their status to STOPPED
     */
    val stops: BigInteger get() = _stops

    /**
     * Number of all times that task was executed, irrespective execution was success or not
     */
    val totalLaunches: BigInteger get() = _successLaunches + _errorLaunches

    /**
     * Number of all times that task was executed successfully
     */
    val successLaunches: BigInteger get() = _successLaunches

    /**
     * Number of all times that task was executed with a wrong result
     */
    val errorLaunches: BigInteger get() = _errorLaunches

    /**
     * Number of all times that task was executed and aborts their execution due to an exception
     */
    val abortLaunches: BigInteger get() = _abortLaunches

    /**
     * Number of all times that task was executed and aborts their execution due to a timeout
     */
    val timeoutLaunches: BigInteger get() = _timeoutLaunches

    /**
     * Current status of task, to check if currently is processing anything or not
     */
    val status: TaskStatus get() = _status

    /**
     * Last date when task was launched. If task wasn't launched ever, their value is null
     */
    val lastLaunchDate: LocalDateTime? get() = if (this::_lastLaunchDate.isInitialized) _lastLaunchDate else null

    /**
     * Returns a summary of current task status, with their execution status, number of starts, stops, launches, ...
     */
    val info: TaskInfo get() = TaskInfo(
        _starts, _stops, _successLaunches, _errorLaunches, _status, implementationTime
    )

    /**
     * Defines dataflow of a task, with all steps from a new execution's start to their finalization
     */
    @OptIn(DelicateCoroutinesApi::class)
    protected suspend fun launch(executionId: String, inputMessage: T?, sender: String, dispatchTime: LocalDateTime) {
        try {
            _status = TaskStatus.RUNNING
            logger.debug("Status changed: $_status")

            var result: DataFrame<*>? = null

            var executionStatus: ExecutionStatus = ExecutionStatus.FINISHED

            logger.debug("Task executing for id $executionId")

            job = GlobalScope.launch {
                try {
                    val start = System.currentTimeMillis()
                    result = onLaunch(inputMessage, sender, dispatchTime)
                    logger.debug("Execution $executionId took ${System.currentTimeMillis() - start} milliseconds to run")

                    val correctResult = try {
                        resultIsCorrect(result, inputMessage, sender, dispatchTime)
                    } catch (e: InterruptedException) {
                        throw e
                    } catch (e: Exception) {
                        false
                    }

                    if (!correctResult) {
                        executionStatus = ExecutionStatus.FINISHED_WITH_ERRORS
                        _errorLaunches = _errorLaunches.inc()
                    } else {
                        _successLaunches = _successLaunches.inc()
                    }
                } catch (e: InterruptedException) {
                    executionStatus = ExecutionStatus.INTERRUPTED
                    _timeoutLaunches = _timeoutLaunches.inc()
                } catch (e: Exception) {
                    executionStatus = ExecutionStatus.ERROR_ABORTED
                    _abortLaunches = _abortLaunches.inc()
                    logger.error("Error during execution", e)
                    onException(e, inputMessage, sender, dispatchTime)
                } finally {
                    mqManager.sendStatus(name, managerQueue, executionId, executionStatus)
                    onEnd(inputMessage, sender, dispatchTime)
                }
            }

            if (taskParameters.timeout > 0) {
                val start = System.currentTimeMillis()

                while (System.currentTimeMillis() - start < taskParameters.timeout) {
                    delay(100)
                }

                if (job.isActive) {
                    job.cancel()
                    executionStatus = ExecutionStatus.TIMEOUT
                    onTimeout(inputMessage, sender, dispatchTime)
                }

            } else {
                job.join()
            }

            if (result == null) {
                logger.debug("Result of process is null, nothing to send")
            } else {
                logger.debug("Sending result to receivers")
                taskParameters.receivers.forEach {  receiver ->
                    mqManager.send(name, receiver, result!!, executionId)
                }
            }
        } catch (e: Exception) {
            logger.error("Fatal error during task launch", e)
        } finally {
            _status = TaskStatus.IDLE
            logger.debug("Status changed: $_status")
        }
    }

    /**
     * Number of times that this task has been started. It's considered that a task has been started when changes their status from STOPPED to IDLE or RUNNING
     */
    private var _starts: BigInteger = BigInteger.ZERO

    /**
     * Number of times that this task has been stopped. Is that a task has been stopped when changes their status to STOPPED
     */
    private var _stops: BigInteger = BigInteger.ZERO

    /**
     * Number of all times that task was executed successfully
     */
    private var _successLaunches: BigInteger = BigInteger.ZERO

    /**
     * Number of all times that task was executed with a wrong result
     */
    private var _errorLaunches: BigInteger = BigInteger.ZERO

    /**
     * Number of all times that task was executed and aborts their execution due to an exception
     */
    private var _abortLaunches: BigInteger = BigInteger.ZERO

    /**
     * Number of all times that task was executed and aborts their execution due to a timeout
     */
    private var _timeoutLaunches: BigInteger = BigInteger.ZERO

    /**
     * Current status of task, to check if currently is processing anything or not
     */
    private var _status: TaskStatus = TaskStatus.STOPPED

    /**
     * Date and time when task was created
     */
    private val implementationTime: LocalDateTime = LocalDateTime.now()

    /**
     * Last date when task was launched
     */
    private lateinit var _lastLaunchDate: LocalDateTime

    /**
     * Logger object to send messages to console or log file
     */
    protected val logger: Logger = LoggerFactory.getLogger(name)

    /**
     * Coroutine job used to run custom logic
     */
    private lateinit var job: Job

    /**
     * Obtains type of received messages of this task, it's used to deserialize input messages from task's MQ queue
     */
    private val inputType: Class<T> = getGenericType()

    /**
     * Name of manager queue obtained from properties. Manager queue is where task puts their executions results
     */
    private val managerQueue: String = applicationContext.getProperty("orchestrator.mq.manager.queue", String::class.java)
        .orElseThrow { throw InitializationException("Property 'orchestrator.mq.manager.queue' doesn't exists") }

    /**
     * Object used to manage MQ operations (create queue, create a consumer, send message to queue, ...)
     */
    private val mqManager: MQManager<*, *, *> = applicationContext.createBean(MQManager::class.java)

    /**
     * Instance of jackson's ObjectMapper used for JSON serialization and deserialization
     */
    protected val jsonMapper: ObjectMapper = applicationContext.createBean(ObjectMapper::class.java)

    /**
     * Object related to task parameters. Contains all values used by task to be parametrized
     */
    protected val taskParameters: P = getTaskParameters(parametersClass)

    /**
     * Overridable function to make some operations when task is started
     */
    protected open fun onConstruct() {}

    /**
     * Abstract function to make custom logic
     */
    protected abstract fun onLaunch(inputMessage: T?, sender: String, dispatchTime: LocalDateTime): DataFrame<*>?

    /**
     * Abstract function to make custom operations when task execution produces an exception
     */
    protected abstract fun onException(e: Exception, inputMessage: T?, sender: String, dispatchTime: LocalDateTime)

    /**
     * Abstract function to make custom operations when task execution finishes
     */
    protected abstract fun onEnd(inputMessage: T?, sender: String, dispatchTime: LocalDateTime)

    /**
     * Abstract function to make custom operations when task execution aborts due to a timeout
     */
    protected abstract fun onTimeout(inputMessage: T?, sender: String, dispatchTime: LocalDateTime)

    /**
     * Overridable function to define a function to check if result of [onLaunch] is correct or not
     */
    protected open fun resultIsCorrect(result: DataFrame<*>?, inputMessage: T?, sender: String, dispatchTime: LocalDateTime): Boolean = true

    /**
     * Overridable function to make some operations when task is stopped
     */
    protected open fun onDestroy() {}

    /**
     * Function that defines logic for MQ consumers related to this task. This function doesn't apply if [disableInputQueue] is true.
     */
    protected fun processReceivedMessage(msg: Any?) {
        logger.debug("New message to process has been received")

        if (msg == null)
            throw java.lang.IllegalArgumentException("Invalid message received: null")

        logger.debug("Trying to deserialize received message")
        val message = when (msg) {
            is TextMessage -> jsonMapper.readValue(msg.text, jsonMapper.typeFactory.constructParametricType(MQMessage::class.java, inputType)) as MQMessage<T>
            is BytesMessage -> jsonMapper.readValue(msg.readUTF(), jsonMapper.typeFactory.constructParametricType(
                MQMessage::class.java, inputType)) as MQMessage<T>
            is RabbitMQMessage -> jsonMapper.readValue(msg.body, jsonMapper.typeFactory.constructParametricType(
                MQMessage::class.java, inputType)) as MQMessage<T>
            else -> throw java.lang.IllegalArgumentException("Invalid message type received: ${msg::class.java.name}")
        }

        logger.debug("Processing received message")
        runBlocking {
            launch(message.executionId, message.message, message.sender, message.dispatchTime)
        }
    }

    /**
     * Converts task parameters passed as a Map object to needed [TaskParameters] child class
     */
    @Suppress("UNCHECKED_CAST")
    private fun getTaskParameters(paramsClass: Class<*>): P =
        jsonMapper.readValue(jsonMapper.writeValueAsString(parameters), paramsClass) as P

    /**
     * Obtains generic type of this task
     */
    @Suppress("UNCHECKED_CAST")
    private fun getGenericType(): Class<T> {
        val generics = GenericTypeUtils.resolveTypeArguments(this::class.java.genericSuperclass)
        return generics[generics.size - 1] as Class<T>
    }

    /**
     * Micronaut event executed when a new instance of this class is created
     */
    @PostConstruct
    protected fun postConstruct() {
        logger.debug("Starting...")
        _status = TaskStatus.IDLE
        logger.debug("Status changed: $_status")

        try {
            if (!disableInputQueue) {
                logger.debug("Input queue is enabled, creating queue and consumer...")
                mqManager.createQueue(name)
                mqManager.createConsumers(name, taskParameters.concurrency) {
                    processReceivedMessage(it)
                }
            }
            onConstruct()

            _starts = _starts.inc()
            logger.debug("Starts incremented, now number of starts is $_starts")
        } catch (e: Exception) {
            logger.error("Fatal exception during task starting", e)
        }
    }

    /**
     * Micronaut event executed when a new instance of this class is destroyed
     */
    @PreDestroy
    protected fun preDestroy() {
        logger.debug("Stopping...")
        _status = TaskStatus.STOPPED
        logger.debug("Status changed: $_status")

        try {
            if (!disableInputQueue) {
                logger.debug("Input queue is enabled, cancelling consumers...")
                mqManager.cancelConsumers()
            }
            onDestroy()
        } catch (e: Exception) {
            logger.error("Fatal exception during task stopping", e)
        }

        _stops = _stops.inc()
        logger.debug("Stops incremented, now number of stops is $_stops")
    }

    init {
        taskParameters.checkParams()
    }
}
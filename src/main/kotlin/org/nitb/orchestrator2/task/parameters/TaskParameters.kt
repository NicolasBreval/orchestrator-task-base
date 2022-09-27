package org.nitb.orchestrator2.task.parameters

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.nitb.orchestrator2.task.annotation.NoArgs
import org.nitb.orchestrator2.task.annotation.TaskParameterCheck
import org.nitb.orchestrator2.task.exception.IllegalTaskParameterValueException
import org.nitb.orchestrator2.task.exception.TaskParametersCheckingException
import org.slf4j.LoggerFactory
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible

/**
 * Base parameters that all tasks must be contained in their custom parameters
 */
@NoArgs
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Base parameters that all tasks must be contained in their custom parameters")
open class TaskParameters(
    /**
     * Timeout used to broke execution if task exceeds it. If is not set, there are no timeout for task
     */
    @param:JsonProperty(TASK_TIMEOUT, required = false, defaultValue = "-1")
    @get:JsonProperty(TASK_TIMEOUT, required = false, defaultValue = "-1")
    @Schema(
        name = TASK_TIMEOUT,
        description = "Timeout used to broke execution if task exceeds it. If is not set, there are no timeout for task",
        required = false,
        example = "5000",
        defaultValue = "-1"
    )
    val timeout: Long = -1,

    /**
     * Set of tasks connected to current task. All receivers will receive output message created by current task
     */
    @param:JsonProperty(TASK_RECEIVERS, required = false, defaultValue = "[]")
    @get:JsonProperty(TASK_RECEIVERS, required = false, defaultValue = "[]")
    @Schema(
        name = TASK_RECEIVERS,
        description = "Set of tasks connected to current task. All receivers will receive output message created by current task",
        required = false,
        example = """["receiver1", "receiver2"]""",
        defaultValue = "[]"
    )
    val receivers: Array<String> = arrayOf(),

    /**
     * Number of instances of a same task that they can be running at same time
     */
    @param:JsonProperty(TASK_CONCURRENCY, required = false, defaultValue = "1")
    @get:JsonProperty(TASK_CONCURRENCY, required = false, defaultValue = "1")
    @Schema(
        name = TASK_CONCURRENCY,
        description = "Number of instances of a same task that they can be running at same time",
        required = false,
        example = "5",
        defaultValue = "1"
    )
    val concurrency: Int = 1
) {

    companion object {
        private const val TASK_TIMEOUT = "task.timeout"
        private const val TASK_RECEIVERS = "task.receivers"
        private const val TASK_CONCURRENCY = "task.concurrency"
        private val lock = ReentrantLock()
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    @TaskParameterCheck
    protected fun checkConcurrency() {
        if (concurrency < 1)
            throw IllegalTaskParameterValueException("$TASK_CONCURRENCY property must be greater than zero.")
    }

    fun checkParams() {
        val checkExceptions =
            this::class.functions.filter { func -> func.hasAnnotation<TaskParameterCheck>() }.mapNotNull { func ->
                var exception: Exception? = null

                try {
                    lock.withLock {
                        val wasAccessible = func.isAccessible

                        if (!wasAccessible)
                            func.isAccessible = true

                        func.call(this)

                        if (!wasAccessible)
                            func.isAccessible = false
                    }
                } catch (e: Exception) {
                    exception = e
                }

                exception
            }

        if (checkExceptions.isNotEmpty()) {
            throw TaskParametersCheckingException(checkExceptions)
        }
    }
}
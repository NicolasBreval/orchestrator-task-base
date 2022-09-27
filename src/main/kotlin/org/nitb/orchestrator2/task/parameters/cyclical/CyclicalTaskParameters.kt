package org.nitb.orchestrator2.task.parameters.cyclical

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.scheduling.cron.CronExpression
import io.swagger.v3.oas.annotations.media.Schema
import org.nitb.orchestrator2.task.annotation.TaskParameterCheck
import org.nitb.orchestrator2.task.exception.IllegalTaskParameterValueException
import org.nitb.orchestrator2.task.parameters.TaskParameters

open class CyclicalTaskParameters(
    timeout: Long = -1,
    receivers: Array<String> = arrayOf(),
    concurrency: Int = 1,
    /**
     * Cron expression used to schedule a cyclical task
     */
    @param:JsonProperty(CRON, required = false, defaultValue = "")
    @get:JsonProperty(CRON, required = false, defaultValue = "")
    @Schema(name = CRON, description = "Cron expression used to schedule a cyclical task", required = false, example = "0 15 10 ? * MON", defaultValue = "")
    var cron: String = "",
    /**
     * Time, in milliseconds, to wait between two executions of a cyclical task. If [CyclicalTaskParameters.FIXED_DELAY] parameter is set, this parameter doesn't applies
     */
    @param:JsonProperty(FIXED_DELAY, required = false, defaultValue = "-1")
    @get:JsonProperty(FIXED_DELAY, required = false, defaultValue = "-1")
    @Schema(name = FIXED_DELAY, description = "Time, in milliseconds, to wait between two executions of a cyclical task. If $CRON parameter is set, this parameters doesn't applies", required = false, example = "5000", defaultValue = "-1")
    var fixedDelay: Long = -1
): TaskParameters(timeout, receivers, concurrency) {

    companion object {
        private const val CRON = "cyclical.cron"
        private const val FIXED_DELAY = "cyclical.fixed-delay"
    }

    @Suppress("UNUSED")
    @TaskParameterCheck
    protected fun checkCron() {
        if (cron.isNotEmpty()) {
            try {
                CronExpression.create(cron)
            } catch (e: Exception) {
                throw IllegalTaskParameterValueException("Cron expression '$cron' is not a valid quartz cron expression")
            }
        }
    }

    @Suppress("UNUSED")
    @TaskParameterCheck
    protected fun checkFixedDelay() {
        if (cron.isEmpty() && fixedDelay <= 0) {
            throw IllegalTaskParameterValueException("If cron expression is not set, fixed delay must be a long value greater than zero")
        }
    }
}
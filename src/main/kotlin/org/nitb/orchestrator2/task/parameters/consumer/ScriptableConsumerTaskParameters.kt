package org.nitb.orchestrator2.task.parameters.consumer

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.nitb.orchestrator2.task.annotation.NoArgs
import org.nitb.orchestrator2.task.annotation.TaskParameterCheck
import org.nitb.orchestrator2.task.exception.IllegalTaskParameterValueException

/**
 * List of parameters that a MQ consumer task needs
 */
@NoArgs
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "List of parameters that a MQ consumer task needs")
open class ScriptableConsumerTaskParameters(
    timeout: Long = -1,
    receivers: Array<String> = arrayOf(),
    concurrency: Int = 1,
    internal: Boolean = true,
    type: String? = null,
    uri: String? = null,
    username: String? = null,
    password: String? = null,
    /**
     * JavaScript code used to transform original message received from queue into a valid DataFrame object
     */
    @param:JsonProperty(SCRIPT, required = true)
    @get:JsonProperty(SCRIPT, required = true)
    @Schema(name = SCRIPT, description = "JavaScript code used to transform original message received from queue into a valid DataFrame object", required = true, example = "")
    val script: String,
    /**
     * JavaScript code used to check if processed value returned by script is correct or not
     */
    @param:JsonProperty(CHECK_RESULT_SCRIPT, required = false)
    @get:JsonProperty(CHECK_RESULT_SCRIPT, required = false)
    @Schema(name = CHECK_RESULT_SCRIPT, description = "JavaScript code used to check if processed value returned by script is correct or not", required = false, example = "")
    val checkResultScript: String? = null
): ConsumerTaskParameters(timeout, receivers, concurrency, internal, type, uri, username, password) {

    companion object {
        private const val SCRIPT = "mq.script"
        private const val CHECK_RESULT_SCRIPT = "mq.check-result-script"
    }

    @Suppress("UNUSED")
    @TaskParameterCheck
    protected fun checkScript() {
        commonCheckScript(script, SCRIPT)
    }

    @Suppress("UNUSED")
    @TaskParameterCheck
    protected fun checkCheckScript() {
        if (checkResultScript != null)
            commonCheckScript(checkResultScript, CHECK_RESULT_SCRIPT)
    }

    private fun commonCheckScript(script: String, paramName: String) {
        if (!"var result *= *.*".toRegex().containsMatchIn(script))
            throw IllegalTaskParameterValueException("Invalid value for $paramName, it must contains a variable called result with result of your script")
    }
}
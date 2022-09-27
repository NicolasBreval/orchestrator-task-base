package org.nitb.orchestrator2.task.parameters.cyclical

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.nitb.orchestrator2.task.annotation.TaskParameterCheck
import org.nitb.orchestrator2.task.exception.IllegalTaskParameterValueException

open class ScriptableCyclicalTaskParameters(
    timeout: Long = -1,
    receivers: Array<String> = arrayOf(),
    concurrency: Int = 1,
    cron: String = "",
    fixedDelay: Long = -1,
    /**
     * JavaScript code to execute on each cycle
     */
    @param:JsonProperty(CYCLICAL_SCRIPT, required = true)
    @get:JsonProperty(CYCLICAL_SCRIPT, required = true)
    @Schema(name = CYCLICAL_SCRIPT, description = "JavaScript code to execute on each cycle", required = true, example = "")
    val script: String,
    /**
     * JavaScript code used to check if processed value returned by script is correct or not
     */
    @param:JsonProperty(CYCLICAL_CHECK_RESULT_SCRIPT, required = false)
    @get:JsonProperty(CYCLICAL_CHECK_RESULT_SCRIPT, required = false)
    @Schema(name = CYCLICAL_CHECK_RESULT_SCRIPT, description = "JavaScript code used to check if processed value returned by script is correct or not", required = false, example = "")
    val checkResultScript: String? = null
): CyclicalTaskParameters(timeout, receivers, concurrency, cron, fixedDelay) {

    companion object {
        private const val CYCLICAL_SCRIPT = "cyclical.script"
        private const val CYCLICAL_CHECK_RESULT_SCRIPT = "cyclical.check-result-script"
    }

    @Suppress("UNUSED")
    @TaskParameterCheck
    protected fun checkScript() {
        commonCheckScript(script, CYCLICAL_SCRIPT)
    }

    @Suppress("UNUSED")
    @TaskParameterCheck
    protected fun checkCheckScript() {
        if (checkResultScript != null)
            commonCheckScript(checkResultScript, CYCLICAL_CHECK_RESULT_SCRIPT)
    }

    private fun commonCheckScript(script: String, paramName: String) {
        if (!"var result *= *.*".toRegex().containsMatchIn(script))
            throw IllegalTaskParameterValueException("Invalid value for $paramName, it must contains a variable called result with result of your script")
    }

}
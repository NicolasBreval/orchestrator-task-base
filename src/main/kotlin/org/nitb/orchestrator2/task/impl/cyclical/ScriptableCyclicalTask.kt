package org.nitb.orchestrator2.task.impl.cyclical

import com.fasterxml.jackson.core.JacksonException
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Prototype
import jakarta.inject.Named
import org.graalvm.polyglot.PolyglotException
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.nitb.orchestrator2.task.exception.ScriptResultException
import org.nitb.orchestrator2.task.js.JavaScriptInterpreter
import org.nitb.orchestrator2.task.parameters.cyclical.ScriptableCyclicalTaskParameters
import java.time.LocalDateTime

@Suppress("UNUSED")
@Prototype
@Named("SCRIPTABLE-CYCLICAL")
class ScriptableCyclicalTask(
    @Parameter
    name: String,
    @Parameter
    parameters: Map<String, Any>,
    applicationContext: ApplicationContext
): CyclicalTask<ScriptableCyclicalTaskParameters>(name, parameters, ScriptableCyclicalTaskParameters::class.java, applicationContext) {

    override fun onLaunch(inputMessage: Nothing?, sender: String, dispatchTime: LocalDateTime): DataFrame<*>? {
        return try {
            JavaScriptInterpreter.process<DataFrame<*>>(taskParameters.script, Pair("inputMessage", inputMessage),
                Pair("sender", sender), Pair("dispatchTime", dispatchTime), Pair("logger", logger))
        } catch (e: Exception) {
            if (e is java.lang.ClassCastException || e is java.lang.IllegalStateException || e is PolyglotException || e is JacksonException) {
                throw ScriptResultException("The result of script must be a variable called `result`, with org.jetbrains.kotlinx.dataframe.DataFrame type", e)
            } else throw e
        }
    }

    override fun onException(e: Exception, inputMessage: Nothing?, sender: String, dispatchTime: LocalDateTime) {

    }

    override fun onEnd(inputMessage: Nothing?, sender: String, dispatchTime: LocalDateTime) {

    }

    override fun onTimeout(inputMessage: Nothing?, sender: String, dispatchTime: LocalDateTime) {

    }

    override fun resultIsCorrect(
        result: DataFrame<*>?,
        inputMessage: Nothing?,
        sender: String,
        dispatchTime: LocalDateTime
    ): Boolean {
        return if (taskParameters.checkResultScript.isNullOrEmpty()) true else try {
            JavaScriptInterpreter.process<Boolean>(taskParameters.checkResultScript, Pair("executionResult", result),
                Pair("inputMessage", inputMessage), Pair("sender", sender), Pair("dispatchTime", dispatchTime), Pair("logger", logger)) ?: false
        } catch (e: InterruptedException) {
            throw e
        } catch (e: Exception) {
            if (e is java.lang.ClassCastException || e is java.lang.IllegalStateException || e is PolyglotException || e is JacksonException) {
                logger.error("The result of script must be a variable called `result`, with boolean type")
                false
            } else false
        }
    }
}
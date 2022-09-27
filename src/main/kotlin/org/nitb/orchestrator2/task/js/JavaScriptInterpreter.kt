package org.nitb.orchestrator2.task.js

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess

import org.nitb.orchestrator2.task.exception.ScriptResultException

object JavaScriptInterpreter {

    inline fun <reified T> process(script: String, vararg params: Pair<String, Any?>): T? {
        Context.newBuilder("js")
            .allowHostAccess(HostAccess.ALL)
            .allowHostClassLookup { true }.build().use { scriptContext ->
                params.forEach { (paramName, paramValue) ->
                    scriptContext.getBindings("js").putMember(paramName, paramValue)
                }

                val scriptWithDependencies = """
                    var InteroperabilityUtils = Java.type('org.nitb.orchestrator2.task.js.JavaScriptInteroperabilityUtils');
                    $script
                """.trimIndent()

                scriptContext.eval("js", scriptWithDependencies)

                val result = scriptContext.getBindings("js").getMember("result")
                    ?: throw ScriptResultException("The result of script must be a variable called `result`, with org.jetbrains.kotlinx.dataframe.DataFrame type")

                return result.`as`(T::class.java)
            }
    }



}
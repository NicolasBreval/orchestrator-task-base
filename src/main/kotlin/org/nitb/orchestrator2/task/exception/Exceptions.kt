package org.nitb.orchestrator2.task.exception

import kotlin.Exception

/**
 * Thrown when a JavaScript execution produces an unexpected result or an exception
 */
class ScriptResultException(msg: String? = null, throwable: Throwable? = null): Exception(msg, throwable)

/**
 * Special type of exception that causes a message on queue won't acknowledge and will be processed again
 */
class MQBlockingException(msg: String? = null, throwable: Throwable? = null): Exception(msg, throwable)

/**
 * Thrown when a message received from a queue doesn't contain a valid format
 */
class MQMessageParseException(msg: String? = null, throwable: Throwable? = null): Exception(msg, throwable)

/**
 * Thrown when a task parameter doesn't pass checks
 */
class IllegalTaskParameterValueException(msg: String? = null, throwable: Throwable? = null): Exception(msg, throwable)

/**
 * Exception with a list of exceptions thrown by task parameters checks
 */
class TaskParametersCheckingException(msg: String? = null, throwable: Throwable? = null): Exception(msg, throwable) {

    constructor(exceptionsList: List<Exception>): this("Some parameters cannot be accessible: \n${exceptionsList.joinToString("\n") { "\t${it.stackTraceToString()}" }}")

}

/**
 * Thrown when a component cannot be initialized correctly
 */
class InitializationException(msg: String? = null, throwable: Throwable? = null): Exception(msg, throwable)


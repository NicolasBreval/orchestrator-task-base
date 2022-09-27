package org.nitb.orchestrator2.task.annotation

/**
 * Annotation used to mark all functions inside a [org.nitb.orchestrator2.task.parameters.TaskParameters] that must be executed at object creation to check their values
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TaskParameterCheck

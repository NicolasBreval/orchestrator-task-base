package org.nitb.orchestrator2.task.annotation

/**
 * Custom annotation with unique purpose to be used by Kotlin's no-arg plugin
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class NoArgs

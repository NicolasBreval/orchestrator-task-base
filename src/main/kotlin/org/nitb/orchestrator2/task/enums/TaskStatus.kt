package org.nitb.orchestrator2.task.enums

/**
 * Status of a task
 */
enum class TaskStatus {
    /**
     * Task is currently doing nothing
     */
    IDLE,

    /**
     * Task is processing anything
     */
    RUNNING,

    /**
     * Task has been stopped by user
     */
    STOPPED
}
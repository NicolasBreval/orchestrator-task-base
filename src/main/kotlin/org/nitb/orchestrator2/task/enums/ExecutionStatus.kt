package org.nitb.orchestrator2.task.enums

/**
 * Defines all possible statuses that a task invocation can have
 */
enum class ExecutionStatus {
    /**
     * Task was executed successfully and their function to check result returns true, or is not implemented
     */
    FINISHED,

    /**
     * Task was executed successfully, but their function to check result returns false
     */
    FINISHED_WITH_ERRORS,

    /**
     * During task execution some exception was thrown
     */
    ERROR_ABORTED,

    /**
     * Task execution was interrupted by user or by system
     */
    INTERRUPTED,

    /**
     * Task execution exceeded their timeout
     */
    TIMEOUT
}
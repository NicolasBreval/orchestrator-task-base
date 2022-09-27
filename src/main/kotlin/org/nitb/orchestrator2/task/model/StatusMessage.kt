package org.nitb.orchestrator2.task.model

import io.micronaut.core.annotation.Introspected
import org.nitb.orchestrator2.task.enums.ExecutionStatus

@Introspected
data class StatusMessage(
    val sender: String,
    val executionId: String,
    val executionStatus: ExecutionStatus
)
package org.nitb.orchestrator2.task.mq.model

import io.micronaut.core.annotation.Introspected
import java.time.LocalDateTime
import java.util.UUID

@Introspected
data class MQMessage<T>(
    val message: T?,
    val sender: String,
    val dispatchTime: LocalDateTime,
    val executionId: String = UUID.randomUUID().toString()
)
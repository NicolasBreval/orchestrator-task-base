package org.nitb.orchestrator2.task.mq.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.micronaut.core.annotation.Introspected
import java.time.LocalDateTime
import java.util.UUID

@Introspected
data class MQMessage<T>(
    val message: T?,
    val sender: String,
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-ddTHH:mm:ss.SSSZ")
    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-ddTHH:mm:ss.SSSZ")
    val dispatchTime: LocalDateTime,
    val executionId: String = UUID.randomUUID().toString()
)
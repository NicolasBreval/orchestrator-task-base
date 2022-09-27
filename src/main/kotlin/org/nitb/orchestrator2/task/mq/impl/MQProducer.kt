package org.nitb.orchestrator2.task.mq.impl

import jakarta.inject.Singleton
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.nitb.orchestrator2.task.mq.model.MQMessage

@Singleton
abstract class MQProducer {
    abstract fun send(queue: String, message: MQMessage<DataFrame<*>>)
}
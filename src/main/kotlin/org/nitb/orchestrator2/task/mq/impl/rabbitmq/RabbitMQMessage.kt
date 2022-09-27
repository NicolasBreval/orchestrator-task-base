package org.nitb.orchestrator2.task.mq.impl.rabbitmq

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Envelope

data class RabbitMQMessage(
    val consumerTag: String?,
    val envelope: Envelope?,
    val properties: AMQP.BasicProperties?,
    val body: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RabbitMQMessage) return false

        if (consumerTag != other.consumerTag) return false
        if (envelope != other.envelope) return false
        if (properties != other.properties) return false
        if (body != null) {
            if (other.body == null) return false
            if (!body.contentEquals(other.body)) return false
        } else if (other.body != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = consumerTag?.hashCode() ?: 0
        result = 31 * result + (envelope?.hashCode() ?: 0)
        result = 31 * result + (properties?.hashCode() ?: 0)
        result = 31 * result + (body?.contentHashCode() ?: 0)
        return result
    }
}
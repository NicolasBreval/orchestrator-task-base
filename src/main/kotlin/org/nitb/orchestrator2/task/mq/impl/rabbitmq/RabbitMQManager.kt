package org.nitb.orchestrator2.task.mq.impl.rabbitmq

import com.fasterxml.jackson.databind.ObjectMapper
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.AMQP.Queue.DeclareOk
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import io.micronaut.context.annotation.Prototype
import io.micronaut.context.annotation.Requires
import jakarta.inject.Named
import org.nitb.orchestrator2.task.mq.impl.MQManager

@Prototype
@Named("rabbitmq-manager")
@Requires(property = "rabbitmq.enabled", value = "true")
class RabbitMQManager(
    private val channel: Channel,
    private val mapper: ObjectMapper
): MQManager<DeclareOk, String, RabbitMQMessage>() {

    override fun close() {
        channel.connection.close()
    }

    override fun waitForConnection() {
        while (!channel.connection.isOpen) Thread.sleep(100)
    }

    override fun newQueue(queue: String): DeclareOk {
        return channel.queueDeclare(queue, true, false, false, null)
    }

    override fun sendNewMessage(queue: String, message: Any, expiryTime: Long?) {
        val properties = expiryTime?.let { AMQP.BasicProperties.Builder().expiration(it.toString()).build() }
        channel.basicPublish("", queue, properties, mapper.writeValueAsBytes(message))
    }

    override fun purgeQueue(queue: String) {
        channel.queuePurge(queue)
    }

    override fun acknowledgement(message: RabbitMQMessage) {
        channel.basicAck(message.envelope!!.deliveryTag, false)
    }

    override fun cancelExistingConsumer(consumer: String) {
        channel.basicCancel(consumer)
    }

    override fun newConsumer(queue: String, onConsume: (RabbitMQMessage) -> Unit): String {
        return channel.basicConsume(queue, object : DefaultConsumer(channel) {
            override fun handleDelivery(
                consumerTag: String?,
                envelope: Envelope?,
                properties: AMQP.BasicProperties?,
                body: ByteArray?
            ) {
                onConsume(RabbitMQMessage(consumerTag, envelope, properties, body))
            }
        })
    }
}
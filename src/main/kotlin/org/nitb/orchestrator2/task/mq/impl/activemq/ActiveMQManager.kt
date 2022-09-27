package org.nitb.orchestrator2.task.mq.impl.activemq

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Prototype
import io.micronaut.context.annotation.Requires
import jakarta.inject.Named
import org.apache.activemq.ActiveMQSession
import org.nitb.orchestrator2.task.mq.impl.MQManager
import org.nitb.orchestrator2.task.util.SystemProperties.Companion.ACTIVEMQ_QUEUE_SYSTEM_OPTION
import org.nitb.orchestrator2.task.util.SystemProperties.Companion.APPLICATION_QUEUE_SYSTEM
import javax.jms.*

@Prototype
@Named("$ACTIVEMQ_QUEUE_SYSTEM_OPTION-manager")
@Requires(property = APPLICATION_QUEUE_SYSTEM, value = ACTIVEMQ_QUEUE_SYSTEM_OPTION)
class ActiveMQManager(
    private val session: Session,
    private val mapper: ObjectMapper
): MQManager<Destination, MessageConsumer, Message>() {

    override fun close() {
        (session as ActiveMQSession).close()
    }

    override fun waitForConnection() {
        while ((session as ActiveMQSession).connection.isClosed) Thread.sleep(100)
    }

    override fun newQueue(queue: String): Destination {
        return (session as ActiveMQSession).createQueue("$queue?consumer.exclusive=true")
    }

    override fun sendNewMessage(queue: String, message: Any) {
        val producer = (session as ActiveMQSession).createProducer(newQueue(queue))
        producer.send(session.createTextMessage(mapper.writeValueAsString(message)))
    }

    override fun purgeQueue(queue: String) {
        val simpleConsumer = (session as ActiveMQSession).createConsumer(newQueue(queue))
        var cleared = false

        while (!cleared) {
            val lastMessage = simpleConsumer.receiveNoWait()?.acknowledge()

            if (lastMessage == null)
                cleared = true
        }

        simpleConsumer.close()
    }

    override fun acknowledgement(message: Message) {
        message.acknowledge()
    }

    override fun cancelExistingConsumer(consumer: MessageConsumer) {
        consumer.close()
    }

    override fun newConsumer(queue: String, onConsume: (Message) -> Unit): MessageConsumer {
        return (session as ActiveMQSession).createConsumer(newQueue(queue), onConsume)
    }
}
package org.nitb.orchestrator2.task.mq.impl

import io.micronaut.context.annotation.Prototype
import org.nitb.orchestrator2.task.enums.ExecutionStatus
import org.nitb.orchestrator2.task.exception.MQBlockingException
import org.nitb.orchestrator2.task.exception.MQMessageParseException
import org.nitb.orchestrator2.task.model.StatusMessage
import org.nitb.orchestrator2.task.mq.model.MQMessage
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.UUID
import javax.annotation.PreDestroy
import javax.jms.BytesMessage
import javax.jms.TextMessage

@Prototype
abstract class MQManager<Q, C, M> {

    fun createQueue(queue: String) {
        waitForConnection()
        newQueue(queue)
    }

    fun createConsumers(queue: String, replicas: Int, onConsume: (M) -> Unit) {
        val logger = LoggerFactory.getLogger(queue)

        (0 until replicas).forEach {  num ->
            logger.debug("Creating consumer number ${num + 1}")
            consumers.add(createConsumer(queue, onConsume))
        }
    }

    fun cancelConsumers() {
        consumers.forEach {  consumer ->
            cancelConsumer(consumer)
        }

        consumers.clear()
    }

    fun send(sender: String, queue: String, message: Any, executionId: String = UUID.randomUUID().toString()) {
        waitForConnection()
        sendNewMessage(queue, MQMessage(message, sender, OffsetDateTime.now(), executionId))
    }

    fun sendStatus(sender: String, queue: String, executionId: String, executionStatus: ExecutionStatus) {
        waitForConnection()
        sendNewMessage(queue, StatusMessage(sender, executionId, executionStatus))
    }

    fun purge(queue: String) {
        waitForConnection()
        purgeQueue(queue)
    }

    abstract fun close()

    private val consumers: MutableList<C> = mutableListOf()

    protected abstract fun waitForConnection()

    protected abstract fun newQueue(queue: String): Q

    protected abstract fun newConsumer(queue: String, onConsume: (M) -> Unit): C

    protected abstract fun cancelExistingConsumer(consumer: C)

    protected abstract fun acknowledgement(message: M)

    protected abstract fun sendNewMessage(queue: String, message: Any)

    protected abstract fun purgeQueue(queue: String)

    @PreDestroy
    protected open fun preDestroy() {
        cancelConsumers()
    }

    private fun createConsumer(queue: String, onConsume: (M) -> Unit): C {
        waitForConnection()
        return newConsumer(queue) { msg ->
            val logger = LoggerFactory.getLogger(queue)
            var sendAck = true

            try {
                onConsume.invoke(msg)
            } catch (e: Exception) {
                when (e) {
                    is MQBlockingException -> {
                        logger.warn("New error produced that blocks message in queue", e)
                        sendAck = false
                    }
                    is MQMessageParseException -> {
                        logger.error("Error processing input message ${if (msg is BytesMessage) msg.readUTF() else if (msg is TextMessage) msg.text else "Unrecognized"}", e)
                    }
                    else -> logger.error("New error produced, message lost", e)
                }
            } finally {
                if (sendAck)
                    acknowledgement(msg)
            }
        }
    }

    private fun cancelConsumer(consumer: C) {
        waitForConnection()
        cancelExistingConsumer(consumer)
    }

}
package org.nitb.orchestrator2.task.impl.consumer

import com.rabbitmq.client.ConnectionFactory
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Prototype
import io.micronaut.context.annotation.Requires
import org.apache.activemq.ActiveMQConnectionFactory
import org.nitb.orchestrator2.task.impl.BaseTask
import org.nitb.orchestrator2.task.mq.impl.MQManager
import org.nitb.orchestrator2.task.mq.impl.activemq.ActiveMQManager
import org.nitb.orchestrator2.task.mq.impl.rabbitmq.RabbitMQManager
import org.nitb.orchestrator2.task.parameters.consumer.ConsumerTaskParameters
import org.nitb.orchestrator2.task.util.SystemProperties
import javax.jms.Session

@Prototype
@Requires(bean = MQManager::class)
abstract class ConsumerTask<P: ConsumerTaskParameters, T>(
    @Parameter
    name: String,
    @Parameter
    parameters: Map<String, Any>,
    parametersClass: Class<P>,
    applicationContext: ApplicationContext
): BaseTask<P, T>(name, parameters, parametersClass, applicationContext) {

    private val customMqManager = if (!taskParameters.internal) {
        when (taskParameters.type) {
            SystemProperties.ACTIVEMQ_QUEUE_SYSTEM_OPTION -> {
                val connectionFactory = ActiveMQConnectionFactory()
                connectionFactory.brokerURL = taskParameters.uri
                taskParameters.username?.let { connectionFactory.userName = it }
                taskParameters.password?.let { connectionFactory.password = it }
                ActiveMQManager(connectionFactory.createConnection().createSession(false, Session.CLIENT_ACKNOWLEDGE), jsonMapper)
            }
            SystemProperties.RABBITMQ_QUEUE_SYSTEM_OPTION -> {
                val connectionFactory = ConnectionFactory()
                connectionFactory.setUri(taskParameters.uri)
                taskParameters.username?.let { connectionFactory.username = it }
                taskParameters.password?.let { connectionFactory.password = it }
                RabbitMQManager(connectionFactory.newConnection().createChannel(), jsonMapper)
            }
            else -> throw java.lang.IllegalArgumentException("Unrecognized MQ type.")
        }
    } else null

    override fun onConstruct() {
        super.onConstruct()

        if (!taskParameters.internal)
            customMqManager?.createConsumers(name, taskParameters.concurrency) { processReceivedMessage(it) }
    }

    override fun onDestroy() {
        if (!taskParameters.internal) {
            customMqManager?.cancelConsumers()

            if (!taskParameters.internal)
                customMqManager?.close()
        }

        super.onDestroy()
    }

}
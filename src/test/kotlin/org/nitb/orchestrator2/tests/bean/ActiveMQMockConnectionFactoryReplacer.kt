package org.nitb.orchestrator2.tests.bean

import io.micronaut.context.annotation.Requires
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import jakarta.inject.Singleton
import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.broker.BrokerService
import java.net.ServerSocket
import javax.jms.ConnectionFactory

@Suppress("UNUSED")
@Singleton
@Requires(property = "micronaut.jms.activemq.classic.enabled", value = "true")
class ActiveMQMockConnectionFactoryReplacer: BeanCreatedEventListener<ConnectionFactory> {

    override fun onCreated(event: BeanCreatedEvent<ConnectionFactory>): ConnectionFactory {
        val randomPort = ServerSocket(0).let { val localPort = it.localPort; it.close(); localPort }
        val brokerUrl = "tcp://localhost:$randomPort"

        val broker = BrokerService()
        broker.addConnector(brokerUrl)
        broker.start()
        broker.deleteAllMessages()

        val connectionFactory = ActiveMQConnectionFactory()
        connectionFactory.brokerURL = brokerUrl
        return connectionFactory
    }
}
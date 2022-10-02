package org.nitb.orchestrator2.tests.bean

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.broker.BrokerService
import java.net.ServerSocket
import javax.jms.ConnectionFactory

@Suppress("UNUSED")
@Factory
class ActiveMQMockConnectionFactory {

    @Bean
    @Requires(property = "jms.activemq.classic.enabled", value = "true")
    fun createConnectionFactory(): ConnectionFactory {
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
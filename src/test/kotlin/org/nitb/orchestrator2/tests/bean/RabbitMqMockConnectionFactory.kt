package org.nitb.orchestrator2.tests.bean

import com.github.fridujo.rabbitmq.mock.MockConnectionFactory
import com.rabbitmq.client.ConnectionFactory
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory

@Suppress("UNUSED")
@Factory
class RabbitMqMockConnectionFactory {

    @Bean
    fun createRabbitMQConnectionFactory(): ConnectionFactory {
        return MockConnectionFactory()
    }

}
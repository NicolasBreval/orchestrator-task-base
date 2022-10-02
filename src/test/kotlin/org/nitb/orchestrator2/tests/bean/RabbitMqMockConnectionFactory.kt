package org.nitb.orchestrator2.tests.bean

import com.github.fridujo.rabbitmq.mock.MockConnectionFactory
import com.rabbitmq.client.ConnectionFactory
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires

@Suppress("UNUSED")
@Factory
class RabbitMqMockConnectionFactory {

    @Bean
    @Requires(property = "rabbitmq.enabled", value = "true")
    fun createRabbitMQConnectionFactory(): ConnectionFactory {
        return MockConnectionFactory()
    }

}
package org.nitb.orchestrator2.task.mq.impl.rabbitmq.bean

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import jakarta.inject.Inject

@Suppress("UNUSED")
@Factory
@Requires(property = "rabbitmq.enabled", value = "true")
class ChannelFactory {

    @Bean
    fun createChannel(): Channel {
        return connectionFactory.newConnection().createChannel()
    }

    @Inject
    private lateinit var connectionFactory: ConnectionFactory
}
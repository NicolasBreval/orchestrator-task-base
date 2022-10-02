package org.nitb.orchestrator2.task.mq.impl.activemq.bean

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import jakarta.inject.Inject
import javax.jms.ConnectionFactory
import javax.jms.Session

@Suppress("UNUSED")
@Factory
@Requires(property = "jms.activemq.classic.enabled", value = "true")
class SessionFactory {

    @Bean
    fun createSession(): Session {
        val connection = connectionFactory.createConnection()
        connection.start()
        return connection.createSession(false, Session.CLIENT_ACKNOWLEDGE)
    }

    @Inject
    private lateinit var connectionFactory: ConnectionFactory
}
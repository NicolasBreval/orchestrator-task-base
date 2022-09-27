package org.nitb.orchestrator2.task.util

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("application")
class SystemProperties {

    companion object {
        const val APPLICATION_QUEUE_SYSTEM = "application.queue.system"
        const val ACTIVEMQ_QUEUE_SYSTEM_OPTION = "activemq"
        const val RABBITMQ_QUEUE_SYSTEM_OPTION = "rabbitmq"
    }

    var queueSystem: String? = null
}
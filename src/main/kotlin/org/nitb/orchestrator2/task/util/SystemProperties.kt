package org.nitb.orchestrator2.task.util

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("application")
class SystemProperties {

    companion object {
        const val MQ_TYPE = "orchestrator.mq.type"
        const val ACTIVEMQ_QUEUE_SYSTEM_OPTION = "activemq"
        const val RABBITMQ_QUEUE_SYSTEM_OPTION = "rabbitmq"
    }

    var queueSystem: String? = null
}
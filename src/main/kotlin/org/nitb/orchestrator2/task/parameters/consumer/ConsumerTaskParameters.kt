package org.nitb.orchestrator2.task.parameters.consumer

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.nitb.orchestrator2.task.annotation.NoArgs
import org.nitb.orchestrator2.task.parameters.TaskParameters

/**
 * List of parameters that a MQ consumer task needs
 */
@NoArgs
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "List of parameters that a MQ consumer task needs")
open class ConsumerTaskParameters(
    timeout: Long = -1,
    receivers: Array<String> = arrayOf(),
    concurrency: Int = 1,
    /**
     * If is true, task uses default MQ configuration to create queue and receive parameters
     */
    @param:JsonProperty(MQ_INTERNAL, required = false, defaultValue = "false")
    @get:JsonProperty(MQ_INTERNAL, required = false, defaultValue = "false")
    @Schema(
        name = MQ_INTERNAL,
        description = "If is true, task uses default MQ configuration to create queue and receive parameters",
        required = false,
        example = "false",
        defaultValue = "true"
    )
    val internal: Boolean = true,

    /**
     * Indicates type of technology used for consumer. This property only applies if [ConsumerTaskParameters.MQ_INTERNAL] is false
     */
    @param:JsonProperty(MQ_TYPE, required = false, defaultValue = "null")
    @get:JsonProperty(MQ_TYPE, required = false, defaultValue = "null")
    @Schema(
        name = MQ_TYPE,
        description = "Indicates type of technology used for consumer. This property only applies if $MQ_INTERNAL is false",
        required = false,
        example = "rabbitmq",
        defaultValue = "null",
        allowableValues = ["rabbitmq", "activemq"]
    )
    val type: String? = null,

    /**
     * URI used for MQ server connection
     */
    @param:JsonProperty(MQ_URI, required = false, defaultValue = "null")
    @get:JsonProperty(MQ_URI, required = false, defaultValue = "null")
    @Schema(
        name = MQ_URI,
        description = "URI used for MQ server connection",
        required = false,
        example = "amqp://user:password@host:port/vhost | failover:(tcp://localhost:61616,tcp://remotehost:61616)",
        defaultValue = "null"
    )
    val uri: String? = null,

    /**
     * Username used for MQ server connection
     */
    @param:JsonProperty(MQ_USERNAME, required = false, defaultValue = "null")
    @get:JsonProperty(MQ_USERNAME, required = false, defaultValue = "null")
    @Schema(
        name = MQ_USERNAME,
        description = "Username used for MQ server connection",
        required = false,
        example = "user",
        defaultValue = "null"
    )
    val username: String? = null,

    /**
     * Password used for MQ server connection
     */
    @param:JsonProperty(MQ_PASSWORD, required = false, defaultValue = "null")
    @get:JsonProperty(MQ_PASSWORD, required = false, defaultValue = "null")
    @Schema(
        name = MQ_PASSWORD,
        description = "Password used for MQ server connection",
        required = false,
        example = "password",
        defaultValue = "null"
    )
    val password: String? = null
) : TaskParameters(timeout, receivers, concurrency) {

    companion object {
        private const val MQ_INTERNAL = "mq.internal"
        private const val MQ_TYPE = "mq.type"
        private const val MQ_URI = "mq.uri"
        private const val MQ_USERNAME = "mq.username"
        private const val MQ_PASSWORD = "mq.password"
    }
}
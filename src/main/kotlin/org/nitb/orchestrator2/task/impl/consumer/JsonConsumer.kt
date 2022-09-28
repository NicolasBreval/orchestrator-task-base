package org.nitb.orchestrator2.task.impl.consumer

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Prototype
import jakarta.inject.Named
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.nitb.orchestrator2.task.parameters.consumer.ConsumerTaskParameters
import java.time.OffsetDateTime

@Suppress("UNUSED")
@Prototype
@Named("JSON-CONSUMER")
class JsonConsumer(
    @Parameter
    name: String,
    @Parameter
    parameters: Map<String, Any>,
    applicationContext: ApplicationContext
): ConsumerTask<ConsumerTaskParameters, DataFrame<*>>(name, parameters, ConsumerTaskParameters::class.java, applicationContext) {

    override fun onLaunch(
        inputMessage: DataFrame<*>?,
        sender: String,
        dispatchTime: OffsetDateTime
    ): DataFrame<*>? {
        return inputMessage
    }

    override fun onException(e: Exception, inputMessage: DataFrame<*>?, sender: String, dispatchTime: OffsetDateTime) {
        // do nothing
    }

    override fun onEnd(inputMessage: DataFrame<*>?, sender: String, dispatchTime: OffsetDateTime) {
        // do nothing
    }

    override fun onTimeout(inputMessage: DataFrame<*>?, sender: String, dispatchTime: OffsetDateTime) {
        // do nothing
    }
}
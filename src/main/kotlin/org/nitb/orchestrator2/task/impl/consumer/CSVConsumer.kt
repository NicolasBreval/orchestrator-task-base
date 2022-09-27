package org.nitb.orchestrator2.task.impl.consumer

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Prototype
import jakarta.inject.Named
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ParserOptions
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.nitb.orchestrator2.task.parameters.consumer.CSVConsumerTaskParameters
import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.util.*

@Suppress("UNUSED")
@Prototype
@Named("CSV-CONSUMER")
class CSVConsumer (
    @Parameter
    name: String,
    @Parameter
    parameters: Map<String, Any>,
    applicationContext: ApplicationContext
): ConsumerTask<CSVConsumerTaskParameters, ByteArray>(name, parameters, CSVConsumerTaskParameters::class.java, applicationContext) {
    override fun onLaunch(inputMessage: ByteArray?, sender: String, dispatchTime: LocalDateTime): DataFrame<*>? {
        return inputMessage?.let { bytes ->
            ByteArrayInputStream(bytes).use { stream ->
                DataFrame.readCSV(stream, delimiter = taskParameters.delimiter, isCompressed = taskParameters.compressed,
                    parserOptions = ParserOptions(locale = Locale(taskParameters.languageCode), nullStrings = taskParameters.nullStrings))
            }
        }
    }

    override fun onException(e: Exception, inputMessage: ByteArray?, sender: String, dispatchTime: LocalDateTime) {

    }

    override fun onEnd(inputMessage: ByteArray?, sender: String, dispatchTime: LocalDateTime) {

    }

    override fun onTimeout(inputMessage: ByteArray?, sender: String, dispatchTime: LocalDateTime) {

    }

}
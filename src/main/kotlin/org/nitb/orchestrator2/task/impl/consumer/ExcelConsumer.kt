package org.nitb.orchestrator2.task.impl.consumer

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Prototype
import jakarta.inject.Named
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.nitb.orchestrator2.task.parameters.consumer.ExcelConsumerTaskParameters
import java.io.ByteArrayInputStream
import java.time.OffsetDateTime

@Suppress("UNUSED")
@Prototype
@Named("EXCEL-CONSUMER")
class ExcelConsumer (
    @Parameter
    name: String,
    @Parameter
    parameters: Map<String, Any>,
    applicationContext: ApplicationContext
): ConsumerTask<ExcelConsumerTaskParameters, ByteArray>(name, parameters, ExcelConsumerTaskParameters::class.java, applicationContext) {

    override fun onLaunch(inputMessage: ByteArray?, sender: String, dispatchTime: OffsetDateTime): DataFrame<*>? {
        return inputMessage?.let { bytes ->
            ByteArrayInputStream(bytes).use { stream ->
                DataFrame.readExcel(stream, sheetName = taskParameters.sheetName)
            }
        }
    }

    override fun onException(e: Exception, inputMessage: ByteArray?, sender: String, dispatchTime: OffsetDateTime) {

    }

    override fun onEnd(inputMessage: ByteArray?, sender: String, dispatchTime: OffsetDateTime) {

    }

    override fun onTimeout(inputMessage: ByteArray?, sender: String, dispatchTime: OffsetDateTime) {

    }
}
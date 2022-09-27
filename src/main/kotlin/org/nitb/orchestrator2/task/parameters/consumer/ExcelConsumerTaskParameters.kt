package org.nitb.orchestrator2.task.parameters.consumer

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

class ExcelConsumerTaskParameters (
    timeout: Long = -1,
    receivers: Array<String> = arrayOf(),
    concurrency: Int = 1,
    internal: Boolean = true,
    type: String? = null,
    uri: String? = null,
    username: String? = null,
    password: String? = null,
    /**
     * Name of sheet to extract from Excel file as a dataframe. If this property is not set, uses the first sheet of file
     */
    @param:JsonProperty(SHEET_NAME, required = false, defaultValue = "null")
    @get:JsonProperty(SHEET_NAME, required = false, defaultValue = "null")
    @Schema(name = SHEET_NAME, description = "Name of sheet to extract from Excel file as a dataframe. If this property is not set, uses the first sheet of file",
        required = false, example = "Sheet 1", defaultValue = "null")
    val sheetName: String? = null
): ConsumerTaskParameters(timeout, receivers, concurrency, internal, type, uri, username, password) {

    companion object {
        private const val SHEET_NAME = "excel.sheet-name"
    }
}
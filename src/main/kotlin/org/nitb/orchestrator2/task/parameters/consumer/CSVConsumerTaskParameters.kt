package org.nitb.orchestrator2.task.parameters.consumer

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.nitb.orchestrator2.task.annotation.TaskParameterCheck
import org.nitb.orchestrator2.task.exception.IllegalTaskParameterValueException
import java.util.*

open class CSVConsumerTaskParameters(
    timeout: Long = -1,
    receivers: Array<String> = arrayOf(),
    concurrency: Int = 1,
    internal: Boolean = true,
    type: String? = null,
    uri: String? = null,
    username: String? = null,
    password: String? = null,
    /**
     * Delimiter character used to deserialize cells of CSV string
     */
    @param:JsonProperty(DELIMITER, required = false, defaultValue = ",")
    @get:JsonProperty(DELIMITER, required = false, defaultValue = ",")
    @Schema(name = DELIMITER, description = "Delimiter character used to deserialize cells of CSV string", required = false, example = "\\t", defaultValue = ",")
    val delimiter: Char = ',',
    /**
     * Language's code used to define locales for CSV parsing, this is used to select some parser's parameters, like decimal separator
     */
    @param:JsonProperty(LANGUAGE_CODE, required = false, defaultValue = "System's default")
    @get:JsonProperty(LANGUAGE_CODE, required = false, defaultValue = "System's default")
    @Schema(name = LANGUAGE_CODE, description = "Language's code used to define locales for CSV parsing, this is used to select some parser's parameters, like decimal separator",
        required = false, example = "us", defaultValue = "System's default", allowableValues = ["https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes"])
    val languageCode: String = Locale.getDefault().language,
    /**
     * If is true, decompress content as a GZIP file and, then, parses de CSV file to an object
     */
    @param:JsonProperty(COMPRESSED, required = false, defaultValue = "false")
    @get:JsonProperty(COMPRESSED, required = false, defaultValue = "false")
    @Schema(name = COMPRESSED, description = "If is true, decompress content as a GZIP file and, then, parses de CSV file to an object",
        required = false, example = "true", defaultValue = "false")
    val compressed: Boolean = false,
    /**
     * List of strings considered as null values on deserialization
     */
    @param:JsonProperty(NULL_STRINGS, required = false, defaultValue = "[]")
    @get:JsonProperty(NULL_STRINGS, required = false, defaultValue = "[]")
    @Schema(name = NULL_STRINGS, description = "List of strings considered as null values on deserialization",
        required = false, example = """["NaN", "NULL", "N/A"]""", defaultValue = "[]")
    val nullStrings: Set<String> = setOf()
): ConsumerTaskParameters(timeout, receivers, concurrency, internal, type, uri, username, password) {

    companion object {
        private const val DELIMITER = "csv.delimiter"
        private const val LANGUAGE_CODE = "csv.language-code"
        private const val COMPRESSED = "csv.compressed"
        private const val NULL_STRINGS = "csv.null-strings"
    }

    @Suppress("UNUSED")
    @TaskParameterCheck
    protected fun checkCountryCode() {
        try {
            Locale(languageCode)
        } catch (e: Exception) {
            throw IllegalTaskParameterValueException("Language code $languageCode is not valid")
        }
    }
}
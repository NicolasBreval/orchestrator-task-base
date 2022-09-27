package org.nitb.orchestrator2.task.serialization.serializers

import com.fasterxml.jackson.core.JsonGenerationException
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.typeClass

class DataframeSerializer: StdSerializer<DataFrame<*>>(DataFrame::class.java) {

    override fun serialize(value: DataFrame<*>?, gen: JsonGenerator?, serializers: SerializerProvider?) {

        if (value == null)
            throw JsonGenerationException("Unable to serialize a null object", gen)

        gen?.writeStartObject()
        gen?.writeArrayFieldStart("columns")
        for (column in value.columns()) {
            gen?.writeStartObject()
            gen?.writeStringField("name", column.name())
            gen?.writeNumberField("position", value.getColumnIndex(column.name()))
            gen?.writeStringField("type", column.typeClass.javaObjectType.name)
            gen?.writeArrayFieldStart("content")
            for (element in column.values()) {
                gen?.writeObject(element)
            }
            gen?.writeEndArray()
            gen?.writeEndObject()
        }
        gen?.writeEndArray()
        gen?.writeEndObject()
    }
}
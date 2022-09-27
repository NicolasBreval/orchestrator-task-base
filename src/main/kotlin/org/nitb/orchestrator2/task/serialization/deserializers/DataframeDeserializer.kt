package org.nitb.orchestrator2.task.serialization.deserializers

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.*

class DataframeDeserializer : StdDeserializer<DataFrame<*>>(DataFrame::class.java) {

    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): DataFrame<*> {
        var dataframe = DataFrame.empty()

        val codec = p?.codec
        val tree = codec?.readTree<JsonNode>(p)

        if (tree?.get("columns")?.isArray != true) {
            throw JsonParseException(p, "Invalid JSON object, field 'columns' is not valid")
        }

        tree.get("columns")?.asSequence()?.sortedBy { element ->
            element.get("position").let {
                if (!it.isNumber) throw JsonParseException(
                    p,
                    "Invalid JSON object, any column has no 'position' field"
                ); it
            }.asInt()
        }?.forEachIndexed { index, element ->
            val name = element.get("name").let {
                if (!it.isTextual) throw JsonParseException(
                    p,
                    "Invalid JSON object, column number $index has no 'name' field"
                ); it
            }.asText()
            val type = element.get("type").let {
                if (!it.isTextual) throw JsonParseException(
                    p,
                    "Invalid JSON object, column $name has no 'type' field"
                ); it
            }.asText().let { Class.forName(it) }
            val content = element.get("content")

            if (!content.isArray) {
                throw JsonParseException(p, "Invalid JSON object, field 'content' on column $name is not valid")
            }

            val column = content.asSequence().map { contentElement ->
                codec.treeToValue(contentElement, type)
            }.toList().toColumn(name, Infer.Type)

            dataframe = dataframe.add(column)
        }

        return dataframe
    }
}
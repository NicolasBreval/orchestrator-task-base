package org.nitb.orchestrator2.task.serialization

import com.fasterxml.jackson.databind.module.SimpleModule
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.nitb.orchestrator2.task.serialization.deserializers.DataframeDeserializer
import org.nitb.orchestrator2.task.serialization.serializers.DataframeSerializer

class DataframeModule: SimpleModule() {

    init {
        this.addSerializer(DataframeSerializer())
        this.addDeserializer(DataFrame::class.java, DataframeDeserializer())
    }
}
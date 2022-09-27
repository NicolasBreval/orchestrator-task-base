package org.nitb.orchestrator2.task.js

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import java.lang.IllegalArgumentException

@Suppress("UNUSED")
object JavaScriptInteroperabilityUtils {

    @JvmStatic
    fun createDataFrameFromColumns(columns: Array<String>, values: Array<Array<Any?>>): DataFrame<*> {
        if (columns.size != values.size) throw IllegalArgumentException("Error on parameters number")
        return dataFrameOf(*columns.zip(values.map { it.toList() }).toTypedArray())
    }

    @JvmStatic
    fun createEmptyDataFrame(): DataFrame<*> {
        return DataFrame.empty(0)
    }

    @JvmStatic
    fun sleep(millis: Long) {
        Thread.sleep(millis)
    }

}
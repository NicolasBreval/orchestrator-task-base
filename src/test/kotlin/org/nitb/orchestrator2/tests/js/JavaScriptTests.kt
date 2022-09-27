package org.nitb.orchestrator2.tests.js

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.getColumn
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.nitb.orchestrator2.task.js.JavaScriptInterpreter

class JavaScriptTests {

    @Test
    fun addTest() {
        val script = """
            var result = a + b;
        """.trimIndent()

        val processed = JavaScriptInterpreter.process<Int>(script, Pair("a", 3), Pair("b", 4))

        assertEquals(7, processed)
    }

    @Test
    fun callJavaTest() {
        val script = """
            var result = Java.type('java.lang.Math').PI;
        """.trimIndent()

        val processed = JavaScriptInterpreter.process<Double>(script)

        assertEquals(Math.PI, processed)
    }

    @Test
    fun createDataframeTest() {
        val script = """
            var InteroperabilityUtils = Java.type('org.nitb.orchestrator2.task.js.JavaScriptInteroperabilityUtils');
            var result = InteroperabilityUtils.createDataFrameFromColumns(['names', 'ages', 'heights'], [['John', 'Mary', 'George', 'Ann'], [18, 23, 17, 21], [1.7, 1.6, 1.9, 1.5]]);
        """.trimIndent()

        val processed = JavaScriptInterpreter.process<DataFrame<*>>(script)

        assertNotNull(processed)
        assertInstanceOf(DataFrame::class.java, processed)
        assertArrayEquals(arrayOf("John", "Mary", "George", "Ann"), (processed as DataFrame<*>).getColumn("names").values().toList().toTypedArray())
        assertArrayEquals(arrayOf(18, 23, 17, 21), processed.getColumn("ages").values().toList().toTypedArray())
        assertArrayEquals(arrayOf(1.7, 1.6, 1.9, 1.5), processed.getColumn("heights").values().toList().toTypedArray())
    }
}
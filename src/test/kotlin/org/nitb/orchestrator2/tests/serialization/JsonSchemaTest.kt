package org.nitb.orchestrator2.tests.serialization

import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.nitb.orchestrator2.task.parameters.TaskParameters
import org.nitb.orchestrator2.task.parameters.consumer.ConsumerTaskParameters
import org.nitb.orchestrator2.task.parameters.consumer.ScriptableConsumerTaskParameters

@MicronautTest
class JsonSchemaTest {

    // region TASK PARAMETERS

    @Test
    fun taskParametersSchemaTest() {
        val schema = jsonSchemaGenerator.generateSchema(TaskParameters::class.java)

        assertTrue(schema.isObjectSchema)

        assertTrue((schema as ObjectSchema).properties["task.timeout"]?.isIntegerSchema ?: false)
        assertTrue(schema.properties["task.receivers"]?.isArraySchema ?: false)
        assertTrue(schema.properties["task.concurrency"]?.isIntegerSchema ?: false)
    }

    // endregion

    // region MQ CONSUMER TASK PARAMETERS

    @Test
    fun mqConsumerTaskParametersSchemaTest() {
        val schema = jsonSchemaGenerator.generateSchema(ConsumerTaskParameters::class.java)

        assertTrue(schema.isObjectSchema)

        assertTrue((schema as ObjectSchema).properties["task.timeout"]?.isIntegerSchema ?: false)
        assertTrue(schema.properties["task.receivers"]?.isArraySchema ?: false)
        assertTrue(schema.properties["task.concurrency"]?.isIntegerSchema ?: false)
        assertTrue(schema.properties["mq.internal"]?.isBooleanSchema ?: false)
        assertTrue(schema.properties["mq.type"]?.isStringSchema ?: false)
        assertTrue(schema.properties["mq.uri"]?.isStringSchema ?: false)
        assertTrue(schema.properties["mq.username"]?.isStringSchema ?: false)
        assertTrue(schema.properties["mq.password"]?.isStringSchema ?: false)
    }

    // endregion

    // region MQ SCRIPTABLE CONSUMER TASK PARAMETERS

    @Test
    fun mqScriptableConsumerTaskParametersSchemaTest() {
        val schema = jsonSchemaGenerator.generateSchema(ScriptableConsumerTaskParameters::class.java)

        assertTrue(schema.isObjectSchema)

        assertTrue((schema as ObjectSchema).properties["task.timeout"]?.isIntegerSchema ?: false)
        assertTrue(schema.properties["task.receivers"]?.isArraySchema ?: false)
        assertTrue(schema.properties["task.concurrency"]?.isIntegerSchema ?: false)
        assertTrue(schema.properties["mq.internal"]?.isBooleanSchema ?: false)
        assertTrue(schema.properties["mq.type"]?.isStringSchema ?: false)
        assertTrue(schema.properties["mq.uri"]?.isStringSchema ?: false)
        assertTrue(schema.properties["mq.username"]?.isStringSchema ?: false)
        assertTrue(schema.properties["mq.password"]?.isStringSchema ?: false)
        assertTrue(schema.properties["mq.script"]?.isStringSchema ?: false)
    }

    // endregion

    // region COMMON

    @Inject
    private lateinit var jsonSchemaGenerator: JsonSchemaGenerator

    // endregion
}
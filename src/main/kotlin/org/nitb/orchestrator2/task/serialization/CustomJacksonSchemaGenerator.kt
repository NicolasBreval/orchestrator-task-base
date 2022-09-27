package org.nitb.orchestrator2.task.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import jakarta.inject.Inject

@Suppress("UNUSED")
@Factory
@Requires(classes = [ObjectMapper::class])
class CustomJacksonSchemaGenerator {

    @Bean
    fun createSchemaGenerator(): JsonSchemaGenerator = JsonSchemaGenerator(jsonMapper)

    @Inject
    private lateinit var jsonMapper: ObjectMapper
}
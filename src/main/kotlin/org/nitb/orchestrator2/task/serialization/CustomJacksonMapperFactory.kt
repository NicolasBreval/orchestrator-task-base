package org.nitb.orchestrator2.task.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import jakarta.inject.Singleton

@Suppress("UNUSED")
@Singleton
class CustomJacksonMapperFactory: BeanCreatedEventListener<ObjectMapper> {

    override fun onCreated(event: BeanCreatedEvent<ObjectMapper>): ObjectMapper {
        val mapper = event.bean
        mapper.registerModule(DataframeModule())
        return mapper
    }
}
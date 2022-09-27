package org.nitb.orchestrator2.task.util

import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.nitb.orchestrator2.task.impl.BaseTask

@Singleton
class TaskBuilder {

    fun newTask(type: String, name: String, parameters: Map<String, Any?>): BaseTask<*, *> {
        return applicationContext.createBean(BaseTask::class.java, Qualifiers.byName(type), name, parameters)
    }

    @Inject
    private lateinit var applicationContext: ApplicationContext

}
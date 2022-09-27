package org.nitb.orchestrator2.task.impl.cyclical

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Prototype
import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.cron.CronExpression
import kotlinx.coroutines.*
import kotlinx.coroutines.Job
import org.nitb.orchestrator2.task.impl.BaseTask
import org.nitb.orchestrator2.task.mq.impl.MQManager
import org.nitb.orchestrator2.task.parameters.cyclical.CyclicalTaskParameters
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.LinkedBlockingDeque

@Prototype
@Requires(bean = MQManager::class)
abstract class CyclicalTask<P: CyclicalTaskParameters>(
    @Parameter
    name: String,
    @Parameter
    parameters: Map<String, Any>,
    parametersClass: Class<P>,
    applicationContext: ApplicationContext
): BaseTask<P, Nothing>(name, parameters, parametersClass, applicationContext, true) {

    private val isCron = taskParameters.cron.isNotEmpty()

    private lateinit var mainJob: Job

    private val jobQueue: LinkedBlockingDeque<Job> = LinkedBlockingDeque()

    @OptIn(DelicateCoroutinesApi::class)
    override fun onConstruct() {
        super.onConstruct()

        mainJob = GlobalScope.launch {
            if (!isCron) {
                scheduleJob()
            } else {
                while (true) {
                    scheduleJob()
                    makeDelay()
                }
            }
        }
    }

    override fun onDestroy() {
        jobQueue.forEach {
            it.cancel()
        }
        mainJob.cancel()

        super.onDestroy()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun scheduleJob() {
        jobQueue.add(GlobalScope.launch {
            if (!isCron) {
                while (true) {
                    launch(UUID.randomUUID().toString(), null, name, LocalDateTime.now())
                    makeDelay()
                }
            } else {
                launch(UUID.randomUUID().toString(), null, name, LocalDateTime.now())
            }
        })
    }

    private suspend fun makeDelay() {
        if (isCron) {
            delay(CronExpression.create(taskParameters.cron).nextTimeAfter(ZonedDateTime.now()).toInstant().toEpochMilli())
        } else {
            delay(taskParameters.fixedDelay)
        }
    }
}
package org.nitb.orchestrator2.task.model

import org.nitb.orchestrator2.task.enums.TaskStatus
import java.math.BigInteger
import java.time.OffsetDateTime

data class TaskInfo(
    val starts: BigInteger,
    val stops: BigInteger,
    val successLaunches: BigInteger,
    val errorLaunches: BigInteger,
    val status: TaskStatus,
    val implementationTime: OffsetDateTime,
    val lastLaunchDate: OffsetDateTime? = null
)
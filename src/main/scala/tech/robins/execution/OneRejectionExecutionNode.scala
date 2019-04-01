package tech.robins.execution

import java.util.UUID

import akka.actor
import akka.actor.{ActorRef, Props}
import tech.robins._
import tech.robins.caching.{Cache, FixedSizeRoundRobinCache}

class OneRejectionExecutionNode(
  id: UUID,
  delaySimulator: DelaySimulator,
  realTimeDelays: Boolean,
  executionUnitsPerMinute: Double,
  resourceCache: Cache[Resource],
  taskAccountant: ActorRef,
  taskScheduler: ActorRef
) extends AbstractExecutionNode(
      id,
      delaySimulator,
      realTimeDelays,
      executionUnitsPerMinute,
      resourceCache,
      taskAccountant,
      taskScheduler
    ) {
  private val rejectedTasksCacheSize = 32
  private val rejectedTasksCache = new FixedSizeRoundRobinCache[Task](rejectedTasksCacheSize)

  protected def onExecuteTask(task: Task): Unit = {
    rejectedTasksCache removeIfPresent task
    executeTask(task)
    requestNewTaskFromScheduler()
  }

  protected def shouldAcceptTask(task: Task): Boolean = {
    log.info(s"Task has been offered: $task")
    val aRequiredResourceIsPresent = resourceCache.exists(task.requiredResources.contains)
    if (aRequiredResourceIsPresent) {
      log.info("Accepting task because a required resource is present")
      true
    } else if (rejectedTasksCache contains task) {
      log.info("Accepting task because it has been offered more than once.")
      true
    } else {
      log.info("Rejecting task because this node does not have any of the required resources. First time offered.")
      rejectedTasksCache add task
      false
    }
  }
}

object OneRejectionExecutionNode extends ExecutionNodeBuilder {
  def props(
    akkaScheduler: actor.Scheduler,
    simulationConfiguration: SimulationConfiguration,
    executionUnitsPerMinute: Double,
    resourceCache: Cache[Resource],
    taskAccountant: ActorRef,
    taskScheduler: ActorRef
  ): Props =
    Props(
      new OneRejectionExecutionNode(
        UUID.randomUUID(),
        new DelaySimulator(akkaScheduler),
        simulationConfiguration.realTimeDelaysEnabled,
        executionUnitsPerMinute,
        resourceCache,
        taskAccountant,
        taskScheduler
      )
    )
}

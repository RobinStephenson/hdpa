package tech.robins.execution

import java.util.UUID

import akka.actor
import akka.actor.{ActorRef, Props}
import tech.robins._

class OneRejectionExecutionNode(
  id: UUID,
  delaySimulator: DelaySimulator,
  realTimeDelays: Boolean,
  executionUnitsPerMinute: Double,
  taskAccountant: ActorRef,
  taskScheduler: ActorRef
) extends ExecutionNode(id, delaySimulator, realTimeDelays, executionUnitsPerMinute, taskAccountant, taskScheduler)
    with SimpleOnExecute {
  private val rejectedTasksCacheSize = 32
  private val rejectedTasksCache = new FixedSizeRoundRobinCache[Task](rejectedTasksCacheSize)

  protected def shouldAcceptTask(task: Task): Boolean = {
    log.info(s"Task has been offered: $task")
    val aRequiredResourceIsPresent = resources.exists(task.requiredResources.contains)
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

object OneRejectionExecutionNode {
  def props(
    akkaScheduler: actor.Scheduler,
    simulationConfiguration: SimulationConfiguration,
    executionUnitsPerMinute: Double,
    taskAccountant: ActorRef,
    taskScheduler: ActorRef
  ): Props =
    Props(
      new OneRejectionExecutionNode(
        UUID.randomUUID(),
        new DelaySimulator(akkaScheduler),
        simulationConfiguration.realTimeDelaysEnabled,
        executionUnitsPerMinute,
        taskAccountant,
        taskScheduler
      )
    )
}

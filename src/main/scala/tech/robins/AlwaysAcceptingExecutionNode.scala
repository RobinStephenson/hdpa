package tech.robins
import java.util.UUID

import akka.actor
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

class AlwaysAcceptingExecutionNode(
  id: UUID,
  delaySimulator: DelaySimulator,
  realTimeDelays: Boolean,
  executionUnitsPerMinute: Double,
  taskAccountant: ActorRef,
  taskScheduler: ActorRef
) extends ExecutionNode(id, delaySimulator, realTimeDelays, executionUnitsPerMinute, taskAccountant, taskScheduler)
    with Actor
    with ActorLogging
    with SimpleOnExecute {

  protected def shouldAcceptTask(task: Task): Boolean = true
}

object AlwaysAcceptingExecutionNode {

  def props(
    akkaScheduler: actor.Scheduler,
    simulationConfiguration: SimulationConfiguration,
    executionUnitsPerMinute: Double,
    taskAccountant: ActorRef,
    taskScheduler: ActorRef
  ): Props =
    Props(
      new AlwaysAcceptingExecutionNode(
        UUID.randomUUID(),
        new DelaySimulator(akkaScheduler),
        simulationConfiguration.realTimeDelaysEnabled,
        executionUnitsPerMinute,
        taskAccountant,
        taskScheduler
      )
    )
}

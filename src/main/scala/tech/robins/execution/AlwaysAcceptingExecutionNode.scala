package tech.robins.execution

import java.util.UUID

import akka.actor
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import tech.robins._
import tech.robins.caching.Cache

class AlwaysAcceptingExecutionNode(
  id: UUID,
  delaySimulator: DelaySimulator,
  realTimeDelays: Boolean,
  executionUnitsPerMinute: Double,
  resourceCache: Cache[Resource],
  taskAccountant: ActorRef,
  taskScheduler: ActorRef
) extends ExecutionNode(
      id,
      delaySimulator,
      realTimeDelays,
      executionUnitsPerMinute,
      resourceCache,
      taskAccountant,
      taskScheduler
    )
    with Actor
    with ActorLogging
    with SimpleOnExecute {

  protected def shouldAcceptTask(task: Task): Boolean = true
}

object AlwaysAcceptingExecutionNode extends ExecutionNodeBuilder {

  def props(
    akkaScheduler: actor.Scheduler,
    simulationConfiguration: SimulationConfiguration,
    executionUnitsPerMinute: Double,
    resourceCache: Cache[Resource],
    taskAccountant: ActorRef,
    taskScheduler: ActorRef
  ): Props =
    Props(
      new AlwaysAcceptingExecutionNode(
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

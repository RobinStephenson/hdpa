package tech.robins.execution
import akka.actor
import akka.actor.{ActorRef, Props}
import tech.robins.{Resource, SimulationConfiguration}
import tech.robins.caching.Cache

trait ExecutionNodeBuilder {
  def props(
    akkaScheduler: actor.Scheduler,
    simulationConfiguration: SimulationConfiguration,
    executionUnitsPerMinute: Double,
    resourceCache: Cache[Resource],
    taskAccountant: ActorRef,
    taskScheduler: ActorRef
  ): Props
}

package tech.robins
import akka.actor.{ActorRef, ActorSystem, Address, RootActorPath}
import akka.cluster.Cluster
import tech.robins.caching.CacheLibrary
import tech.robins.execution.ExecutionNodeLibrary

import scala.concurrent.Await
import scala.concurrent.duration._

// TODO shut self down when cluster master goes down
object SimulationApplicationWorker extends SimulationApplication {
  val roleName = "simulationWorker"

  def main(args: Array[String]): Unit = {
    val roles = Array(roleName)
    val executionUnitsPerMinute = extractExecutionUnitsPerMinuteFromArgs(args)
    val executionNodeName = "AlwaysAccepting" // TODO get from args
    val resourceCacheName = "FixedSizeRoundRobin" // TODO get from args
    val resourceCacheSize = 4 // TODO get from args
    // TODO store information about the nodes in the cluster (when they arrived/left, exec units per min, resource cache) in report
    val tsConfig = getConfig(roles)
    val simConfig: SimulationConfiguration = SimulationConfiguration(tsConfig)
    val system = ActorSystem("SimulationSystem", tsConfig)
    val cluster = Cluster(system)
    cluster.registerOnMemberUp(
      () =>
        createExecutionNodeAndRegisterWithMaster(
          cluster,
          system,
          simConfig,
          executionUnitsPerMinute,
          executionNodeName,
          resourceCacheName,
          resourceCacheSize
      )
    )
  }

  private def createExecutionNodeAndRegisterWithMaster(
    cluster: Cluster,
    system: ActorSystem,
    simConfig: SimulationConfiguration,
    executionUnitsPerMinute: Double,
    executionNodeName: String,
    resourceCacheName: String,
    resourceCacheSize: Int
  ): Unit = {
    val simulationMasterNodeAddress = getSimulationMasterNodeAddress(cluster)
    val taskAccountant: ActorRef = getTaskAccountant(system, simulationMasterNodeAddress)
    val taskScheduler = getTaskScheduler(system, simulationMasterNodeAddress)
    val resourceCache = CacheLibrary.byName[Resource](resourceCacheName, resourceCacheSize)
    val executionNodeProps = ExecutionNodeLibrary
      .builders(executionNodeName)
      .props(system.scheduler, simConfig, executionUnitsPerMinute, resourceCache, taskAccountant, taskScheduler)
    system.actorOf(executionNodeProps, "executionNode")
  }

  private def getSimulationMasterNodeAddress(cluster: Cluster) =
    cluster.state.members
      .find(_.hasRole(SimulationApplicationMaster.roleName))
      .getOrElse(throw new IllegalStateException(s"Could not find a simulation master in cluster: ${cluster.state}"))
      .address

  private def getTaskScheduler(system: ActorSystem, simulationMasterNodeAddress: Address) =
    Await.result(
      system
        .actorSelection(RootActorPath(simulationMasterNodeAddress) / "user" / "taskScheduler")
        .resolveOne(1.minute),
      1.minute
    )

  private def getTaskAccountant(system: ActorSystem, simulationMasterNodeAddress: Address) =
    Await.result(
      system
        .actorSelection(RootActorPath(simulationMasterNodeAddress) / "user" / "taskAccountant")
        .resolveOne(1.minute),
      1.minute
    )

  private def extractExecutionUnitsPerMinuteFromArgs(args: Array[String]): Double = {
    if (args.isEmpty) throw new IllegalArgumentException("Provide an execution units per minute parameter.")
    try args.head.toDouble
    catch {
      case _: NumberFormatException =>
        throw new IllegalArgumentException("Execution units per minute must be a double.")
    }
  }
}

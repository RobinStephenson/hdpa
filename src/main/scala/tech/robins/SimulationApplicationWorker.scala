package tech.robins
import akka.actor.{ActorSystem, RootActorPath}
import akka.cluster.Cluster
import tech.robins.execution.OneRejectionExecutionNode

import scala.concurrent.Await
import scala.concurrent.duration._

// TODO shut self down when cluster master goes down
object SimulationApplicationWorker extends SimulationApplication {
  val roleName = "simulationWorker"

  def main(args: Array[String]): Unit = {
    val roles = Array(roleName)
    val executionUnitsPerMinute = extractExecutionUnitsPerMinuteFromArgs(args)
    val tsConfig = getConfig(roles)
    val simConfig: SimulationConfiguration = SimulationConfiguration(tsConfig)
    val system = ActorSystem("SimulationSystem", tsConfig)
    val cluster = Cluster(system)
    cluster.registerOnMemberUp(
      () => createExecutionNodeAndRegisterWithMaster(cluster, system, simConfig, executionUnitsPerMinute, args)
    )
  }

  // TODO easily change type of execution node from configuration
  private def createExecutionNodeAndRegisterWithMaster(
    cluster: Cluster,
    system: ActorSystem,
    simConfig: SimulationConfiguration,
    executionUnitsPerMinute: Double,
    args: Array[String]
  ): Unit = {

    val simulationMasterNodeAddress = cluster.state.members
      .find(_.hasRole(SimulationApplicationMaster.roleName))
      .getOrElse(throw new IllegalStateException(s"Could not find a simulation master in cluster: ${cluster.state}"))
      .address
    val taskAccountant = Await.result(
      system
        .actorSelection(RootActorPath(simulationMasterNodeAddress) / "user" / "taskAccountant")
        .resolveOne(1.minute),
      1.minute
    )
    val taskScheduler = Await.result(
      system
        .actorSelection(RootActorPath(simulationMasterNodeAddress) / "user" / "taskScheduler")
        .resolveOne(1.minute),
      1.minute
    )
    system.actorOf(
      OneRejectionExecutionNode
        .props(system.scheduler, simConfig, executionUnitsPerMinute, taskAccountant, taskScheduler),
      "executionNode"
    )
  }

  private def extractExecutionUnitsPerMinuteFromArgs(args: Array[String]): Double = {
    if (args.isEmpty) throw new IllegalArgumentException("Provide an execution units per minute parameter.")
    try args.head.toDouble
    catch {
      case _: NumberFormatException =>
        throw new IllegalArgumentException("Execution units per minute must be a double.")
    }
  }
}

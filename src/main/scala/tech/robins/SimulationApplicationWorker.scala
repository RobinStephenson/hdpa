package tech.robins
import akka.actor.{ActorRef, ActorSystem, Address, RootActorPath}
import akka.cluster.Cluster
import scopt.OptionParser
import tech.robins.caching.CacheLibrary
import tech.robins.execution.ExecutionNodeLibrary

import scala.concurrent.Await
import scala.concurrent.duration._

// TODO shut self down when cluster master goes down
// TODO easy way to start N execution nodes in separate processes
object SimulationApplicationWorker extends SimulationApplication {
  val roleName = "simulationWorker"

  def main(args: Array[String]): Unit = {
    val roles = Array(roleName)
    val workerConfig =
      ArgsParsing.parser.parse(args, WorkerConfiguration()).getOrElse(throw new IllegalArgumentException())
    // TODO store information about the nodes in the cluster (when they arrived/left, exec units per min, resource cache) in report
    val tsConfig = getConfig(roles)
    val simConfig: SimulationConfiguration = SimulationConfiguration(tsConfig)
    val system = ActorSystem("SimulationSystem", tsConfig)
    val cluster = Cluster(system)
    cluster.registerOnMemberUp(() => createAndRegisterExecutionNode(cluster, system, simConfig, workerConfig))
  }

  private def createAndRegisterExecutionNode(
    cluster: Cluster,
    system: ActorSystem,
    simConfig: SimulationConfiguration,
    workerConfig: WorkerConfiguration
  ): Unit = {
    val simulationMasterNodeAddress = getSimulationMasterNodeAddress(cluster)
    val taskAccountant: ActorRef = getTaskAccountant(system, simulationMasterNodeAddress)
    val taskScheduler = getTaskScheduler(system, simulationMasterNodeAddress)
    val resourceCache = CacheLibrary.byName[Resource](workerConfig.resourceCacheName, workerConfig.resourceCacheSize)
    val executionNodeProps = ExecutionNodeLibrary
      .builders(workerConfig.executionNodeName)
      .props(system.scheduler, simConfig, workerConfig.execUnits, resourceCache, taskAccountant, taskScheduler)
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
}

case class WorkerConfiguration(
  execUnits: Double = 1,
  executionNodeName: String = "",
  resourceCacheName: String = "",
  resourceCacheSize: Int = 1
)

object ArgsParsing {
  val parser: OptionParser[WorkerConfiguration] = new OptionParser[WorkerConfiguration]("HDPA") {
    opt[Double]("execUnits")
      .required()
      .validate(units => if (units >= 1) success else failure("Execution units per minute must be >= 1"))
      .action((x, config) => config.copy(execUnits = x))

    opt[String]("executionNode")
      .required()
      .action((x, config) => config.copy(executionNodeName = x))

    opt[String]("resourceCache")
      .required()
      .action((x, config) => config.copy(resourceCacheName = x))

    opt[Int]("resourceCacheSize")
      .required()
      .validate(size => if (size >= 1) success else failure("Resource cache size must be >= 1"))
      .action((x, config) => config.copy(resourceCacheSize = x))
  }
}

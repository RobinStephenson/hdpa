package tech.robins

import java.nio.file.Paths
import java.time.{LocalDateTime, Duration => jDuration}
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit.NANOSECONDS

import AbstractWorkloadGenerator.{StartGeneratingWork, SubscribeToEndOfWorkGeneration}
import ExecutionNode.StartRequestingTasksFromScheduler
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection, Props, RootActorPath}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.cluster.ClusterEvent._

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

class SimulationController(simulationConfiguration: SimulationConfiguration) extends Actor with ActorLogging {
  import SimulationController._

  private var simulationStarted = false
  private var simulationStartTime: Option[LocalDateTime] = None

  private val executionNodesWaitingForSimulationStart = mutable.Set.empty[Member]

  private val cluster = Cluster(context.system)

  private val workloadGenerator =
    getWorkloadGeneratorByName(simulationConfiguration.workGenerationConfiguration.generatorName)

  private val scheduler = getSchedulerByName(simulationConfiguration.schedulerName)

  private val taskAccountant = context.system.actorOf(TaskAccountant.props(self), "taskAccountant")

  private def getActorForExecutionNode(executionNodeMember: Member): ActorSelection =
    context.actorSelection(RootActorPath(executionNodeMember.address) / "user" / "executionNode")

  private def getWorkloadGeneratorByName(name: String): ActorRef = {
    val props = WorkloadGeneratorLibrary.propsByName(name)(simulationConfiguration.workGenerationConfiguration)
    context.system.actorOf(props, "workloadGenerator")
  }

  private def getSchedulerByName(name: String): ActorRef = {
    val props = SchedulerLibrary.propsByName(name)
    context.system.actorOf(props, "taskScheduler")
  }

  private def handleUnusableMember(member: Member): Unit =
    if (!simulationStarted && member.roles.contains(SimulationApplicationWorker.roleName)) {
      executionNodesWaitingForSimulationStart remove member
      log.info(s"Member $member removed from execution nodes waiting for simulation start")
    }

  private def startSimulation(): Unit = {
    log.info("Starting up simulation")
    simulationStarted = true
    simulationStartTime = Some(LocalDateTime.now())

    workloadGenerator ! SubscribeToEndOfWorkGeneration(taskAccountant)
    workloadGenerator ! StartGeneratingWork(scheduler)

    executionNodesWaitingForSimulationStart
      .map(getActorForExecutionNode)
      .foreach(execNode => {
        log.info(s"Telling worker: $execNode to start requesting tasks from scheduler")
        execNode ! StartRequestingTasksFromScheduler
      })
    executionNodesWaitingForSimulationStart.clear()
  }

  private def simulationComplete(
    taskExecutionReports: Map[Task, TaskExecutionReport],
    workGenerationReport: WorkGenerationReport
  ): Unit = {
    log.info("Simulation completed! Generating report and terminating system.")
    val now = LocalDateTime.now()
    val simulationDuration = FiniteDuration(jDuration.between(simulationStartTime.get, now).toNanos, NANOSECONDS)
    val simulationReport =
      SimulationReport(simulationConfiguration, workGenerationReport, taskExecutionReports, simulationDuration)
    saveReport(simulationReport)
    context.system.terminate()
  }

  private def saveReport(report: SimulationReport): Unit = {
    log.info("Saving simulation report: {}", report)
    val fileNameDateFormat = DateTimeFormatter.ofPattern("yyyy-mm-dd-HH-mm-ss")
    val reportFileName = LocalDateTime.now().format(fileNameDateFormat) + ".json"
    val reportPath = Paths.get(".", simulationConfiguration.reportOutputPath)
    val savedFilePath = SimulationReportSaver.saveToFile(report, reportPath, reportFileName)
    log.info(s"Report saved to path: $savedFilePath")
  }

  private def memberUp(member: Member): Unit = {
    log.info(s"Member is Up: ${member.address} With roles: ${member.roles}")
    if (member.roles.contains(SimulationApplicationWorker.roleName)) {
      if (simulationStarted) {
        val executionNode = getActorForExecutionNode(member)
        log.info(s"Telling following new node to request tasks from scheduler: $executionNode")
        executionNode ! StartRequestingTasksFromScheduler
      } else {
        executionNodesWaitingForSimulationStart add member
        log.info(
          s"Member $member added to execution nodes waiting for simulation start. " +
            s"${executionNodesWaitingForSimulationStart.size} waiting."
        )
      }
    } else {
      log.info(s"Member does not have role ${SimulationApplicationWorker.roleName} so is not added to waiting workers.")
    }
  }

  private def unreachableMember(member: Member): Unit = {
    log.info("Member detected as unreachable: {}", member)
    handleUnusableMember(member)
  }

  private def memberRemoved(member: Member, previousStatus: MemberStatus): Unit = {
    log.info("Member is Removed: {} after {}", member.address, previousStatus)
    handleUnusableMember(member)
  }

  override def preStart(): Unit =
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive: Receive = {
    case StartSimulation                                => startSimulation()
    case MemberUp(member)                               => memberUp(member)
    case SimulationComplete(taskReports, workGenReport) => simulationComplete(taskReports, workGenReport)
    case UnreachableMember(member)                      => unreachableMember(member)
    case MemberRemoved(member, previousStatus)          => memberRemoved(member, previousStatus)
    case msg                                            => log.warning(s"Unhandled message in receive: $msg")
  }
}

object SimulationController {
  def props(simulationConfig: SimulationConfiguration): Props = Props(new SimulationController(simulationConfig))

  final case object StartSimulation
  final case class SimulationComplete(
    taskReports: Map[Task, TaskExecutionReport],
    workGenerationReport: WorkGenerationReport
  )
}

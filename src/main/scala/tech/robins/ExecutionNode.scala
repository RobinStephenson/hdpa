package tech.robins

import java.util.UUID
import java.util.concurrent.TimeUnit

import AbstractScheduler.RequestTaskForExecution
import TaskAccountant.TaskExecutionComplete
import akka.actor
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._

class ExecutionNode(
  id: UUID,
  delaySimulator: DelaySimulator,
  realTimeDelays: Boolean,
  executionUnitsPerMinute: Double,
  taskAccountant: ActorRef,
  taskScheduler: ActorRef
) extends Actor
    with ActorLogging {
  import ExecutionNode._
  import context.dispatcher

  private val resources: mutable.Set[Resource] = mutable.Set()

  private def getOrFetchResource(resource: Resource): FetchResult = {
    if (resources contains resource) {
      log.info(s"Fetching local resource $resource")
      FetchResult localResourceFetch resource
    } else {
      log.info(s"Fetching remote resource $resource")
      resources add resource
      FetchResult remoteResourceFetch resource
    }
  }

  private def calculateExecutionTime(executionUnits: Double): FiniteDuration =
    FiniteDuration((executionUnits / executionUnitsPerMinute).toLong, TimeUnit.MINUTES)

  private def executeTaskThenRequestAnother(task: Task): Unit = {
    executeTask(task)
    requestNewTaskFromScheduler()
  }

  private def executeTask(task: Task): Unit = {
    log.info(s"Executing task $task")
    val resourceFetchResults = task.requiredResources.map(getOrFetchResource)
    val totalResourceAccessDuration = resourceFetchResults.map(_.fetchDuration).reduce(_ + _)
    val executionDuration = calculateExecutionTime(task.executionUnits)
    val totalDuration = totalResourceAccessDuration + executionDuration
    if (realTimeDelays) {
      log.info(s"Simulating real time delay of $totalDuration")
      val delayFuture = delaySimulator simulate totalDuration
      Await.result(delayFuture, maxDelayTimeout)
    }
    val executionReport = generateTaskExecutionReport(task, totalDuration, resourceFetchResults)
    log.info(s"Signalling task accountant that task $task is complete")
    taskAccountant ! TaskExecutionComplete(task, executionReport)
  }

  private def generateTaskExecutionReport(task: Task, duration: FiniteDuration, fetchResults: Set[FetchResult]) = {
    log.info(s"Generating task execution report for task: $task")
    val numberOfLocalResources = fetchResults.count(_.wasLocal)
    val numberOfRemoteResources = fetchResults.count(!_.wasLocal)
    TaskExecutionReport(task, duration, numberOfLocalResources, numberOfRemoteResources)
  }

  private def requestNewTaskFromScheduler(): Unit = {
    log.info("Requesting new task from scheduler")
    taskScheduler ! RequestTaskForExecution
  }

  def receive: Receive = {
    case NewTaskForExecution(task)         => executeTaskThenRequestAnother(task)
    case StartRequestingTasksFromScheduler => requestNewTaskFromScheduler()
    case msg                               => log.warning(s"Unhandled message in receive: $msg")
  }
}

object ExecutionNode {
  private val maxDelayTimeout = 1.hour

  final case class NewTaskForExecution(task: Task)
  final case object StartRequestingTasksFromScheduler

  def props(
    akkaScheduler: actor.Scheduler,
    simulationConfiguration: SimulationConfiguration,
    executionUnitsPerMinute: Double,
    taskAccountant: ActorRef,
    taskScheduler: ActorRef
  ): Props =
    Props(
      new ExecutionNode(
        UUID.randomUUID(),
        new DelaySimulator(akkaScheduler),
        simulationConfiguration.realTimeDelaysEnabled,
        executionUnitsPerMinute,
        taskAccountant,
        taskScheduler
      )
    )
}

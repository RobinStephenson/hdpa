package tech.robins

import java.util.UUID
import java.util.concurrent.TimeUnit

import AbstractScheduler.{AcceptTask, RejectTask, RequestTaskForExecution}
import TaskAccountant.TaskExecutionComplete
import akka.actor.{Actor, ActorLogging, ActorRef}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._

abstract class ExecutionNode(
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

  protected val resources: mutable.Set[Resource] = mutable.Set()

  protected val maxDelayTimeout: FiniteDuration = 1.hour

  // TODO add a max number of resources configuration, after which resources will be removed (least used first?)
  protected def getOrFetchResource(resource: Resource): FetchResult = {
    if (resources contains resource) {
      log.info(s"Fetching local resource $resource")
      FetchResult localResourceFetch resource
    } else {
      log.info(s"Fetching remote resource $resource")
      resources add resource
      FetchResult remoteResourceFetch resource
    }
  }

  protected def calculateExecutionTime(executionUnits: Double): FiniteDuration =
    FiniteDuration((executionUnits / executionUnitsPerMinute).toLong, TimeUnit.MINUTES)

  protected def executeTask(task: Task): Unit = {
    log.info(s"Executing task $task")
    val resourceFetchResults = task.requiredResources.map(getOrFetchResource)
    val totalResourceAccessDuration = resourceFetchResults.map(_.fetchDuration).reduce(_ + _)
    val executionDuration = calculateExecutionTime(task.executionUnits)
    log.info(s"Task resource access duration: $totalResourceAccessDuration, execution duration: $executionDuration")
    log.info(s"Task exec units: ${task.executionUnits}, node exec units per min: $executionUnitsPerMinute")
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

  protected def currentNodeSchedulingData: NodeSchedulingData = NodeSchedulingData(resources.toSet)

  protected def requestNewTaskFromScheduler(): Unit = {
    log.info("Requesting new task from scheduler")
    val schedulingData = currentNodeSchedulingData
    taskScheduler ! RequestTaskForExecution(schedulingData)
  }

  protected def onExecuteTask(task: Task): Unit

  protected def shouldAcceptTask(task: Task): Boolean

  protected def onNewTaskOffer(task: Task): Unit =
    if (shouldAcceptTask(task))
      taskScheduler ! AcceptTask(task)
    else
      taskScheduler ! RejectTask(task)

  def receive: Receive = {
    case ExecuteTask(task)        => onExecuteTask(task)
    case OfferTask(task)          => onNewTaskOffer(task)
    case RequestWorkFromScheduler => requestNewTaskFromScheduler()
    case msg                      => log.warning(s"Unhandled message in receive: $msg")
  }
}

object ExecutionNode {
  final case class ExecuteTask(task: Task)
  final case class OfferTask(task: Task)
  final case object RequestWorkFromScheduler
}

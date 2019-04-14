package tech.robins.execution

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef}
import tech.robins.TaskAccountant.TaskExecutionComplete
import tech.robins._
import tech.robins.caching.Cache
import tech.robins.scheduling.AbstractScheduler.{AcceptTask, RejectTask, RequestTaskForExecution}

import scala.concurrent.Await
import scala.concurrent.duration._

abstract class AbstractExecutionNode(
  id: UUID,
  protected val delaySimulator: DelaySimulator,
  protected val realTimeDelays: Boolean,
  protected val executionUnitsPerMinute: Double,
  resourceCache: Cache[Resource],
  protected val taskAccountant: ActorRef,
  taskScheduler: ActorRef
) extends Actor
    with ActorLogging {
  import AbstractExecutionNode._
  import context.dispatcher

  protected val maxDelayTimeout: FiniteDuration = 1.hour

  protected def getOrFetchImaginaryResource(resourceId: String): FetchResult =
    resourceCache.getItems.find(_.id == resourceId) match {
      case Some(resource) =>
        log.info(s"Fetching local resource with id $resourceId")
        FetchResult localResourceFetch resource
      case None =>
        log.info(s"Fetching remote resource $resourceId")
        val resource = ImaginaryResource(resourceId)
        resourceCache add resource
        FetchResult remoteResourceFetch resource
    }

  protected def calculateExecutionTime(executionUnits: Double): FiniteDuration =
    FiniteDuration((executionUnits / executionUnitsPerMinute).toLong, TimeUnit.MINUTES)

  protected def executeTask(task: Task): Unit = {
    task match {
      case imaginaryTask: ImaginaryTask => executeImaginaryTask(imaginaryTask)
      case realTask: RealTask           => executeRealTask(realTask)
    }
  }

  protected def executeImaginaryTask(task: ImaginaryTask): Unit = {
    log.info(s"Executing task $task")
    val resourceFetchResults = task.requiredResourceIds.map(getOrFetchImaginaryResource)
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

  protected def executeRealTask(task: RealTask): Unit = {
    log.info(s"Executing real task $task")
    val localResourceIds = resourceCache.getItems.map(_.id).toSet
    val localResourceCount = task.requiredResourceIds.intersect(localResourceIds).size
    val remoteResourcesCount = task.requiredResourceIds.size - localResourceCount
    val resources = task.requiredResourceIds.map(resourceId => {
      resourceCache.find(_.id == resourceId) match {
        case Some(localResource) =>
          log.info(s"Got local resource with id $resourceId")
          localResource
        case None =>
          log.info(s"Fetching remote resource with id $resourceId")
          val resource = task.fetchRemoteResource(resourceId)
          resourceCache.add(resource)
          resource
      }
    })
    log.info(s"Starting work method of task $task")
    val workStartInstant = Instant.now()
    val result = task.work(resources)
    val workEndInstant = Instant.now()
    val duration = FiniteDuration(workEndInstant.toEpochMilli - workStartInstant.toEpochMilli, TimeUnit.MILLISECONDS)
    log.info(s"Real task execution result: $result")
    log.info(s"Execution duration: $duration")
    val executionReport = TaskExecutionReport(task, duration, localResourceCount, remoteResourcesCount)
    taskAccountant ! TaskExecutionComplete(task, executionReport)
  } // TODO send result somewhere? or make that tasks job

  private def generateTaskExecutionReport(task: Task, duration: FiniteDuration, fetchResults: Set[FetchResult]) = {
    log.info(s"Generating task execution report for task: $task")
    val numberOfLocalResources = fetchResults.count(_.wasLocal)
    val numberOfRemoteResources = fetchResults.count(!_.wasLocal)
    TaskExecutionReport(task, duration, numberOfLocalResources, numberOfRemoteResources)
  }

  protected def currentNodeSchedulingData: NodeSchedulingData =
    NodeSchedulingData(resourceCache.getItems.map(_.id).toSet)

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

object AbstractExecutionNode {
  final case class ExecuteTask(task: Task)
  final case class OfferTask(task: Task)
  final case object RequestWorkFromScheduler
}

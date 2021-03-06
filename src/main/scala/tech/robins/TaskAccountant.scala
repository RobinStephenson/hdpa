package tech.robins

import tech.robins.workgeneration.AbstractWorkloadGenerator.EndOfWorkGeneration
import SimulationController.SimulationComplete
import TaskAccountant.{TaskExecutionComplete, TaskSentToScheduler}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.collection.mutable

class TaskAccountant(simulationController: ActorRef) extends Actor with ActorLogging {
  private var workGenerationReport: Option[WorkGenerationReport] = None
  private var workGenerationComplete: Boolean = false

  private val pendingTasks = mutable.Set.empty[Task]
  private val completedTasks = mutable.Map.empty[Task, TaskExecutionReport]

  private def updateCompletedTasks(completedTask: Task, report: TaskExecutionReport): Unit =
    completedTasks get completedTask match {
      case Some(previousReport) =>
        log.error(
          s"Task $completedTask was previously registered as complete with report $previousReport. " +
            s"A second completion message was sent with report $report"
        )
      case None =>
        completedTasks += (completedTask -> report)
        log.info(s"Added task $completedTask to list of completed tasks. ${completedTasks.size} tasks complete.")
    }

  private def addToPendingTasks(scheduledTask: Task): Unit = {
    if (pendingTasks contains scheduledTask) {
      log.warning(s"Task $scheduledTask is already pending")
    } else {
      pendingTasks += scheduledTask
      log.info(s"Added task $scheduledTask to pending tasks. ${pendingTasks.size} tasks pending.")
    }
  }

  private def removeFromPendingTasks(completedTask: Task): Unit = {
    val wasPresent = pendingTasks remove completedTask
    if (wasPresent)
      log.info(s"Completed task $completedTask removed from pending tasks")
    else
      log.warning(s"Completed task $completedTask was not in the pending tasks queue")
    // TODO investigate errors. probably a race condition where the completion message arrives before the task accountant is told its pending
    //Completed task Task(0e1e85da-aaa8-495b-a9e2-aba9f2bbef6d,Set(Resource(40b4f84a-eb34-410d-825c-48c84f0cc63d)),243.54078699816682) was not in the pending tasks queue
  }

  private def notifySimulationControllerIfAllTasksCompleted(): Unit =
    if (workGenerationComplete && pendingTasks.isEmpty) {
      workGenerationReport match {
        case None => throw new IllegalStateException("WorkGenerationReport must be defined")
        case Some(workGenReport) =>
          if (completedTasks.size == workGenReport.totalTasks) {
            simulationController ! SimulationComplete(completedTasks.toMap, workGenReport)
          }
      }
    }

  private def taskExecutionCompleted(task: Task, report: TaskExecutionReport): Unit = {
    updateCompletedTasks(task, report)
    removeFromPendingTasks(task)
    notifySimulationControllerIfAllTasksCompleted()
  }

  private def noMoreWorkBeingGenerated(report: WorkGenerationReport): Unit = {
    log.info("Workload generation completed! Waiting for existing tasks to finish.")
    workGenerationReport = Some(report)
    workGenerationComplete = true
  }

  def receive: Receive = {
    case EndOfWorkGeneration(report)                  => noMoreWorkBeingGenerated(report)
    case TaskSentToScheduler(task)                          => addToPendingTasks(task)
    case TaskExecutionComplete(task, executionReport) => taskExecutionCompleted(task, executionReport)
    case msg                                          => log.warning(s"Unhandled message in receive: $msg")
  }
}

object TaskAccountant {
  final case class TaskSentToScheduler(task: Task)
  final case class TaskExecutionComplete(task: Task, executionReport: TaskExecutionReport)

  def props(simulationController: ActorRef): Props = Props(new TaskAccountant(simulationController))
}

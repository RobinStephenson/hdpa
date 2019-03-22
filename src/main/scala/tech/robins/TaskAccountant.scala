package tech.robins

import AbstractWorkloadGenerator.EndOfWorkGeneration
import SimulationController.SimulationComplete
import TaskAccountant.{TaskExecutionComplete, TaskScheduled}
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
      log.warning(s"Scheduled task $scheduledTask is already pending")
    } else {
      log.info(s"Adding scheduled task $scheduledTask to pending tasks. ${pendingTasks.size} tasks pending.")
      pendingTasks += scheduledTask
    }
  }

  private def removeFromPendingTasks(completedTask: Task): Unit = {
    val wasPresent = pendingTasks remove completedTask
    if (wasPresent)
      log.info(s"Competed task $completedTask removed from pending tasks")
    else
      log.error(s"Completed task $completedTask was not in the pending tasks queue")
  }

  private def notifySimulationControllerIfAllTasksCompleted(): Unit =
    if (workGenerationComplete && pendingTasks.isEmpty) {
      log.info("Work generation and all tasks completed. Informing simulation controller that simulation is complete.")
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
    case TaskScheduled(task)                          => addToPendingTasks(task)
    case TaskExecutionComplete(task, executionReport) => taskExecutionCompleted(task, executionReport)
    case msg                                          => log.warning(s"Unhandled message in receive: $msg")
  }
}

object TaskAccountant {
  final case class TaskScheduled(task: Task)
  final case class TaskExecutionComplete(task: Task, executionReport: TaskExecutionReport)

  def props(simulationController: ActorRef): Props = Props(new TaskAccountant(simulationController))
}

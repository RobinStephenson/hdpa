package tech.robins

import akka.actor.{Actor, ActorLogging, ActorRef}
import tech.robins.AbstractScheduler.{NewTaskForScheduling, RequestTaskForExecution}

import scala.collection.mutable

trait AbstractScheduler extends Actor with ActorLogging {
  protected val taskQueue: mutable.Queue[Task] = mutable.Queue.empty[Task]
  protected val waitingWorkers: mutable.Queue[ActorRef] = mutable.Queue.empty[ActorRef]

  protected def newTaskArrivedForWaitingWorkers(): Unit

  protected def enqueueNewTask(task: Task): Unit = {
    val taskQueueWasEmpty = taskQueue.isEmpty
    taskQueue enqueue task
    log.info(s"Queued new task $task. ${taskQueue.length} tasks now waiting.")
    if (taskQueueWasEmpty && waitingWorkers.nonEmpty) newTaskArrivedForWaitingWorkers()
  }

  protected def handleNewTaskRequest(requester: ActorRef): Unit

  def receive: Receive = {
    case NewTaskForScheduling(task) => enqueueNewTask(task)
    case RequestTaskForExecution =>
      val requester = sender()
      log.info(s"Task requested from scheduler by $requester")
      handleNewTaskRequest(requester)
    case msg => log.warning(s"Unhandled message in receive: $msg")
  }
}

object AbstractScheduler {
  final case class NewTaskForScheduling(task: Task)

  final case object RequestTaskForExecution
}

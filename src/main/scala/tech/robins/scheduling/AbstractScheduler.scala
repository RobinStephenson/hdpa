package tech.robins.scheduling

import akka.actor.{Actor, ActorLogging, ActorRef}
import tech.robins.{NodeSchedulingData, Task}

trait AbstractScheduler extends Actor with ActorLogging {
  import AbstractScheduler._

  protected def onNewTaskForScheduling(task: Task): Unit

  protected def handleNewTaskRequest(requester: ActorRef, schedulingData: NodeSchedulingData): Unit

  def receive: Receive = {
    case NewTaskForScheduling(task) => onNewTaskForScheduling(task)
    case RequestTaskForExecution(nodeSchedulingData) =>
      val requester = sender()
      log.info(s"Task requested from scheduler by $requester")
      handleNewTaskRequest(requester, nodeSchedulingData)
    case msg => log.warning(s"Unhandled message in receive: $msg")
  }
}

object AbstractScheduler {
  final case class NewTaskForScheduling(task: Task)

  final case class AcceptTask(task: Task)

  final case class RejectTask(task: Task)

  final case class RequestTaskForExecution(nodeSchedulingData: NodeSchedulingData)
}

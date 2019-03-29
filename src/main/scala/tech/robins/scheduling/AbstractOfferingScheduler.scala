package tech.robins.scheduling
import akka.actor.ActorRef
import tech.robins.Task
import tech.robins.scheduling.AbstractScheduler.{AcceptTask, RejectTask}

trait AbstractOfferingScheduler extends AbstractScheduler {
  protected def onAcceptTask(task: Task, worker: ActorRef)

  protected def onRejectTask(task: Task, worker: ActorRef)

  private val offeringMessageHandlers: Receive = {
    case AcceptTask(task) => onAcceptTask(task, sender())
    case RejectTask(task) => onRejectTask(task, sender())
  }

  override def receive: Receive = offeringMessageHandlers orElse super.receive
}

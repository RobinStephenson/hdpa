package tech.robins.scheduling

import akka.actor.{ActorRef, Props}
import tech.robins.execution.AbstractExecutionNode.OfferTask
import tech.robins._

class FifoOfferingScheduler
    extends AbstractOfferingScheduler
    with HasTaskQueue
    with HasWaitingWorkers
    with SimpleAcceptHandler {
  protected def onNewTaskForScheduling(task: Task): Unit = {
    taskQueue.append(task)
    log.info(s"Task added to task queue: $task")
    log.info(s"Task queue is now ${taskQueue.length} long.")
    waitingWorkers.foreach(_ ! OfferTask(task))
    log.info(s"Task offered to ${waitingWorkers.length} waiting workers")
  }

  protected def onRejectTask(task: Task, worker: ActorRef): Unit = {
    log.info(s"Task $task rejected by worker $worker. Looking for another task to offer.")
    val otherTasks = taskQueue - task
    offerFirstTaskFromListIfAvailable(otherTasks, worker)
  }

  protected def offerFirstTaskFromListIfAvailable(list: Iterable[Task], worker: ActorRef): Unit =
    list.headOption match {
      case Some(task) => worker ! OfferTask(task)
      case None       => addToWaitingWorkers(worker)
    }

  protected def handleNewTaskRequest(requester: ActorRef, schedulingData: NodeSchedulingData): Unit =
    offerFirstTaskFromListIfAvailable(taskQueue, requester)
}

object FifoOfferingScheduler {
  val props: Props = Props(new FifoOfferingScheduler)
}

package tech.robins
import akka.actor.{ActorRef, Props}
import tech.robins.ExecutionNode.{ExecuteTask, OfferTask, RequestWorkFromScheduler}

// TODO package schedulers, execution node stuff etc after commit
class FifoOfferingScheduler extends AbstractScheduler with HasTaskQueue with HasWaitingWorkers {
  protected def onNewTaskForScheduling(task: Task): Unit = {
    taskQueue.append(task)
    log.info(s"Task added to task queue: $task")
    log.info(s"Task queue is now ${taskQueue.length} long.")
    waitingWorkers.foreach(_ ! OfferTask(task))
    log.info(s"Task offered to ${waitingWorkers.length} waiting workers")
  }

  protected def onAcceptTask(task: Task, worker: ActorRef): Unit = {
    val taskStillAvailable = taskQueue contains task
    if (taskStillAvailable) {
      log.info(s"Task $task accepted by $worker and is still available. Assigned to worker.")
      taskQueue -= task
      worker ! ExecuteTask(task)
    } else {
      log.info(s"Task $task accepted by $worker but is no longer available. Telling worker to re request work")
      worker ! RequestWorkFromScheduler
    }
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

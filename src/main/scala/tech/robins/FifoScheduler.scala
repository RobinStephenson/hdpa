package tech.robins

import ExecutionNode.NewTaskForExecution
import akka.actor.{ActorLogging, ActorRef, Props}

/**
  * First in first out scheduler. Tasks as well as Requests for new tasks are served in FIFO.
  */
class FifoScheduler extends AbstractScheduler {
  protected def newTaskArrivedForWaitingWorkers(): Unit = {
    log.info(s"New task arrived for waiting workers. Task queue: $taskQueue. Waiting workers: $waitingWorkers")
    val nextWorker = waitingWorkers.dequeue()
    val task = taskQueue.dequeue()
    log.info(s"Waiting worker $nextWorker assigned new task $task and removed from waiting workers.")
    log.info(s"Remaining waiting workers: $waitingWorkers")
    nextWorker ! NewTaskForExecution(task)
  }

  protected def handleNewTaskRequest(requester: ActorRef): Unit = {
    log.info(s"New task requested from scheduler by $requester")
    if (taskQueue.nonEmpty) {
      val task = taskQueue.dequeue()
      log.info(s"Task: $task is assigned to requester $requester")
      requester ! NewTaskForExecution(task)
    } else {
      log.info(s"Task requested by $requester, but no tasks currently in queue. Added to waiting workers.")
      waitingWorkers enqueue requester
      log.info(s"There are now ${waitingWorkers.length} workers waiting.")
    }
  }
}

object FifoScheduler {
  val props = Props(new FifoScheduler)
}

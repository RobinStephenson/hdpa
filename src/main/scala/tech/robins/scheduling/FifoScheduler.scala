package tech.robins.scheduling

import akka.actor.{ActorRef, Props}
import tech.robins.execution.AbstractExecutionNode.ExecuteTask
import tech.robins._

/**
  * First in first out scheduler. Tasks as well as Requests for new tasks are served in FIFO.
  */
class FifoScheduler extends AbstractScheduler with HasTaskQueue with HasWaitingWorkers {
  protected def onNewTaskForScheduling(task: Task): Unit = {
    val taskQueueWasEmpty = taskQueue.isEmpty
    taskQueue append task
    log.info(s"Queued new task $task. ${taskQueue.length} tasks now waiting.")
    if (taskQueueWasEmpty && waitingWorkers.nonEmpty) newTaskArrivedForWaitingWorkers(task)
  }

  protected def newTaskArrivedForWaitingWorkers(task: Task): Unit = {
    log.info(s"New task arrived for waiting workers. Task queue: $taskQueue. Waiting workers: $waitingWorkers")
    val nextWorker = waitingWorkers.remove(0)
    val task = dequeueFirstTask()
    log.info(s"Waiting worker $nextWorker assigned new task $task and removed from waiting workers.")
    log.info(s"Remaining waiting workers: $waitingWorkers")
    nextWorker ! ExecuteTask(task)
  }

  protected def handleNewTaskRequest(requester: ActorRef, nodeSchedulingData: NodeSchedulingData): Unit = {
    log.info(s"New task requested from scheduler by $requester")
    if (taskQueue.nonEmpty) {
      val task = dequeueFirstTask()
      log.info(s"Task: $task is assigned to requester $requester")
      requester ! ExecuteTask(task)
    } else {
      log.info(s"Task requested by $requester, but no tasks currently in queue. Added to waiting workers.")
      addToWaitingWorkers(requester)
    }
  }
}

object FifoScheduler {
  val props = Props(new FifoScheduler)
}

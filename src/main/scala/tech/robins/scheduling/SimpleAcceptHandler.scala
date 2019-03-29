package tech.robins.scheduling
import akka.actor.ActorRef
import tech.robins.Task
import tech.robins.execution.AbstractExecutionNode.{ExecuteTask, RequestWorkFromScheduler}

trait SimpleAcceptHandler extends AbstractOfferingScheduler { this: HasTaskQueue =>
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
}

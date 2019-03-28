package tech.robins.execution
import tech.robins.Task

trait SimpleOnExecute { this: ExecutionNode =>
  protected def onExecuteTask(task: Task): Unit = {
    executeTask(task)
    requestNewTaskFromScheduler()
  }
}

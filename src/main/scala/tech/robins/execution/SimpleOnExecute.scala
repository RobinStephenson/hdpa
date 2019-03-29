package tech.robins.execution
import tech.robins.Task

trait SimpleOnExecute { this: AbstractExecutionNode =>
  protected def onExecuteTask(task: Task): Unit = {
    executeTask(task)
    requestNewTaskFromScheduler()
  }
}

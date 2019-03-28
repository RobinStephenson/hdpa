package tech.robins

trait SimpleOnExecute { this: ExecutionNode =>
  protected def onExecuteTask(task: Task): Unit = {
    executeTask(task)
    requestNewTaskFromScheduler()
  }
}

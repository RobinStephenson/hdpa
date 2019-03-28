package tech.robins.scheduling
import tech.robins.Task

import scala.collection.mutable

trait HasTaskQueue {
  protected val taskQueue: mutable.ArrayBuffer[Task] = mutable.ArrayBuffer.empty

  protected def dequeueFirstTask(): Task = taskQueue.remove(0)
}

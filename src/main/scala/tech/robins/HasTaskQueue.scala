package tech.robins
import scala.collection.mutable

trait HasTaskQueue {
  protected val taskQueue: mutable.ArrayBuffer[Task] = mutable.ArrayBuffer.empty

  protected def dequeueFirstTask(): Task = taskQueue.remove(0)
}

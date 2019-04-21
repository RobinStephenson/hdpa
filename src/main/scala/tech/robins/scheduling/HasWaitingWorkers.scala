package tech.robins.scheduling
import akka.actor.{Actor, ActorLogging, ActorRef}

import scala.collection.mutable

trait HasWaitingWorkers extends Actor with ActorLogging { this: AbstractScheduler =>
  protected val waitingWorkers: mutable.ArrayBuffer[ActorRef] = mutable.ArrayBuffer.empty

  protected def addToWaitingWorkers(worker: ActorRef): Unit = if (waitingWorkers.contains(worker)) {
    log.warning(s"Worker passed to addToWaitingWorkers despite being added already: $worker")
  } else {
    waitingWorkers append worker
    log.info(s"There are now ${waitingWorkers.length} workers waiting for tasks.")
  }
}

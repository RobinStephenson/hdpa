package tech.robins
import akka.actor.{Actor, ActorLogging, ActorRef}

import scala.collection.mutable

trait HasWaitingWorkers extends Actor with ActorLogging {
  protected val waitingWorkers: mutable.ArrayBuffer[ActorRef] = mutable.ArrayBuffer.empty

  protected def addToWaitingWorkers(worker: ActorRef): Unit = {
    waitingWorkers append worker
    log.info(s"There are now ${waitingWorkers.length} workers waiting.")
  }
}

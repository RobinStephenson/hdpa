package tech.robins.workgeneration

import akka.actor.{Actor, ActorLogging, ActorRef}
import tech.robins.scheduling.AbstractScheduler.NewTaskForScheduling
import tech.robins.{Task, WorkGenerationReport}

import scala.collection.mutable

trait AbstractWorkloadGenerator extends Actor with ActorLogging {
  import AbstractWorkloadGenerator._

  private val endOfWorkSubscribers: mutable.Set[ActorRef] = mutable.Set.empty

  private def startGeneratingWorkThenSendFinishedMessages(scheduler: ActorRef): Unit = {
    log.info("Starting work generation")
    def sendTask(task: Task): Unit = scheduler ! NewTaskForScheduling(task)
    val workGenerationReport = generateWork(sendTask)
    log.info(s"Work generation complete. Sending work generation reports to subscribers: $endOfWorkSubscribers")
    endOfWorkSubscribers.foreach(_ ! EndOfWorkGeneration(workGenerationReport))
  }

  protected def subscribeToEndOfWork(subscriber: ActorRef): Unit = {
    log.info(s"Subscribing actor $subscriber to EndOfWorkGeneration updates")
    if (endOfWorkSubscribers contains subscriber)
      log.warning(s"Subscriber $subscriber already subscribed to end of work updates")
    else
      endOfWorkSubscribers add subscriber
  }

  protected def generateWork(sendTask: Task => Unit): WorkGenerationReport

  def receive: Receive = {
    case SubscribeToEndOfWorkGeneration(subscriber) => subscribeToEndOfWork(subscriber)
    case StartGeneratingWork(scheduler)             => startGeneratingWorkThenSendFinishedMessages(scheduler)
    case msg                                        => log.warning(s"Unhandled message in receive: $msg")
  }
}

object AbstractWorkloadGenerator {
  final case class StartGeneratingWork(scheduler: ActorRef)
  final case class SubscribeToEndOfWorkGeneration(subscriber: ActorRef)
  final case class EndOfWorkGeneration(workGenerationReport: WorkGenerationReport)
}

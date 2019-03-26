package tech.robins

import akka.actor.{Actor, ActorLogging, ActorRef}

import scala.collection.mutable

trait AbstractWorkloadGenerator extends Actor with ActorLogging { // TODO different types of work gen
  import AbstractWorkloadGenerator._

  private val endOfWorkSubscribers: mutable.Set[ActorRef] = mutable.Set.empty

  protected def startGeneratingWorkThenSendFinishedMessages(scheduler: ActorRef): Unit = {
    log.info("Starting work generation")
    val workGenerationReport = generateWork(scheduler)
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

  protected def generateWork(scheduler: ActorRef): WorkGenerationReport

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

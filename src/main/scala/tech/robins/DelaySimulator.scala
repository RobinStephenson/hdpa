package tech.robins

import akka.actor

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

class DelaySimulator(scheduler: actor.Scheduler) {
  def simulate(duration: FiniteDuration)(implicit executionContext: ExecutionContext): Future[FiniteDuration] =
    akka.pattern.after(duration, scheduler)(Future(duration))
}

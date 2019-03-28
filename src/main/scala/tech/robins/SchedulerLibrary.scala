package tech.robins
import akka.actor.Props

object SchedulerLibrary {
  val propsByName: Map[String, Props] = Map(
    "FifoScheduler" -> FifoScheduler.props,
    "FifoOfferingScheduler" -> FifoOfferingScheduler.props
  )
}

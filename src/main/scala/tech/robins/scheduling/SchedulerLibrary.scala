package tech.robins.scheduling
import akka.actor.Props

object SchedulerLibrary {
  val propsByName: Map[String, Props] = Map(
    "FifoScheduler" -> FifoScheduler.props,
    "FifoOfferingScheduler" -> FifoOfferingScheduler.props,
    "GreedyMaxLocalityScheduler" -> GreedyMaxLocalityScheduler.props
  )
}

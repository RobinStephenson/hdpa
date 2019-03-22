package tech.robins
import akka.actor.Props

object WorkloadGeneratorLibrary {
  val propsByName: Map[String, WorkGenerationConfiguration => Props] = Map(
    "FiftyNewResourceTasksGenerator" -> FiftyNewResourceTasksGenerator.props
  )
}

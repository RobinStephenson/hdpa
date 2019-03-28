package tech.robins.workgeneration
import akka.actor.Props
import tech.robins.WorkGenerationConfiguration

object WorkloadGeneratorLibrary {
  val propsByName: Map[String, WorkGenerationConfiguration => Props] = Map(
    "FiftyNewResourceTasksGenerator" -> FiftyNewResourceTasksGenerator.props,
    "MixedNewAndOldResourceWorkloadGenerator" -> MixedNewAndOldResourceWorkloadGenerator.props
  )
}

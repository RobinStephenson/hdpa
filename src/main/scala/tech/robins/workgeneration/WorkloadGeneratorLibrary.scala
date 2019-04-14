package tech.robins.workgeneration
import akka.actor.Props
import akka.stream.ActorMaterializer
import tech.robins.WorkGenerationConfiguration

object WorkloadGeneratorLibrary {
  def propsByName(name: String, materializer: ActorMaterializer): WorkGenerationConfiguration => Props =
    name match {
      case "FiftyNewResourceTasksGenerator"          => FiftyNewResourceTasksGenerator.props
      case "MixedNewAndOldResourceWorkloadGenerator" => MixedNewAndOldResourceWorkloadGenerator.props
      case "GitHubSearchingTaskGenerator"            => GitHubSearchingTaskGenerator.props(_)(materializer)
    }
}

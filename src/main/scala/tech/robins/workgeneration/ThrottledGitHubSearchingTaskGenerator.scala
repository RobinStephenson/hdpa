package tech.robins.workgeneration

import akka.NotUsed
import akka.actor.Props
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.Flow
import com.typesafe.config.ConfigFactory
import tech.robins.GitHubContentSearch.RepoFullName
import tech.robins.{RealTask, WorkGenerationConfiguration}

import scala.concurrent.duration._

class ThrottledGitHubSearchingTaskGenerator(
  maxNumberOfRepos: Int,
  searchTerm: String,
  fileExtension: String,
  workGenerationTimeout: Duration = 1.hour
)(implicit materializer: ActorMaterializer)
    extends GitHubSearchingTaskGenerator(maxNumberOfRepos, searchTerm, fileExtension, workGenerationTimeout) {
  override protected val createTasksFlow: Flow[RepoFullName, RealTask, NotUsed] =
    super.createTasksFlow.throttle(16, 270.seconds, 8, ThrottleMode.Shaping)
}

object ThrottledGitHubSearchingTaskGenerator {
  def props(workGenConfig: WorkGenerationConfiguration)(implicit materializer: ActorMaterializer): Props = {
    val gitHubSearchConfig = ConfigFactory.load().getConfig("simulation.workloadGeneration.gitHubSearch")
    val maxNumberOfReposToGenerateTasksFor = gitHubSearchConfig.getInt("maxNumberOfReposToGenerateTasksFor")
    val searchTerm = gitHubSearchConfig.getString("searchTerm")
    val fileExtension = gitHubSearchConfig.getString("fileExtension")
    Props(new ThrottledGitHubSearchingTaskGenerator(maxNumberOfReposToGenerateTasksFor, searchTerm, fileExtension))
  }
}

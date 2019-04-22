package tech.robins.workgeneration

import akka.NotUsed
import akka.actor.Props
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.typesafe.config.ConfigFactory
import tech.robins.GitHubContentSearch.RepoFullName
import tech.robins._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class GitHubSearchingTaskGenerator(maxNumberOfRepos: Int, searchTerm: String, fileExtension: String, workGenerationTimeout: Duration = 1.hour)(implicit materializer: ActorMaterializer)
    extends AbstractWorkloadGenerator {
  private val gmfGraphRepoSource: Source[RepoFullName, NotUsed] =
    Source
      .fromGraph(GitHubContentSearch(searchTerm, fileExtension))
      .throttle(100, 2.minutes) // To prevent hitting the rate limit
      .take(maxNumberOfRepos)

  protected def createTasksFlow: Flow[RepoFullName, RealTask, NotUsed] = Flow[RepoFullName].mapConcat[RealTask](
    repoFullName =>
      List(
        GitHubCountCommitAuthorsTask(repoFullName),
        GitHubCountCommitsByAuthorTask(repoFullName),
        GitHubMeanCommitMessageLengthTask(repoFullName)
    )
  )

  private def schedulerSink(sendTask: Task => Unit): Sink[RealTask, Future[WorkGenerationReport]] =
    Sink.fold[WorkGenerationReport, RealTask](WorkGenerationReport(0))((workGenReport, task) => {
      sendTask(task)
      WorkGenerationReport(workGenReport.totalTasks + 1)
    })

  protected def generateWork(sendTask: Task => Unit): WorkGenerationReport = {
    val eventualReport: Future[WorkGenerationReport] = gmfGraphRepoSource
      .via(createTasksFlow)
      .toMat(schedulerSink(sendTask))(Keep.right)
      .run()
    Await.result(eventualReport, workGenerationTimeout)
  }
}

object GitHubSearchingTaskGenerator {
  def props(workGenConfig: WorkGenerationConfiguration)(implicit materializer: ActorMaterializer): Props = {
    val gitHubSearchConfig = ConfigFactory.load().getConfig("simulation.workloadGeneration.gitHubSearch")
    val maxNumberOfReposToGenerateTasksFor = gitHubSearchConfig.getInt("maxNumberOfReposToGenerateTasksFor")
    val searchTerm = gitHubSearchConfig.getString("searchTerm")
    val fileExtension = gitHubSearchConfig.getString("fileExtension")
    Props(new GitHubSearchingTaskGenerator(maxNumberOfReposToGenerateTasksFor, searchTerm, fileExtension))
  }
}

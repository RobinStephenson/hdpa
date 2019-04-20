package tech.robins.workgeneration

import akka.NotUsed
import akka.actor.Props
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import tech.robins.GitHubContentSearch.RepoFullName
import tech.robins._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

// Wrapper for Kohsuke GHRepo class
class GitHubSearchingTaskGenerator(workGenerationTimeout: Duration = 1.hour)(implicit materializer: ActorMaterializer)
    extends AbstractWorkloadGenerator {
  private val gmfGraphRepoSource: Source[RepoFullName, NotUsed] =
    Source.fromGraph(GitHubContentSearch("figure", "gmfgraph")).throttle(100, 2.minutes)

  private val createTasksFlow: Flow[RepoFullName, RealTask, NotUsed] = Flow[RepoFullName].mapConcat[RealTask](
    repoFullName =>
      List(
        GitHubCountCommitAuthorsTask(repoFullName),
        GitHubCountCommitsByAuthor(repoFullName)
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
  def props(workGenConfig: WorkGenerationConfiguration)(implicit materializer: ActorMaterializer): Props =
    Props(new GitHubSearchingTaskGenerator)
}

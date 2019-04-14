package tech.robins

import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler, StageLogging}
import org.kohsuke.github.{GHContent, GitHub, PagedIterator}
import tech.robins.GitHubContentSearch.RepoFullName

class GitHubContentSearch(searchTerm: String, fileExtension: String, gh: GitHub)
    extends GraphStage[SourceShape[RepoFullName]] {
  require(!gh.isAnonymous, "Must be authorized with GitHub")

  val out: Outlet[RepoFullName] = Outlet("GitHubFileSearchSource")

  val shape: SourceShape[RepoFullName] = SourceShape(out)

  def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with StageLogging {
    lazy val resultsIterator: PagedIterator[GHContent] = gh
      .searchContent()
      .extension(fileExtension)
      .q(searchTerm)
      .list()
      .iterator()

    setHandler(
      out,
      new OutHandler {
        def onPull(): Unit = {
          val result = resultsIterator.next().getOwner.getFullName
          push(out, result)
          if (!resultsIterator.hasNext) {
            log.info(s"No more repositories, completing stream")
            complete(out)
          }
        }
      }
    )
  }
}

object GitHubContentSearch {
  type RepoFullName = String

  def apply(searchTerm: String, fileExtension: String): GitHubContentSearch =
    new GitHubContentSearch(searchTerm, fileExtension, GitHubApi.authenticatedApi)
}

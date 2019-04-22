package tech.robins
import java.io.File
import java.nio.file.Files
import java.util.UUID

import org.eclipse.jgit.api.Git
import tech.robins.RealTask.WorkResult

import scala.collection.JavaConverters._

trait GitHubAnalysisTask extends RealTask {
  val repoName: String

  val requiredResourceIds: Set[String] = Set(repoName)

  def work(resources: Set[Resource]): WorkResult = {
    resources.find(_.id == repoName) match {
      case Some(repo: GitHubRepo) => doAnalysis(repo)
      case _                      => throw new IllegalStateException
    }
  }

  protected def doAnalysis(repo: GitHubRepo): WorkResult

  def fetchRemoteResource(resourceId: String): Resource = {
    require(resourceId == repoName, "GitHubAnalysisTasks can only depend on one repo")
    val repoDir = new File(Files.createTempDirectory(repoName.replace('/', '-')).toUri)
    repoDir.deleteOnExit()
    val localRepo = Git
      .cloneRepository()
      .setDirectory(repoDir)
      .setURI(s"https://github.com/$repoName")
      .call()
    GitHubRepo(repoName, localRepo)
  }
}

case class GitHubCountCommitAuthorsTask(id: UUID, repoName: String) extends GitHubAnalysisTask {
  protected def doAnalysis(repo: GitHubRepo): WorkResult = {
    val numberOfCommitAuthors: Int = repo.localClone
      .log()
      .call()
      .asScala
      .map(_.getAuthorIdent.getEmailAddress)
      .toSet
      .size
    WorkResult(numberOfCommitAuthors)
  }
}

object GitHubCountCommitAuthorsTask {
  def apply(repoName: String): GitHubCountCommitAuthorsTask =
    GitHubCountCommitAuthorsTask(UUID.randomUUID(), repoName)
}

case class GitHubCountCommitsByAuthorTask(id: UUID, repoName: String) extends GitHubAnalysisTask {
  protected def doAnalysis(repo: GitHubRepo): WorkResult = {
    val commitsPerAuthor: Map[String, Int] = repo.localClone
      .log()
      .call()
      .asScala
      .map(_.getAuthorIdent.getEmailAddress)
      .foldLeft(Map.empty[String, Int]) { (accumulatedCommitCounts, authorEmail) =>
        val currentAuthorCommitCount = accumulatedCommitCounts.getOrElse(authorEmail, 0)
        accumulatedCommitCounts.updated(authorEmail, currentAuthorCommitCount + 1)
      }
    WorkResult(commitsPerAuthor)
  }
}

object GitHubCountCommitsByAuthorTask {
  def apply(repoName: String): GitHubCountCommitsByAuthorTask =
    GitHubCountCommitsByAuthorTask(UUID.randomUUID(), repoName)
}

case class GitHubMeanCommitMessageLengthTask(id: UUID, repoName: String) extends GitHubAnalysisTask {
  override protected def doAnalysis(repo: GitHubRepo): WorkResult = {
    val messageLengths = repo.localClone
      .log()
      .call()
      .asScala
      .map(_.getFullMessage.length)
    val numberOfCommits = messageLengths.size
    val meanLength = messageLengths.sum.toDouble / numberOfCommits
    WorkResult(meanLength)
  }
}

object GitHubMeanCommitMessageLengthTask {
  def apply(repoName: String): GitHubMeanCommitMessageLengthTask =
    GitHubMeanCommitMessageLengthTask(UUID.randomUUID(), repoName)
}
package tech.robins

import java.util.UUID

import org.eclipse.jgit.api.Git
import tech.robins.RealTask.WorkResult
import tech.robins.caching.{HasCacheRemovalHook, UnitCacheRemovalHook}

import scala.concurrent.duration._

trait Resource extends HasCacheRemovalHook {
  val id: String
}

case class ImaginaryResource(id: String) extends Resource with UnitCacheRemovalHook

case class GitHubRepo(fullName: String, localClone: Git) extends Resource {
  val id: String = fullName

  def onRemovedFromCache(): Unit = localClone.getRepository.getWorkTree.delete()
}

sealed trait Task extends UnitCacheRemovalHook {
  val id: UUID
  val requiredResourceIds: Set[String]
}

case class ImaginaryTask(id: UUID, requiredResourceIds: Set[String], executionUnits: Double) extends Task

object ImaginaryTask {
  def apply(requiredResourceIds: Set[String], executionUnits: Double): ImaginaryTask =
    ImaginaryTask(UUID.randomUUID(), requiredResourceIds, executionUnits)
}

trait RealTask extends Task {
  val requiredResourceIds: Set[String]

  def work(resources: Set[Resource]): WorkResult

  def fetchRemoteResource(resourceId: String): Resource
}

object RealTask {
  final case class WorkResult(payload: Any)
}

case class TaskExecutionReport(task: Task, duration: FiniteDuration, localResources: Int, remoteResources: Int)

case class WorkGenerationReport(totalTasks: Int)

case class NodeSchedulingData(presentResourceIds: Set[String])

case class FetchResult(resource: Resource, fetchDuration: FiniteDuration, wasLocal: Boolean)

object FetchResult {
  // TODO move these to config

  /**
    * How long it takes to access input data which is already available locally on a node
    */
  val localAccessTime: FiniteDuration = 1.second

  /**
    * How long it takes to access input data which is not present on an execution node
    */
  val remoteAccessTime: FiniteDuration = 30.seconds

  def localResourceFetch(resource: Resource): FetchResult = FetchResult(resource, localAccessTime, wasLocal = true)

  def remoteResourceFetch(resource: Resource): FetchResult = FetchResult(resource, remoteAccessTime, wasLocal = false)
}

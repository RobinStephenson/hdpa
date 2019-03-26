package tech.robins

import java.util.UUID

import scala.concurrent.duration._

case class Resource(id: UUID)

case class Task(id: UUID, requiredResources: Set[Resource], executionUnits: Double)

object Task {
  def apply(requiredResources: Set[Resource], executionUnits: Double): Task =
    Task(UUID.randomUUID(), requiredResources, executionUnits)
}

case class TaskExecutionReport(task: Task, duration: FiniteDuration, localResources: Int, remoteResources: Int)

case class WorkGenerationReport(totalTasks: Int) // TODO resources -> number of times appearing in a task

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

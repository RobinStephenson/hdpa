package tech.robins

import scala.concurrent.duration.FiniteDuration

case class SimulationReport(
  simulationConfiguration: SimulationConfiguration,
  workGenerationReport: WorkGenerationReport,
  taskExecutionReports: Map[Task, TaskExecutionReport]
) {

  private val taskExecutionDurations = taskExecutionReports.values.map(_.duration)

  val totalTaskExecutionUnits: Double = taskExecutionReports.values.map(_.task.executionUnits).sum

  val totalDuration: FiniteDuration = taskExecutionDurations.reduce(_ + _)
  val maxDuration: FiniteDuration = taskExecutionDurations.max
  val minDuration: FiniteDuration = taskExecutionDurations.min
  val meanDuration: FiniteDuration = totalDuration / taskExecutionReports.size

  val totalLocalFetches: Int = taskExecutionReports.values.map(_.localResources).sum
  val totalRemoteFetches: Int = taskExecutionReports.values.map(_.remoteResources).sum
  val simulationLocalityRate: Double = totalLocalFetches / totalRemoteFetches
}

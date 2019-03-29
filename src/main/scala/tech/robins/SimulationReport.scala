package tech.robins

import scala.concurrent.duration.FiniteDuration

case class SimulationReport(
  simulationConfiguration: SimulationConfiguration,
  workGenerationReport: WorkGenerationReport,
  taskExecutionReports: Map[Task, TaskExecutionReport],
  realTimeSimulationDuration: FiniteDuration
) {

  private val taskExecutionDurations = taskExecutionReports.values.map(_.duration)

  val totalTaskExecutionUnits: Double = taskExecutionReports.values.map(_.task.executionUnits).sum

  // totalTaskExecutionDuration alone is not a useful stat as it doesnt take into account concurrency
  private val totalTaskExecutionDuration: FiniteDuration = taskExecutionDurations.reduce(_ + _)
  val maxTaskExecutionDuration: FiniteDuration = taskExecutionDurations.max
  val minTaskExecutionDuration: FiniteDuration = taskExecutionDurations.min
  val meanTaskExecutionDuration: FiniteDuration = totalTaskExecutionDuration / taskExecutionReports.size

  val totalLocalFetches: Int = taskExecutionReports.values.map(_.localResources).sum
  val totalRemoteFetches: Int = taskExecutionReports.values.map(_.remoteResources).sum
  val simulationLocalityRate: Double = totalLocalFetches / (totalRemoteFetches + totalLocalFetches)
}

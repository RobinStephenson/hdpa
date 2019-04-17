package tech.robins

import com.typesafe.config.Config

case class SimulationConfiguration(
  schedulerName: String,
  realTimeDelaysEnabled: Boolean,
  workGenerationConfiguration: WorkGenerationConfiguration,
  reportOutputPath: String
)

object SimulationConfiguration {
  def apply(tsConfig: Config): SimulationConfiguration = {
    val realTimeDelaysEnabled = tsConfig.getBoolean("simulation.useRealTimeDelays")
    val schedulerName = tsConfig.getString("simulation.schedulerName")
    val reportOutputPath = tsConfig.getString("simulation.reportOutputPath")
    val workGenerationConfiguration = WorkGenerationConfiguration.fromConfig(tsConfig)
    SimulationConfiguration(schedulerName, realTimeDelaysEnabled, workGenerationConfiguration, reportOutputPath)
  }
}

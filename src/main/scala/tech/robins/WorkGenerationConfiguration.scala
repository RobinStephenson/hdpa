package tech.robins

import com.typesafe.config.Config
case class WorkGenerationConfiguration(generatorName: String, executionUnitsDistribution: NormalDistribution)

object WorkGenerationConfiguration {
  def fromConfig(config: Config): WorkGenerationConfiguration = {
    val genConfig = config.getConfig("simulation.workloadGeneration")
    val generatorName = genConfig.getString("generatorName")
    val executionUnitsMean = genConfig.getDouble("executionUnitsPerTask.mean")
    val executionUnitsStandardDeviation = genConfig.getDouble("executionUnitsPerTask.standardDeviation")
    val executionUnitsDistribution = NormalDistribution(executionUnitsMean, executionUnitsStandardDeviation)
    WorkGenerationConfiguration(generatorName, executionUnitsDistribution)
  }
}

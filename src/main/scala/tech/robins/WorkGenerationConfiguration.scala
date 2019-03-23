package tech.robins

import com.typesafe.config.Config

import scala.collection.JavaConverters._

case class WorkGenerationConfiguration(generatorName: String, executionUnitsDistribution: Distribution)

object WorkGenerationConfiguration {
  def fromConfig(config: Config): WorkGenerationConfiguration = {
    val genConfig = config.getConfig("simulation.workloadGeneration")
    val generatorName = genConfig.getString("generatorName")
    val distributionConfig = genConfig.getConfig("executionUnitsPerTaskDistribution")
    val executionUnitsDistribution = extractExecutionUnitsDistribution(distributionConfig)
    WorkGenerationConfiguration(generatorName, executionUnitsDistribution)
  }

  private def extractExecutionUnitsDistribution(execUnitsDistributionConfig: Config): Distribution = {
    val executionUnitsDistributionType = execUnitsDistributionConfig.getString("type")
    executionUnitsDistributionType match {
      case "Normal" =>
        val executionUnitsMean = execUnitsDistributionConfig.getDouble("mean")
        val executionUnitsStandardDeviation = execUnitsDistributionConfig.getDouble("standardDeviation")
        NormalDistribution(executionUnitsMean, executionUnitsStandardDeviation)
      case "Buckets" =>
        val bucketConfigs = execUnitsDistributionConfig.getConfigList("buckets").asScala.toList
        BucketDistribution(bucketConfigs.map(extractBucket))
    }
  }

  private def extractBucket(bucketConfig: Config): Bucket = {
    val likelihood = bucketConfig.getInt("likelihood")
    val minValue = bucketConfig.getDouble("minValue")
    val maxValue = bucketConfig.getDouble("maxValue")
    Bucket(minValue, maxValue, likelihood)
  }
}

package tech.robins

import scala.util.Random

trait Distribution {
  def random(): Double
}

case class NormalDistribution(mean: Double, standardDeviation: Double) extends Distribution {
  def random(): Double = Random.nextGaussian() * standardDeviation + mean
}

case class BucketDistribution(buckets: List[Bucket]) extends Distribution {

  def random(): Double = {
    val bucket = chooseWeightedRandomBucket()
    bucket.random()
  }

  private def chooseWeightedRandomBucket(): Bucket = { // TODO test
    val totalLikelihood = buckets.map(_.likelihood).sum
    var r = Random.nextInt(totalLikelihood - 1) + 1 // -1 + 1 because we need range to start at 1
    var chosenBucket: Bucket = null
    var i = 0
    while (chosenBucket == null) {
      val bucket = buckets(i)
      r = r - bucket.likelihood
      if (r <= 0) chosenBucket = bucket
      i += 1
    }
    chosenBucket
  }
}

case class Bucket(minValue: Double, maxValue: Double, likelihood: Int) {
  def random(): Double = minValue + (maxValue - minValue) * Random.nextDouble()
}

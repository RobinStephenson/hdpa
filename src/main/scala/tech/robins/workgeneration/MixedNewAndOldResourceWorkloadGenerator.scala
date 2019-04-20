package tech.robins.workgeneration

import java.util.UUID

import akka.actor.Props
import tech.robins._

import scala.collection.mutable
import scala.util.Random

class MixedNewAndOldResourceWorkloadGenerator(executionUnitsDistribution: Distribution)
    extends AbstractWorkloadGenerator {
  private val numberOfTasks = 50

  private val existingResourceIds: mutable.ArrayBuffer[String] = mutable.ArrayBuffer.empty

  private def createTaskWithNewResource(): Task = {
    val resource = UUID.randomUUID().toString
    existingResourceIds += resource
    ImaginaryTask(Set(resource), executionUnitsDistribution.random())
  }

  private def createTaskWithExistingResource(): Task = {
    val randomIndex = Random.nextInt(existingResourceIds.size)
    val resource = existingResourceIds(randomIndex)
    ImaginaryTask(Set(resource), executionUnitsDistribution.random())
  }

  protected def generateWork(sendTask: Task => Unit): WorkGenerationReport = {
    for (_ <- Range(0, numberOfTasks / 2)) { // divide by 2 because 2 tasks are created in each iteration
      sendTask(createTaskWithNewResource())
      sendTask(createTaskWithExistingResource())
    }
    WorkGenerationReport(numberOfTasks)
  }
}

object MixedNewAndOldResourceWorkloadGenerator {
  def props(config: WorkGenerationConfiguration): Props =
    Props(new MixedNewAndOldResourceWorkloadGenerator(config.executionUnitsDistribution))
}

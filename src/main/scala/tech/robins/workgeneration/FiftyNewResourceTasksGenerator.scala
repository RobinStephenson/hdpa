package tech.robins.workgeneration

import java.util.UUID

import akka.actor.Props
import tech.robins._

import scala.collection.mutable

class FiftyNewResourceTasksGenerator(workGenerationConfiguration: WorkGenerationConfiguration)
    extends AbstractWorkloadGenerator {
  private val encounteredResourceIds = mutable.Set.empty[String]

  protected def generateWork(sendTask: Task => Unit): WorkGenerationReport = {
    val totalTasks = 50
    log.info(s"Generating work for scheduler")
    for (_ <- Range(0, 50)) {
      val task = createTask(0, 1)
      log.info(s"Sending new task $task to scheduler")
      sendTask(task)
    }
    WorkGenerationReport(totalTasks)
  }

  protected def createTask(numberOfEncounteredResources: Int, numberOfNewResources: Int): Task =
    if (numberOfEncounteredResources > encounteredResourceIds.size) {
      throw new IllegalArgumentException(
        s"Not enough encountered resources to satisfy request. " +
          s"${encounteredResourceIds.size} available, $numberOfEncounteredResources requested"
      )
    } else {
      val encounteredResourcesDependencies = encounteredResourceIds.take(numberOfEncounteredResources)
      val newResourcesDependencies = for (_ <- Range(0, numberOfNewResources)) yield UUID.randomUUID().toString
      newResourcesDependencies.foreach(encounteredResourceIds.add)
      val allResourceDependencies = encounteredResourcesDependencies.toSet ++ newResourcesDependencies
      val executionUnits = workGenerationConfiguration.executionUnitsDistribution.random()
      val task = ImaginaryTask(allResourceDependencies, executionUnits)
      log.info(s"Created new task $task")
      task
    }
}

object FiftyNewResourceTasksGenerator {
  def props(config: WorkGenerationConfiguration): Props = Props(new FiftyNewResourceTasksGenerator(config))
}

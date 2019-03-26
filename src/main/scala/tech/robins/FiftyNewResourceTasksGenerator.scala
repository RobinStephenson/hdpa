package tech.robins

import java.util.UUID

import AbstractScheduler.NewTaskForScheduling
import akka.actor.{ActorRef, Props}

import scala.collection.mutable

class FiftyNewResourceTasksGenerator(workGenerationConfiguration: WorkGenerationConfiguration)
    extends AbstractWorkloadGenerator {
  private val encounteredResources = mutable.Set.empty[Resource]

  private def generateNewResource(): Resource = {
    val resource = Resource(UUID.randomUUID())
    log.info(s"Generated new resource $resource")
    resource
  }

  protected def generateWork(scheduler: ActorRef): WorkGenerationReport = {
    val totalTasks = 50
    log.info(s"Generating work for scheduler $scheduler")
    for (_ <- Range(0, 50)) {
      val task = createTask(0, 1)
      log.info(s"Sending new task $task to scheduler")
      scheduler ! NewTaskForScheduling(task)
    }
    WorkGenerationReport(totalTasks)
  }

  protected def createTask(numberOfEncounteredResources: Int, numberOfNewResources: Int): Task =
    if (numberOfEncounteredResources > encounteredResources.size) {
      throw new IllegalArgumentException(
        s"Not enough encountered resources to satisfy request. " +
          s"${encounteredResources.size} available, $numberOfEncounteredResources requested"
      )
    } else {
      val encounteredResourcesDependencies = encounteredResources take numberOfEncounteredResources
      val newResourcesDependencies = for (_ <- Range(0, numberOfNewResources)) yield generateNewResource()
      newResourcesDependencies.foreach(encounteredResources.add)
      val allResourceDependencies = encounteredResourcesDependencies.toSet ++ newResourcesDependencies
      val executionUnits = workGenerationConfiguration.executionUnitsDistribution.random()
      val task = Task(UUID.randomUUID(), allResourceDependencies, executionUnits)
      log.info(s"Created new task $task")
      task
    }
}

object FiftyNewResourceTasksGenerator {
  def props(config: WorkGenerationConfiguration): Props = Props(new FiftyNewResourceTasksGenerator(config))
}

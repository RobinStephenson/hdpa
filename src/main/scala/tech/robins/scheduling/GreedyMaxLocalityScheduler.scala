package tech.robins.scheduling
import akka.actor.{ActorRef, Props}
import tech.robins.execution.AbstractExecutionNode.{ExecuteTask, RequestWorkFromScheduler}
import tech.robins.{NodeSchedulingData, Resource, Task}

class GreedyMaxLocalityScheduler extends AbstractScheduler with HasTaskQueue with HasWaitingWorkers {
  protected def onNewTaskForScheduling(task: Task): Unit = {
    taskQueue append task
    waitingWorkers.foreach(_ ! RequestWorkFromScheduler)
  }

  protected def handleNewTaskRequest(requester: ActorRef, schedulingData: NodeSchedulingData): Unit =
    if (taskQueue.nonEmpty) {
      val task = getTaskWithMostLocalResources(schedulingData.presentResources)
      taskQueue -= task
      requester ! ExecuteTask(task)
    } else {
      addToWaitingWorkers(requester)
    }

  protected def getTaskWithMostLocalResources(localResources: Set[Resource]): Task = {
    val localResourceCounts =
      taskQueue.map(task => task -> task.requiredResources.count(localResources.contains)).toMap
    localResourceCounts.maxBy(_._2)._1
  }
}

object GreedyMaxLocalityScheduler {
  val props: Props = Props(new GreedyMaxLocalityScheduler)
}

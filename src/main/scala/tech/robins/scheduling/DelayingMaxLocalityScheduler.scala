package tech.robins.scheduling

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ActorRef, Props}
import tech.robins.{NodeSchedulingData, Task}
import tech.robins.caching.{FixedSizeRoundRobinCache, UnitCacheRemovalHook}
import tech.robins.execution.AbstractExecutionNode.{ExecuteTask, RequestWorkFromScheduler}

case class SkippedWorkerAndCount(worker: ActorRef, count: AtomicInteger) extends UnitCacheRemovalHook {
  def toPair: (ActorRef, AtomicInteger) = worker -> count
}

/** Scheduler which delays task assigment to workers up to the delay threshold if they do not have any local resources.
  * If there are multiple tasks in the queue for which the worker has local resources, the task with the largest number
  *
  * If delayThreshold is set to 1, this is similar to the Matchmaking scheduler.
  * For greater values of delayThreshold, this similar the Delay scheduler.
  * Difference is that workers are skipped, rather than tasks.
  * @param skippedWorkersCacheSize the size of cache for skipped workers. This should be larger than the number of nodes
  *                                in this system to avoid workers waiting longer than the delay threshold, as a result
  *                                of being evicted from the cache.
  * @param delayThreshold The maximum number of times a workers request for new work can be skipped due to lack of local
  *                       resources before the worker is assigned a task anyway.
  */
class DelayingMaxLocalityScheduler(skippedWorkersCacheSize: Int, delayThreshold: Int = 1)
    extends GreedyMaxLocalityScheduler {
  require(skippedWorkersCacheSize >= 1)
  require(delayThreshold >= 1)

  private val skippedWorkersCache = new FixedSizeRoundRobinCache[SkippedWorkerAndCount](skippedWorkersCacheSize)

  private def skippedWorkersCounts = skippedWorkersCache.getItems.map(_.toPair).toMap

  private def sendTask(task: Task, worker: ActorRef): Unit = {
    log.info(s"Sending task $task to worker $worker")
    taskQueue -= task
    skippedWorkersCounts.get(worker).foreach(_.set(0))
    worker ! ExecuteTask(task)
  }

  override protected def handleNewTaskRequest(requester: ActorRef, schedulingData: NodeSchedulingData): Unit = {
    if (taskQueue.nonEmpty) {
      val task = getTaskWithMostLocalResources(schedulingData.presentResourceIds)
      val localResources = task.requiredResourceIds intersect schedulingData.presentResourceIds
      if (localResources.nonEmpty) {
        log.info(s"Task with a local resource identified for $requester")
        sendTask(task, requester)
      } else {
        log.info(s"No tasks with local resources in queue for $requester")
        skippedWorkersCounts.get(requester) match {
          case Some(skipCount) =>
            if (skipCount.get >= delayThreshold) {
              // TODO Could improve by keeping a cache of recently used resources
              //  (therefore likely to have a worker which currently has that resource) and dont assign tasks with those
              //  resources when only assigning because delay threshold is met. Save them for other tasks.
              //  or, a reasonably close heuristic, send the task most recently added to the queue. or pick at random.
              //  At the moment, I think it may be getting the next task in queue, which should be improved if so.
              log.info(s"Worker is past delay threshold, so is assigned task anyway")
              sendTask(task, requester)
            } else {
              val skips = skipCount.incrementAndGet()
              log.info(s"Worker has now been skipped $skips times. Telling worker to request again")
              requester ! RequestWorkFromScheduler
            }
          case None =>
            log.info(s"Worker added to skipped workers cache: $requester. Telling worker to request again")
            skippedWorkersCache add SkippedWorkerAndCount(requester, new AtomicInteger(1))
            requester ! RequestWorkFromScheduler
        }
      }
    } else {
      addToWaitingWorkers(requester)
    }
  }
}

object DelayingMaxLocalityScheduler {
  val props = Props(new DelayingMaxLocalityScheduler(128, 1)) // TODO get from config
}

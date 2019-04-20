package tech.robins.scheduling

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ActorRef, Props}
import com.typesafe.config.{Config, ConfigFactory}
import tech.robins.{NodeSchedulingData, Task}
import tech.robins.caching.FixedSizeRoundRobinCache
import tech.robins.execution.AbstractExecutionNode.{ExecuteTask, RequestWorkFromScheduler}

import scala.util.Random

case class SkippedWorkerAndCount(worker: ActorRef, count: AtomicInteger) {
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
abstract class DelayingMaxLocalityScheduler(skippedWorkersCacheSize: Int, delayThreshold: Int)
    extends GreedyMaxLocalityScheduler {
  require(skippedWorkersCacheSize >= 1)
  require(delayThreshold >= 1)

  private val skippedWorkersCache = new FixedSizeRoundRobinCache[SkippedWorkerAndCount](skippedWorkersCacheSize)

  private def skippedWorkersCounts = skippedWorkersCache.getItems.map(_.toPair).toMap

  protected def sendTask(task: Task, worker: ActorRef): Unit = {
    log.info(s"Sending task $task to worker $worker")
    taskQueue -= task
    skippedWorkersCounts.get(worker).foreach(_.set(0))
    worker ! ExecuteTask(task)
  }

  protected def chooseTaskForNonLocalAssignment: Task

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
              sendTask(chooseTaskForNonLocalAssignment, requester)
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
  val dmlsConfig: Config = ConfigFactory.load().getConfig("simulation.scheduler.dmlsParameters")
  // TODO update configs in appendix
  // TODO mention in section on configuration that DMLS scheduler has extra config parameters. Ignored if other scheduler used.
  val skippedWorkerCacheSize: Int = dmlsConfig.getInt("skippedWorkerCacheSize")
  val delayThreshold: Int = dmlsConfig.getInt("delayThreshold")
}

// When delay threshold is met and a non local assignment is required: the first item in the queue is chosen
class DMLSFirstInQueue(skippedWorkersCacheSize: Int, delayThreshold: Int)
    extends DelayingMaxLocalityScheduler(skippedWorkersCacheSize, delayThreshold) {
  protected def chooseTaskForNonLocalAssignment: Task = taskQueue.head
}

object DMLSFirstInQueue {
  val props: Props = Props(
    new DMLSFirstInQueue(
      DelayingMaxLocalityScheduler.skippedWorkerCacheSize,
      DelayingMaxLocalityScheduler.delayThreshold
    )
  )
}

// When delay threshold is met and a non local assignment is required: the last item in the queue is chosen
class DMLSLastInQueue(skippedWorkersCacheSize: Int, delayThreshold: Int)
    extends DelayingMaxLocalityScheduler(skippedWorkersCacheSize, delayThreshold) {
  protected def chooseTaskForNonLocalAssignment: Task = taskQueue.last
}

object DMLSLastInQueue {
  val props: Props = Props(
    new DMLSLastInQueue(
      DelayingMaxLocalityScheduler.skippedWorkerCacheSize,
      DelayingMaxLocalityScheduler.delayThreshold
    )
  )
}

// When delay threshold is met and a non local assignment is required: one is chosen at random
class DMLSRandom(skippedWorkersCacheSize: Int, delayThreshold: Int)
    extends DelayingMaxLocalityScheduler(skippedWorkersCacheSize, delayThreshold) {
  protected def chooseTaskForNonLocalAssignment: Task = {
    val randomIndex = Random.nextInt(taskQueue.length)
    taskQueue(randomIndex)
  }
}

object DMLSRandom {
  val props: Props = Props(
    new DMLSRandom(
      DelayingMaxLocalityScheduler.skippedWorkerCacheSize,
      DelayingMaxLocalityScheduler.delayThreshold
    )
  )
}

// When delay threshold is met and a non local assignment is required:
// The last task in the queue which does not require a recently used resources is chosen if possible. If no such task
// exists, then the last task in the queue is used.
class DMLSRecentResourceCache(recentResourceIdCacheSize: Int, skippedWorkersCacheSize: Int, delayThreshold: Int)
    extends DelayingMaxLocalityScheduler(skippedWorkersCacheSize, delayThreshold) {
  require(recentResourceIdCacheSize >= 1)

  private val recentResourceIdCache = new FixedSizeRoundRobinCache[String](recentResourceIdCacheSize)

  override protected def sendTask(task: Task, worker: ActorRef): Unit = {
    task.requiredResourceIds.foreach(recentResourceIdCache.add)
    super.sendTask(task, worker)
  }

  protected def chooseTaskForNonLocalAssignment: Task = {
    val tasksWithoutRecentlyUsedResources = taskQueue.filterNot(
      task => recentResourceIdCache.exists(recentResourceId => task.requiredResourceIds.contains(recentResourceId))
    )
    if (tasksWithoutRecentlyUsedResources.nonEmpty)
      tasksWithoutRecentlyUsedResources.last
    else
      taskQueue.last
  }
}

object DMLSRecentResourceCache {
  private val recentResourceCacheSize = DelayingMaxLocalityScheduler.dmlsConfig.getInt("recentResourceCacheSize")
  val props: Props = Props(
    new DMLSRecentResourceCache(
      recentResourceCacheSize,
      DelayingMaxLocalityScheduler.skippedWorkerCacheSize,
      DelayingMaxLocalityScheduler.delayThreshold
    )
  )
}

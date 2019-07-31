package io.github.junheng.akka.monitor.dispatcher

import akka.dispatch.MonitorableThreadFactory

/**
 *
 * @param id                    id of this dispatcher
 * @param poolSize              the number of worker threads that have started but not yet terminated
 * @param activeThreadCount     Returns the number of threads that execute tasks
 * @param parallelism           the targeted parallelism level of this pool
 * @param runningThreadCount    an estimate of the number of worker threads that are not blocked waiting to join tasks or for other managed synchronization
 * @param queuedSubmissionCount an estimate of the number of tasks submitted to this pool that have not yet begun executing
 * @param queuedTaskCount       an estimate of the total number of tasks currently held in queues by worker threads
 * @param stealCount            an estimate of the total number of tasks stolen from one thread's work queue by another.
 */
case class DispatcherStatus
(
  id: String,
  poolSize: Int,
  activeThreadCount: Int,
  parallelism: Int,
  runningThreadCount: Int,
  queuedSubmissionCount: Int,
  queuedTaskCount: Long,
  stealCount: Long
)

case class DispatcherStateList(states: List[DispatcherState])

object DispatcherStateList {
  def create(pools: List[MonitoredForkJoinPool]): DispatcherStateList = {
    val states = pools map { pool =>
      val name = pool.getFactory.asInstanceOf[MonitorableThreadFactory].name
      val quiescent = pool.isQuiescent
      val shutdown = pool.isShutdown
      val terminated = pool.isTerminated
      val terminating = pool.isTerminating

      new DispatcherState(name, quiescent, shutdown, terminated, terminating)

    }

    DispatcherStateList(states)
  }
}


case class DispatcherState(name: String, quiescent: Boolean, shutdown: Boolean, terminated: Boolean, terminating: Boolean)

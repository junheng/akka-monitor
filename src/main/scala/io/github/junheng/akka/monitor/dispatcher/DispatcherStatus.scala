package io.github.junheng.akka.monitor.dispatcher

/**
  *
  * @param id id of this dispatcher
  * @param poolSize the number of worker threads that have started but not yet terminated
  * @param activeThreadCount Returns the number of threads that execute tasks
  * @param parallelism the targeted parallelism level of this pool
  * @param runningThreadCount an estimate of the number of worker threads that are not blocked waiting to join tasks or for other managed synchronization
  * @param queuedSubmissionCount an estimate of the number of tasks submitted to this pool that have not yet begun executing
  * @param queuedTaskCount  an estimate of the total number of tasks currently held in queues by worker threads
  * @param stealCount an estimate of the total number of tasks stolen from one thread's work queue by another.
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
   stealCount: Long)

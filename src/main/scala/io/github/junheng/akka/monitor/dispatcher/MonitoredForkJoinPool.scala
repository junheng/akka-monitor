package io.github.junheng.akka.monitor.dispatcher

import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.Executors

import akka.dispatch.ForkJoinExecutorConfigurator.AkkaForkJoinTask
import akka.dispatch.MonitorableThreadFactory
import akka.event.LoggingAdapter
import io.github.junheng.akka.monitor.dispatcher.MonitoredForkJoinPool.WorkerThreadFactory

import scala.concurrent.forkjoin.ForkJoinPool
import scala.concurrent.{ExecutionContext, Future}

class MonitoredForkJoinPool(parallelism: Int, monitorInterval: Long, threadFactory: WorkerThreadFactory, unhandledExceptionHandler: UncaughtExceptionHandler) extends ForkJoinPool(parallelism, threadFactory, unhandledExceptionHandler, true) {

  override def execute(runnable: Runnable): Unit = {
    if (runnable eq null) {
      throw new NullPointerException
    } else {
      super.execute(new AkkaForkJoinTask(runnable))
    }
  }

  //monitored thread
  Future {
    while (true) {
      try {
        if (getFactory != null && MonitoredForkJoinPool.monitor != null) {
          MonitoredForkJoinPool.monitor(
            DispatcherStatus(
              getFactory.asInstanceOf[MonitorableThreadFactory].name,
              getPoolSize,
              getActiveThreadCount,
              getParallelism,
              getRunningThreadCount,
              getQueuedSubmissionCount,
              getQueuedTaskCount,
              getStealCount
            )
          )
        }
      } catch {
        case ex: Exception => ex.printStackTrace()
      } finally {
        Thread.sleep(monitorInterval)
      }
    }
  }(ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor()))
}

object MonitoredForkJoinPool {
  type WorkerThreadFactory = ForkJoinPool.ForkJoinWorkerThreadFactory
  type UncaughtExceptionHandler = Thread.UncaughtExceptionHandler

  var monitor: (DispatcherStatus) => Unit = _ //hook method please replace before dispatcher started

  def logger(log: LoggingAdapter) = monitor = (status: DispatcherStatus) => {
    import status._
    log.info(s"$id - [PS $poolSize] [ATC $activeThreadCount] [P $parallelism] [RTC $runningThreadCount] [QSC $queuedSubmissionCount] [QTC $queuedTaskCount] [SC $stealCount]]")
  }
}

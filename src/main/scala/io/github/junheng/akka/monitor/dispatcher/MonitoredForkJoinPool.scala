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

  MonitoredForkJoinPool.monitoredForkJoinPools += this

  override def shutdown(): Unit = super.shutdown()

  override def execute(runnable: Runnable): Unit = {
    if (runnable eq null) {
      throw new NullPointerException
    } else {
      super.execute(new AkkaForkJoinTask(runnable))
    }
  }
}

object MonitoredForkJoinPool {
  type WorkerThreadFactory = ForkJoinPool.ForkJoinWorkerThreadFactory
  type UncaughtExceptionHandler = Thread.UncaughtExceptionHandler

  protected var monitoredForkJoinPools = Set[MonitoredForkJoinPool]()

  Future {
    while (true) {
      try {
        //update watched list
        monitoredForkJoinPools = monitoredForkJoinPools.filterNot(p => p.isShutdown || p.isTerminated || p.isTerminating)
        monitoredForkJoinPools.foreach { p =>
          if (!p.isShutdown && !p.isTerminated && !p.isTerminating && p.getFactory != null && MonitoredForkJoinPool.monitor != null) {
            MonitoredForkJoinPool.monitor(
              DispatcherStatus(
                p.getFactory.asInstanceOf[MonitorableThreadFactory].name,
                p.getPoolSize,
                p.getActiveThreadCount,
                p.getParallelism,
                p.getRunningThreadCount,
                p.getQueuedSubmissionCount,
                p.getQueuedTaskCount,
                p.getStealCount
              )
            )
          }
        }
      } catch {
        case ex: Exception => ex.printStackTrace()
      } finally {
        Thread.sleep(60000)
      }
    }
  }(ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor()))

  var monitor: (DispatcherStatus) => Unit = _ //hook method please replace before dispatcher started

  def logger(log: LoggingAdapter) = monitor = (status: DispatcherStatus) => {
    import status._
    log.info(s"$id - [PS $poolSize] [ATC $activeThreadCount] [P $parallelism] [RTC $runningThreadCount] [QSC $queuedSubmissionCount] [QTC $queuedTaskCount] [SC $stealCount]]")
  }
}

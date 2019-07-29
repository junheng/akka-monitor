package io.github.junheng.akka.monitor.dispatcher

import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.Executors

import akka.actor.ActorRef
import akka.dispatch.MonitorableThreadFactory
import akka.dispatch.forkjoin.ForkJoinPool
import io.github.junheng.akka.monitor.dispatcher.MonitoredForkJoinPool.WorkerThreadFactory

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class MonitoredForkJoinPool(parallelism: Int, threadFactory: WorkerThreadFactory, unhandledExceptionHandler: UncaughtExceptionHandler)
  extends ForkJoinPool(parallelism, threadFactory, unhandledExceptionHandler, true) {

  MonitoredForkJoinPool.monitoredForkJoinPools += this

  override def shutdown(): Unit = super.shutdown()

  override def execute(runnable: Runnable): Unit = {
    if (runnable eq null) {
      throw new NullPointerException
    } else {
      super.execute(runnable)
    }
  }
}

object MonitoredForkJoinPool {
  type WorkerThreadFactory = ForkJoinPool.ForkJoinWorkerThreadFactory
  type UncaughtExceptionHandler = Thread.UncaughtExceptionHandler

  protected var monitoredForkJoinPools: Set[MonitoredForkJoinPool] = Set[MonitoredForkJoinPool]()

  protected var watcher: ActorRef = ActorRef.noSender

  protected var reportInterval: FiniteDuration = 15 seconds

  def registerOverseer(watcher: ActorRef, reportInterval: FiniteDuration = 15 seconds): Unit = {
    this.watcher = watcher
    this.reportInterval = reportInterval
  }

  Future {
    while (true) {
      try {
        //update watched list
        monitoredForkJoinPools = monitoredForkJoinPools.filterNot(p => p.isShutdown || p.isTerminated || p.isTerminating)
        monitoredForkJoinPools.foreach { p =>
          if (!p.isShutdown && !p.isTerminated && !p.isTerminating && p.getFactory != null && MonitoredForkJoinPool.watcher != null) {
            MonitoredForkJoinPool.watcher ! DispatcherStatus(
              p.getFactory.asInstanceOf[MonitorableThreadFactory].name,
              p.getPoolSize,
              p.getActiveThreadCount,
              p.getParallelism,
              p.getRunningThreadCount,
              p.getQueuedSubmissionCount,
              p.getQueuedTaskCount,
              p.getStealCount
            )
          }
        }
      } catch {
        case ex: Exception => watcher ! ex
      } finally {
        Thread.sleep(reportInterval.toMillis)
      }
    }
  }(ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor()))

}

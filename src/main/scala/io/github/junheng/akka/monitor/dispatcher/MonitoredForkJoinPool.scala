package io.github.junheng.akka.monitor.dispatcher

import java.lang.Thread.UncaughtExceptionHandler

import akka.actor.ActorRef
import akka.dispatch.MonitorableThreadFactory
import akka.dispatch.forkjoin.ForkJoinPool
import io.github.junheng.akka.monitor.dispatcher.MonitoredForkJoinPool.WorkerThreadFactory

import scala.concurrent.duration.{FiniteDuration, _}
import scala.language.postfixOps

class MonitoredForkJoinPool(parallelism: Int, threadFactory: WorkerThreadFactory, unhandledExceptionHandler: UncaughtExceptionHandler)
  extends ForkJoinPool(parallelism, threadFactory, unhandledExceptionHandler, true) {

  MonitoredForkJoinPool.synchronized {
    MonitoredForkJoinPool.monitoredForkJoinPools :+= this
  }

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

  protected var monitoredForkJoinPools: Seq[MonitoredForkJoinPool] = Seq[MonitoredForkJoinPool]()

  protected var watcher: ActorRef = ActorRef.noSender

  protected var reportInterval: FiniteDuration = 15 seconds

  def registerWatcher(watcher: ActorRef, reportInterval: FiniteDuration = 15 seconds): Unit = {
    MonitoredForkJoinPool.synchronized {
      this.watcher = watcher
      this.reportInterval = reportInterval
    }
  }

  def start(): Unit = {
    new Thread(() => {
      while (true) {
        try {
          MonitoredForkJoinPool.synchronized {
            //update watched list
            Option(watcher) foreach { registerWatcher =>
              registerWatcher ! DispatcherStateList.create(monitoredForkJoinPools.toList)
              monitoredForkJoinPools.filterNot(p => p.isShutdown || p.isTerminated || p.isTerminating).foreach { p =>
                println(s"echo pools: ${monitoredForkJoinPools.size}")
                if (!p.isShutdown && !p.isTerminated && !p.isTerminating && p.getFactory != null && registerWatcher != null) {
                  registerWatcher ! DispatcherStatus(
                    p.getFactory.asInstanceOf[MonitorableThreadFactory].name,
                    p.getPoolSize, p.getActiveThreadCount,
                    p.getParallelism, p.getRunningThreadCount,
                    p.getQueuedSubmissionCount, p.getQueuedTaskCount, p.getStealCount
                  )
                }
              }
            }
          }
        } catch {
          case ex: Throwable => Option(watcher) foreach (_ ! ex)
        } finally {
          Thread.sleep(reportInterval.toMillis)
        }
      }
    }, s"${this.getClass.getSimpleName}-Watch-Thread").start()
  }
}

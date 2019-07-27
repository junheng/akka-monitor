package io.github.junheng.akka.monitor.dispatcher

import java.util.concurrent.ExecutorService

import akka.dispatch.{MonitorableThreadFactory, ExecutorServiceFactory}
import io.github.junheng.akka.monitor.dispatcher.MonitoredForkJoinPool.WorkerThreadFactory

class ForkJoinExecutorServiceFactory(val threadFactory: WorkerThreadFactory, val parallelism: Int, monitorInterval: Long) extends ExecutorServiceFactory {
   def createExecutorService: ExecutorService = new MonitoredForkJoinPool(parallelism, monitorInterval, threadFactory, MonitorableThreadFactory.doNothing)
 }

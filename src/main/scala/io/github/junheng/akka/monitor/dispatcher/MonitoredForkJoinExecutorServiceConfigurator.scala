package io.github.junheng.akka.monitor.dispatcher

import java.util.concurrent.ThreadFactory

import akka.dispatch._
import akka.dispatch.forkjoin.ForkJoinPool
import com.typesafe.config.Config


/**
 * Usages:
 *
 * actor {
 * provider = "akka.cluster.ClusterActorRefProvider"
 * default-dispatcher {
 * executor = "io.github.junheng.akka.overseer.MonitoredForkJoinExecutorServiceConfigurator"
 * monitored-fork-join-executor {
 * parallelism-min = 80
 * parallelism-factor = 50
 * parallelism-max = 2000
 * }
 * }
 * }
 */
class MonitoredForkJoinExecutorServiceConfigurator(_config: Config, prerequisites: DispatcherPrerequisites)
  extends ExecutorServiceConfigurator(_config, prerequisites) {

  private val config = _config.getConfig("monitored-fork-join-executor")

  override def createExecutorServiceFactory(id: String, threadFactory: ThreadFactory): ExecutorServiceFactory = {
    val factory = threadFactory match {
      case m: MonitorableThreadFactory => m.withName(m.name + "-" + id)
      case other => other
    }

    new ForkJoinExecutorServiceFactory(validate(factory), poolConfig)
  }

  def poolConfig: Int = {
    ThreadPoolConfig.scaledPoolSize(
      config.getInt("parallelism-min"),
      config.getDouble("parallelism-factor"),
      config.getInt("parallelism-max")
    )
  }

  def validate(t: ThreadFactory): ForkJoinPool.ForkJoinWorkerThreadFactory = t match {
    case correct: ForkJoinPool.ForkJoinWorkerThreadFactory => correct
    case x => throw new IllegalStateException(s"Need ForkJoinPool.ForkJoinWorkerThreadFactory, actual is ${x.getClass.getName}")
  }
}
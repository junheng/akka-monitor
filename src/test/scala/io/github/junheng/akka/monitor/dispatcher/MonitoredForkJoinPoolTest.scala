package io.github.junheng.akka.monitor.dispatcher

import io.github.junheng.akka.monitor.AbstractActorTest

import scala.concurrent.duration._
import scala.language.postfixOps

class MonitoredForkJoinPoolTest extends AbstractActorTest("pool-test.conf") {

  "MonitoredForkJoinPool" should {
    "can monitor fork join pool status" in {
      MonitoredForkJoinPool.registerWatcher(self, 1 seconds)

      expectMsgAllClassOf[DispatcherStatus](5 seconds)
    }
  }
}

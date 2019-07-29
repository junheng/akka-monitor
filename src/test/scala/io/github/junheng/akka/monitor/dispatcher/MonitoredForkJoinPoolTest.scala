package io.github.junheng.akka.monitor.dispatcher

import scala.concurrent.duration._
import scala.language.postfixOps

class MonitoredForkJoinPoolTest extends AbstractActorTest("pool-test.conf") {

  "MonitoredForkJoinPool" should {
    "can monitor fork join pool status" in {
      MonitoredForkJoinPool.registerOverseer(self, 1 seconds)

      expectMsgAllClassOf[DispatcherStatus](5 seconds)
    }
  }
}
package io.github.junheng.akka.monitor.mailbox

import akka.actor.{Actor, Props}
import io.github.junheng.akka.monitor.AbstractActorTest
import io.github.junheng.akka.monitor.mailbox.MonitoredSafeMailbox.{MessageQueueCreated, OutOfMessageQueueCapacity}
import io.github.junheng.akka.monitor.mailbox.MonitoredSafeMailboxTest.TestWallActor

import scala.concurrent.duration._
import scala.language.postfixOps

class MonitoredSafeMailboxTest extends AbstractActorTest("mailbox-test.conf") {
  "MonitoredSafeMailbox" should {
    "can got message if mailbox created or full" in {
      MonitoredSafeMailbox.registerWatcher(self)
      expectMsgAllClassOf[MessageQueueCreated](5 seconds)
      val testActor = system.actorOf(Props[TestWallActor])
      testActor ! "MSG 0"
      testActor ! "MSG 1"
      expectMsgAllClassOf[OutOfMessageQueueCapacity](5 seconds)
    }
  }

}

object MonitoredSafeMailboxTest {

  class TestWallActor extends Actor {
    override def receive: Receive = {
      case _ => Thread.sleep(10000)
    }
  }

}

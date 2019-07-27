package io.github.junheng.akka.monitor.mailbox

import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch.{MessageQueue, NodeMessageQueue, ProducesMessageQueue, MailboxType}
import com.typesafe.config.Config

import scala.language.postfixOps

class MonitoredSafeMailbox(capacity: Int) extends MailboxType with ProducesMessageQueue[NodeMessageQueue] {

  def this(settings: ActorSystem.Settings, config: Config) = this(config.getInt("mailbox-capacity"))

  override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue = {
    val messageQueue = new MonitoredSafeMessageQueue(capacity, MonitoredSafeMailbox.whenOutOfMessageQueueCapacity)
    MonitoredSafeMailbox.whenMessageQueueCreated(owner, system, messageQueue)
    messageQueue
  }
}

object MonitoredSafeMailbox {
  var whenMessageQueueCreated: (Option[ActorRef], Option[ActorSystem], MessageQueue) => Unit = (_, _, _) => {}
  var whenOutOfMessageQueueCapacity: (ActorRef, ActorRef, Any, Int) => Unit = (_, _, _, _) => {}
}


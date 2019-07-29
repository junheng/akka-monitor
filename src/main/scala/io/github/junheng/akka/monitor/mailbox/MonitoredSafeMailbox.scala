package io.github.junheng.akka.monitor.mailbox

import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch.{MailboxType, MessageQueue, NodeMessageQueue, ProducesMessageQueue}
import com.typesafe.config.Config
import io.github.junheng.akka.monitor.mailbox.MonitoredSafeMailbox.MessageQueueCreated

import scala.language.postfixOps

class MonitoredSafeMailbox(capacity: Int) extends MailboxType with ProducesMessageQueue[NodeMessageQueue] {

  def this(settings: ActorSystem.Settings, config: Config) = this(config.getInt("mailbox-capacity"))

  override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue = {
    val messageQueue = new MonitoredSafeMessageQueue(capacity, MonitoredSafeMailbox.watcher)
    Option(MonitoredSafeMailbox.watcher) foreach (_ ! MessageQueueCreated(owner, system, messageQueue))
    messageQueue
  }
}

object MonitoredSafeMailbox {

  case class MessageQueueCreated(owner: Option[ActorRef], system: Option[ActorSystem], queue: MessageQueue)

  case class OutOfMessageQueueCapacity(sender: ActorRef, receiver: ActorRef, message: Any, current: Int)

  protected var watcher: ActorRef = ActorRef.noSender

  def registerWatcher(watcher: ActorRef): Unit = {
    this.watcher = watcher
  }
}


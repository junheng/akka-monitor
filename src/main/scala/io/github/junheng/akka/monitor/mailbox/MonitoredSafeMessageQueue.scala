package io.github.junheng.akka.monitor.mailbox

import akka.actor.ActorRef
import akka.dispatch.{AbstractNodeQueue, Envelope, MessageQueue, UnboundedMessageQueueSemantics}
import io.github.junheng.akka.monitor.mailbox.MonitoredSafeMailbox.OutOfMessageQueueCapacity

import scala.annotation.tailrec

class MonitoredSafeMessageQueue(capacity: Int, watcher: ActorRef)
  extends AbstractNodeQueue[Envelope]
    with MessageQueue
    with UnboundedMessageQueueSemantics {

  final def enqueue(receiver: ActorRef, handle: Envelope): Unit = {
    if (count() < capacity) add(handle)
    else watcher ! OutOfMessageQueueCapacity(handle.sender, receiver, handle.message, count())
  }

  final def dequeue(): Envelope = poll()

  final def numberOfMessages: Int = count()

  final def hasMessages: Boolean = !isEmpty

  @tailrec final def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = {
    val envelope = dequeue()
    if (envelope ne null) {
      deadLetters.enqueue(owner, envelope)
      cleanUp(owner, deadLetters)
    }
  }
}
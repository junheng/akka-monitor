package io.github.junheng.akka.monitor.mailbox

import akka.actor.{ActorSystem, ActorRef, ActorLogging, Actor}
import akka.dispatch.MessageQueue
import com.typesafe.config.Config
import io.github.junheng.akka.monitor.mailbox.SafeMailboxMonitor._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.collection.JavaConversions._

class SafeMailboxMonitor(config: Config) extends Actor with ActorLogging {

  import context.dispatcher

  private val reportInterval = config.getDuration("report-interval", MILLISECONDS) millisecond

  private val watchedPaths = config.getStringList("watched-actor-paths")

  private var monitoredMQs = Map[ActorRef, MessageQueue]()

  private val reporter = context.system.scheduler.schedule(reportInterval, reportInterval, self, LogMessageQueueDetail)

  override def preStart(): Unit = log.info("started")

  override def postStop(): Unit = reporter.cancel()

  MonitoredSafeMailbox.whenMessageQueueCreated = {
    case (Some(owner), Some(system), mq) =>
      val path = owner.path.toStringWithoutAddress
      watchedPaths.find(r => path.matches(r)) match {
        case Some(matched) => self ! MessageQueueCreated(owner, system, mq)
        case None if watchedPaths.isEmpty => self ! MessageQueueCreated(owner, system, mq)
        case None =>
      }
    case _ => //ignored
  }

  MonitoredSafeMailbox.whenOutOfMessageQueueCapacity = { (sender, receiver, message, count) =>
    val messageType = message.getClass.getSimpleName
    val senderPath = sender.path.toStringWithoutAddress
    val receiptPath = receiver.path.toStringWithoutAddress
    log.warning(s"out of message queue capacity, message [$messageType] from [$senderPath] to [$receiptPath], current count [$count]")
  }

  override def receive: Receive = {
    case MessageQueueCreated(owner, system, mq) => monitoredMQs += owner -> mq

    case LogMessageQueueDetail =>
      val message = new StringBuilder("mailbox status:")
      monitoredMQs filter (_._2.numberOfMessages > 0) foreach { case (owner, queue) =>
        message ++= s"\nmailbox [${owner.path.toStringWithoutAddress}] has [${queue.numberOfMessages}] messages"
      }
      log.info(message.toString())

    case GetMessageQueueDetail => sender ! MessageQueueDetails(monitoredMQs.map(x => x._1.path.toStringWithoutAddress -> x._2.numberOfMessages))

    case GetSpecificMessageQueueDetail(path) =>
      monitoredMQs.find(_._1.path.toStringWithoutAddress == path) match {
        case Some((actorRef, queue)) => sender ! Some(MessageQueueDetail(actorRef.path.toStringWithoutAddress, queue.numberOfMessages))
        case None => sender ! None
      }
  }

}

object SafeMailboxMonitor {

  case object LogMessageQueueDetail

  case object GetMessageQueueDetail

  case class GetSpecificMessageQueueDetail(path: String)

  case class MessageQueueDetail(path: String, numberOfMessages: Int)

  case class MessageQueueDetails(queues: Map[String, Int])

  case class MessageQueueCreated(owner: ActorRef, system: ActorSystem, messageQueue: MessageQueue)

}

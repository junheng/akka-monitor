package io.github.junheng.akka.monitor.mailbox

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.dispatch.MessageQueue
import com.typesafe.config.Config
import io.github.junheng.akka.monitor.mailbox.MonitoredSafeMailbox.{MessageQueueCreated, OutOfMessageQueueCapacity}
import io.github.junheng.akka.monitor.mailbox.SafeMailboxMonitor._

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Make sure this actor not use a heavy load dispatcher
 * the best practice is use special dispatch instead akka default
 *
 * @param reportInterval    log output interval
 * @param watchedPathPatten actor path regex patten to be watched
 */
class SafeMailboxMonitor(reportInterval: Long, watchedPathPatten: List[String]) extends Actor with ActorLogging {

  import context.dispatcher

  def this(config: Config) = this(
    config.getDuration("report-interval", MILLISECONDS),
    config.getStringList("watched-actor-paths").asScala.toList
  )

  protected var monitoredMQs: Map[ActorRef, MessageQueue] = Map[ActorRef, MessageQueue]()

  private val reporter = context.system.scheduler.schedule(reportInterval millis, reportInterval millis, self, LogMessageQueueDetail)

  override def preStart(): Unit = log.info("started")

  override def postStop(): Unit = reporter.cancel()

  override def receive: Receive = {

    case MessageQueueCreated(Some(owner), Some(system), mq) =>
      val path = owner.path.toStringWithoutAddress
      watchedPathPatten.find(r => path.matches(r)) match {
        case Some(_) => monitoredMQs += owner -> mq
        case None if watchedPathPatten.isEmpty => monitoredMQs += owner -> mq //if no actor path a specified then watch all
        case None =>
      }

    case OutOfMessageQueueCapacity(sender, receiver, message, count) =>
      val messageType = message.getClass.getSimpleName
      val senderPath = sender.path.toStringWithoutAddress
      val receiptPath = receiver.path.toStringWithoutAddress
      log.warning(s"out of message queue capacity, message [$messageType] from [$senderPath] to [$receiptPath], current count [$count]")

    case LogMessageQueueDetail =>
      val message = new StringBuilder("mailbox status:")
      monitoredMQs filter (_._2.numberOfMessages > 0) foreach { case (owner, queue) =>
        message ++= s"\nmailbox [${owner.path.toStringWithoutAddress}] has [${queue.numberOfMessages}] messages"
      }
      log.info(message.toString())

    case GetMessageQueueDetail =>
      sender ! MessageQueueDetails(monitoredMQs.map(x => x._1.path.toStringWithoutAddress -> x._2.numberOfMessages))

    case GetSpecificMessageQueueDetail(path) =>
      monitoredMQs.find(_._1.path.toStringWithoutAddress == path) match {
        case Some((actorRef, queue)) => sender ! MessageQueueDetail(actorRef.path.toStringWithoutAddress, queue.numberOfMessages)
        case None => sender ! MessageQueueDetail(path, 0)
      }
  }

}

object SafeMailboxMonitor {

  case object LogMessageQueueDetail

  case object GetMessageQueueDetail

  case class GetSpecificMessageQueueDetail(path: String)

  case class MessageQueueDetail(path: String, numberOfMessages: Int)

  case class MessageQueueDetails(queues: Map[String, Int])

}

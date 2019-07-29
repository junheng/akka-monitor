package io.github.junheng.akka.monitor.dispatcher

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

abstract class AbstractActorTest(configPath: String) extends TestKit(ActorSystem("test", ConfigFactory.load(configPath)))
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll
  with ImplicitSender {


}

organization in ThisBuild := "io.github.junheng.akka"

val akkaVersion = "2.5.19"

lazy val `akka-monitor` = (project in file("."))
  .settings(
    name := "akka-monitor",
    version := "0.4.1.1-SNAPSHOT",
    scalaVersion := "2.12.8",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion withSources(),
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test withSources(),
      "org.scalatest" %% "scalatest" % "3.0.8" % Test withSources()
    ),
    resolvers += "Artifactory" at "http://52.80.172.76:8081/artifactory/bigdata/",
    publishTo := Some("Artifactory Realm" at "http://52.80.172.76:8081/artifactory/bigdata;build.timestamp=" + new java.util.Date().getTime),
    credentials += Credentials(Path.userHome / ".sbt" / "credentials")
  )

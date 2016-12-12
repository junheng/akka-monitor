organization in ThisBuild := "io.github.junheng.akka"

lazy val `akka-monitor` = (project in file("."))
  .settings(
    name := "akka-monitor",
    version := "0.3-SNAPSHOT",
    scalaVersion := "2.11.8",
    libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-http" % "10.0.0" withSources()
    )
  )

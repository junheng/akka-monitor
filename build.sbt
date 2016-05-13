import CommonDependency.dependencies

organization in ThisBuild := "io.github.junheng.akka"

lazy val root = (project in file("."))
  .settings(
    name := "akka-monitor",
    version := "0.2-SNAPSHOT",
    scalaVersion := "2.11.7",
    libraryDependencies ++= dependencies.akka
  )

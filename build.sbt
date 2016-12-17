organization in ThisBuild := "io.github.junheng.akka"

resolvers += "Artifactory" at "http://54.222.244.187:8081/artifactory/bigdata/"

publishTo := Some("Artifactory Realm" at "http://54.222.244.187:8081/artifactory/bigdata;build.timestamp=" + new java.util.Date().getTime)

credentials += Credentials(Path.userHome / ".sbt" / "credentials")

lazy val `akka-monitor` = (project in file("."))
  .settings(
    name := "akka-monitor",
    version := "0.3-SNAPSHOT",
    scalaVersion := "2.11.8",
    libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-http" % "10.0.0" withSources()
    )
  )

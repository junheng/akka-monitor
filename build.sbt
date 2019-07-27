organization in ThisBuild := "io.github.junheng.akka"

lazy val `akka-monitor` = (project in file("."))
  .settings(
    name := "akka-monitor",
    version := "0.4-SNAPSHOT",
    scalaVersion := "2.12.8",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.5.19" withSources()
    ),
    resolvers += "Artifactory" at "http://52.80.172.76:8081/artifactory/bigdata/",
    publishTo := Some("Artifactory Realm" at "http://52.80.172.76:8081/artifactory/bigdata;build.timestamp=" + new java.util.Date().getTime),
    credentials += Credentials(Path.userHome / ".sbt" / "credentials")
  )

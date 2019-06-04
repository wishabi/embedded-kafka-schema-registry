import sbtrelease.Version

val embeddedKafkaVersion = "2.1.0"
val confluentVersion = "5.0.3"
val kafkaVersion = "2.0.1"
val akkaVersion = "2.5.23"
val flipplib = "http://flipplib.jfrog.io/flipplib/"

lazy val publishSettings = Seq(
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  publishArtifact := true,
  publishArtifact in Test := false,
  publishTo := Some("FlippLib Releases" at flipplib + "libs-release-local"),
  // https://github.com/sbt/sbt/issues/3570#issuecomment-432814188
  updateOptions := updateOptions.value.withGigahorse(false)
)

import ReleaseTransformations._

lazy val releaseSettings = Seq(
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    setNextVersion,
    commitNextVersion,
    pushChanges
  ),
  releaseVersionBump := Version.Bump.Minor,
  releaseCrossBuild := true
)

lazy val commonSettings = Seq(
  organization := "com.flipp.dataplatform",
  scalaVersion := "2.12.8",
  crossScalaVersions := Seq("2.12.8", "2.11.12"),
  homepage := Some(url("https://github.com/embeddedkafka/embedded-kafka-schema-registry")),
  parallelExecution in Test := false,
  logBuffered in Test := false,
  fork in Test := true,
  javaOptions ++= Seq("-Xms512m", "-Xmx2048m"),
  scalacOptions += "-deprecation",
  scalafmtOnCompile := true
)

lazy val commonLibrarySettings = libraryDependencies ++= Seq(
  "io.github.embeddedkafka" %% "embedded-kafka-streams" % embeddedKafkaVersion,
  "io.confluent" % "kafka-avro-serializer" % confluentVersion,
  "io.confluent" % "kafka-schema-registry" % confluentVersion,
  "io.confluent" % "kafka-schema-registry" % confluentVersion classifier "tests",
  "org.slf4j" % "slf4j-log4j12" % "1.7.26" % Test,
  "org.scalatest" %% "scalatest" % "3.0.7" % Test,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "javax.ws.rs" % "javax.ws.rs-api" % "2.1.1" artifacts Artifact("javax.ws.rs-api", "jar", "jar")
)

// Add these to match our broker versions, to be removed after cluster upgrade
dependencyOverrides ++= Seq(
  "org.apache.kafka" %% "kafka" % kafkaVersion,
  "org.apache.kafka" % "kafka-streams" % kafkaVersion,
  "org.apache.kafka" % "kafka-clients" % kafkaVersion,
  "org.apache.kafka" % "connect-api" % kafkaVersion
)

lazy val root = (project in file("."))
  .settings(name := "embedded-kafka-schema-registry")
  .settings(publishSettings: _*)
  .settings(commonSettings: _*)
  .settings(commonLibrarySettings)
  .settings(releaseSettings: _*)
  .settings(resolvers ++= Seq(
    "confluent" at "https://packages.confluent.io/maven/",
    "Flipplib Ext-Releases-Local" at flipplib + "ext-release-local",
    "FlippLib Snapshots" at flipplib + "libs-snapshot-local",
    "FlippLib Releases" at flipplib + "libs-release-local",
    Resolver.sonatypeRepo("snapshots"),
    Resolver.jcenterRepo
  ))

// Credentials for flipp artifactory
(sys.env.get("ARTIFACTORY_USER"), sys.env.get("ARTIFACTORY_API_KEY")) match {
  case (Some(username), Some(password)) =>
    credentials += Credentials("Artifactory Realm", "flipplib.jfrog.io", username, password)
  case _ =>
    println("USERNAME and/or PASSWORD is missing")
    credentials ++= Seq()
}

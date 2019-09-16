val scala212 = "2.12.10"
val scala213 = "2.13.0"

lazy val commonSettings = Seq(
  scalaVersion := scala213,
  crossScalaVersions := Seq(scala212, scala213),
  organization := "com.free2move",
  version := "0.1.2",
  licenses += ("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/Free2MoveApp/geo-scala")),
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.1.0-RC2" % Test,
    "org.scalatestplus" %% "scalatestplus-scalacheck" % "3.1.0.0-RC2" % Test,
    "org.scalacheck" %% "scalacheck" % "1.14.0" % Test),
)

lazy val root = project.in(file("."))
  .settings(commonSettings ++ publishSettings ++ Seq(
    name := "geo-scala",
    publishArtifact := false,
    packagedArtifacts := Map()
  ))
  .aggregate(core, circe, polyline)

lazy val core = project.in(file("core"))
.settings(commonSettings ++ publishSettings ++ Seq(
    name := "geo-scala-core"
  ))

val circeVersion = "0.12.1"
lazy val circe = project.in(file("circe"))
  .dependsOn(core)
  .settings(commonSettings ++ publishSettings ++ Seq(
    name := "geo-scala-circe",
    libraryDependencies += "io.circe" %% "circe-core" % circeVersion,
    libraryDependencies += "io.circe" %% "circe-parser" % circeVersion % Test
  ))

lazy val polyline = project.in(file("polyline"))
  .dependsOn(core)
  .settings(commonSettings ++ publishSettings ++ Seq(
    name := "geo-scala-polyline"
  ))

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact in Test := false,
  // The Nexus repo we're publishing to.
  publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
  ),
  pomIncludeRepository := { x => false },
  pomExtra := (
    <scm>
      <url>https://github.com/Free2MoveApp/geo-scala</url>
      <connection>scm:git:git://github.com/Free2MoveApp/geo-scala.git</connection>
      <developerConnection>scm:git:git@github.com:Free2MoveApp/geo-scala.git</developerConnection>
      <tag>HEAD</tag>
    </scm>
    <issueManagement>
      <system>github</system>
      <url>https://github.com/Free2MoveApp/geo-scala/issues</url>
    </issueManagement>
    <developers>
      <developer>
        <id>satabin</id>
        <name>Lucas Satabin</name>
        <email>lucas.satabin@free2move.me</email>
      </developer>
      <developer>
        <id>ybasket</id>
        <name>Yannick Heiber</name>
        <email>yannick.heiber@free2move.me</email>
      </developer>
    </developers>
    <ciManagement>
      <system>travis</system>
      <url>https://travis-ci.com/#!/free2moveapp/geo—scala</url>
    </ciManagement>
  )
)

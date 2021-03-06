name := "hdpa"

version := "1.0"

scalaVersion := "2.12.8"

libraryDependencies += "com.github.scopt" %% "scopt" % "3.7.1"
libraryDependencies += "com.google.code.gson" % "gson" % "1.7.1"
libraryDependencies += "com.typesafe" % "config" % "1.3.2"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.21"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.5.21"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5"
libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "5.3.0.201903130848-r"
libraryDependencies += "org.kohsuke" % "github-api" % "1.95"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"


libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.21" % Test


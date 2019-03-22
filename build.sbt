name := "hdpa"

version := "1.0"

scalaVersion := "2.12.8"

libraryDependencies += "com.google.code.gson" % "gson" % "1.7.1"
libraryDependencies += "com.typesafe" % "config" % "1.3.2"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.21"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.5.21"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.21" % Test


name := "gpsdStream"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.16"
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-udp" % "0.20"
libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka" % "0.22"
libraryDependencies += "io.circe" %% "circe-core" % "0.10.0"
libraryDependencies += "io.circe" %% "circe-generic" % "0.10.0"
libraryDependencies += "io.circe" %% "circe-parser" % "0.10.0"
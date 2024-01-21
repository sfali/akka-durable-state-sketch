ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val AkkaVersion = "2.8.0"
lazy val SlickVersion = "3.4.1"
lazy val AkkaManagementVersion = "1.1.3"
lazy val akkaHttpVersion = "10.2.7"
lazy val circeVersion = "0.14.1"


lazy val root = (project in file("."))
  .settings(
    name := "delivery-date-service",
    libraryDependencies ++= Seq(
//      "org.typelevel" %% "cats-core" % "2.9.0",
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "de.heikoseeberger" %% "akka-http-circe" % "1.39.2",
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion,
      "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
      "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
//      "org.apache.kafka" %% "kafka-clients" % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream-kafka" % "3.0.0", // Check for the latest version
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "ch.qos.logback" % "logback-classic" % "1.4.7",
      "com.typesafe.akka" %% "akka-discovery" % "2.8.0",
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.15" % Test,
      "com.lightbend.akka" %% "akka-persistence-jdbc" % "5.2.0",
      "com.typesafe.akka" %% "akka-persistence-query" % "2.8.0",
      "com.typesafe.slick" %% "slick" % SlickVersion,
      "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion,
      "org.postgresql" % "postgresql" % "42.5.4",
    )
  )

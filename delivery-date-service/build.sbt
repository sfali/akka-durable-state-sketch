ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val AkkaVersion = "2.9.1"
lazy val SlickVersion = "3.4.1"
lazy val AkkaManagementVersion = "1.1.3"
lazy val akkaHttpVersion = "10.6.0"
lazy val circeVersion = "0.14.1"

lazy val root = (project in file("."))
  .settings(
    name := "delivery-date-service",
    resolvers += "Akka Repository" at "https://repo.akka.io/maven/",
    libraryDependencies ++= Seq(
//      "org.typelevel" %% "cats-core" % "2.9.0",
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion,
      "de.heikoseeberger" %% "akka-http-circe" % "1.39.2",
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion,
      "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
      "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
      "com.lightbend.akka" %% "akka-projection-durable-state" % "1.5.1",
      "com.lightbend.akka" %% "akka-projection-eventsourced" % "1.5.1",
      "com.lightbend.akka" %% "akka-projection-r2dbc" % "1.5.2",
      "org.typelevel" %% "cats-core" % "2.9.0",
      "com.typesafe.akka" %% "akka-stream-kafka" % "4.0.0",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "ch.qos.logback" % "logback-classic" % "1.4.7",
      "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.15" % Test,
      "com.lightbend.akka" %% "akka-persistence-r2dbc" % "1.2.1",
      "com.typesafe.akka" %% "akka-http" % "10.6.0",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.6.0", // Align akka-http-spray-json version with akka-http
      "com.typesafe.akka" %% "akka-persistence-query" % AkkaVersion,
      "org.postgresql" % "postgresql" % "42.5.4",
      // fixes this error: java.lang.NoClassDefFoundError: com/typesafe/sslconfig/util/LoggerFactory
      "com.typesafe" %% "ssl-config-core" % "0.6.1"
    )
  )

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val akka_version = "2.8.0"
lazy val SlickVersion = "3.4.1"
lazy val AkkaManagementVersion = "1.1.3"

lazy val root = (project in file("."))
  .settings(
    name := "delivery-date-service",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akka_version,
      "com.typesafe.akka" %% "akka-persistence-typed" % akka_version,
      "com.typesafe.akka" %% "akka-persistence-testkit" % akka_version,
      "com.typesafe.akka" %% "akka-cluster-typed" % akka_version,
      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akka_version,
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion,
      "com.typesafe.akka" %% "akka-serialization-jackson" % akka_version,
      "com.typesafe.akka" %% "akka-slf4j" % akka_version,
      "ch.qos.logback" % "logback-classic" % "1.4.7",
      "com.typesafe.akka" %% "akka-discovery" % "2.8.0",
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akka_version % Test,
      "org.scalatest" %% "scalatest" % "3.2.15" % Test,
      "com.lightbend.akka" %% "akka-persistence-jdbc" % "5.2.0",
      "com.typesafe.akka" %% "akka-persistence-query" % "2.8.0",
      "com.typesafe.slick" %% "slick" % SlickVersion,
      "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion,
      "org.postgresql" % "postgresql" % "42.5.4",
    )
  )

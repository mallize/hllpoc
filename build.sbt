
name := "email-click-tracker"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.13.15"

libraryDependencies ++= Seq(
  "com.twitter" %% "algebird-core" % "0.13.8",
  // "org.postgresql" % "postgresql" % "42.6.0",
  "org.scalatest" %% "scalatest" % "3.2.17" % Test
)

resolvers ++= Seq(
  Resolver.mavenCentral,
)
        

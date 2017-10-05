name := "jumio-client"

organization := "com.snapswap"

version := "1.0.20"

scalaVersion := "2.11.11"

scalacOptions := Seq(
  "-feature",
  "-unchecked",
  "-deprecation",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture",
  "-Ywarn-unused-import",
  "-encoding",
  "UTF-8"
)


libraryDependencies ++= {
  val akkaHttpV = "10.0.10"
  Seq(
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
    "joda-time" % "joda-time" % "2.9.7",
    "org.joda" % "joda-convert" % "1.8.1",
    "com.google.code.findbugs" % "jsr305" % "3.0.1" % "provided",
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV % "test"
  )
}

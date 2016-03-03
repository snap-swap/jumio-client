name := "jumio-client"

organization  := "com.snapswap"

version       := "0.0.1"

scalaVersion  := "2.11.7"

scalacOptions := Seq("-feature", "-unchecked", "-deprecation", "-language:existentials", "-language:higherKinds", "-language:implicitConversions", "-Xfatal-warnings", "-Xlint", "-Yno-adapted-args", "-Ywarn-dead-code", "-Ywarn-numeric-widen", "-Ywarn-value-discard", "-Xfuture", "-Ywarn-unused-import", "-encoding", "UTF-8")

libraryDependencies ++= {
  val akkaV = "2.4.2"
  Seq(
    "com.typesafe.akka" %% "akka-http-core" % akkaV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaV,
    "joda-time" % "joda-time" % "2.9.2",
    "org.joda" % "joda-convert" % "1.8.1",
    "org.scalatest" %% "scalatest" % "2.2.6" % "test",
    "com.typesafe.akka" %% "akka-testkit" % akkaV % "test"
  )
}

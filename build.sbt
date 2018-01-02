name := "formulation"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.0.1" % Test,
  "org.scalacheck" %% "scalacheck" % "1.13.5" % Test,
  "org.scalatest" %% "scalatest" % "3.0.4" % Test,
  "org.apache.avro" % "avro" % "1.8.1"
)

coverageExcludedPackages := "formulation.*RecordN"


addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4")

sourceGenerators in Compile += (sourceManaged in Compile).map(Boilerplate.gen).taskValue
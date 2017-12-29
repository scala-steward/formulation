name := "formulation"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies += "org.typelevel" %% "cats-core" % "1.0.0-RC2"
libraryDependencies += "org.apache.avro" % "avro" % "1.8.1"

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4")

sourceGenerators in Compile += (sourceManaged in Compile).map(Boilerplate.gen).taskValue
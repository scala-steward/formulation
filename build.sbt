val core = project.in(file("core"))
  .settings(commonSettings("core"))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "1.0.1" % Test,
      "org.scalacheck" %% "scalacheck" % "1.13.5" % Test,
      "org.scalatest" %% "scalatest" % "3.0.4" % Test,
      "org.apache.avro" % "avro" % "1.8.2",
      "com.chuusai" %% "shapeless" % "2.3.3"
    ),
    coverageExcludedPackages := "formulation.*RecordN",
    sourceGenerators in Compile += (sourceManaged in Compile).map(Boilerplate.gen).taskValue
  )

val benchmark = project.in(file("benchmark"))
  .settings(commonSettings("benchmark"))
  .settings(
    crossScalaVersions := crossScalaVersions.value.init,
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % "0.9.0",
      "io.circe" %% "circe-generic" % "0.9.0",
      "io.circe" %% "circe-parser" % "0.9.0",
      "com.sksamuel.avro4s" %% "avro4s-core" % "1.8.0"
    )
  )
  .dependsOn(core)
  .enablePlugins(JmhPlugin)

def commonSettings(n: String) = Seq(
  name := s"formulation-$n",
  version := "0.1.0",
  organization := "net.vectos",
  scalaVersion := "2.12.1",
//  scalacOptions ++= Seq(
//    "-language:higherKinds",
//    "-feature",
//    "-deprecation",
//    "-Yno-adapted-args",
//    "-Xlint",
//    "-Xfatal-warnings",
//    "-unchecked"
//  ),
//  scalacOptions in compile ++= Seq(
//    "-Yno-imports",
//    "-Ywarn-numeric-widen"
//  ),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4")
)

val root = project.aggregate(core)
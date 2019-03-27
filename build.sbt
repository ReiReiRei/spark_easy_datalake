name := "spark_easy_datalake"

version := "0.1"

lazy val releaseVersion = "0.1"

scalaVersion := "2.11.12"

lazy val compilerOptions = Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-encoding",
  "utf-8", // Specify character encoding used by source files.
  "-explaintypes", // Explain type errors in more detail.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
  "-language:experimental.macros", // Allow macro definition (besides implementation and application)
  "-language:higherKinds", // Allow higher-kinded types
  "-language:implicitConversions", // Allow definition of implicit functions called views
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xfuture", // Turn on future language features.
  "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
  "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
  "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Ywarn-nullary-unit", // Warn when nullary methods return Unit.
  "-Ywarn-numeric-widen", // Warn when numerics are widened.
  "-Ywarn-unused", // Warn is unused.
  "-Ywarn-value-discard" // Warn when non-Unit expression results are unused.
)

import Dependencies._

lazy val commonSettings = Seq(
  scalacOptions ++= compilerOptions,
  parallelExecution in Test := false,
  fork := true,
  logBuffered in Test := false
)

lazy val settings = commonSettings ++ assemblySettings

lazy val assemblySettings = Seq(
  organization := "org.krivda",
  scalaVersion := "2.11.12",
  scalaVersion := "2.11.12",
  test in assembly := {},
  assemblyJarName in assembly := s"${name.value}-${version.value}.jar",
  assemblyMergeStrategy in assembly := {
    case m if m.toLowerCase.endsWith("manifest.mf")     => MergeStrategy.discard
    case m if m.toLowerCase.matches("meta-inf.*\\.sf$") => MergeStrategy.discard
    case "log4j.properties"                             => MergeStrategy.discard
    case m if m.toLowerCase.startsWith("meta-inf/services/") =>
      MergeStrategy.concat
    case _ => MergeStrategy.first
  },
  publishArtifact in packageDoc := false,
  publishArtifact in packageSrc := false,
  publishArtifact in packageBin := false,
  publishArtifact in makePom := false,
  artifact in (Compile, assembly) := {
    val art = (artifact in (Compile, assembly)).value
    art.withClassifier(None)
  }
)

lazy val commonDependencies = Seq(
  sparkCore % "provided",
  sparkSql % "provided",
  sparkSqlHive % "provided",
  sparkSqlKafka,
  pureconfig
)

lazy val root = (project in file("."))
  .aggregate(
    core,
    example
  )

lazy val core = (project in file("core")).settings(
  settings,
  name := "core",
  version := releaseVersion,
  libraryDependencies ++= commonDependencies
)

lazy val example = (project in file("example"))
  .settings(
    settings,
    name := "example",
    version := releaseVersion,
    libraryDependencies ++= commonDependencies
  )
  .dependsOn(core)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias(
  "check",
  "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck"
)

import Dependencies._

ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.2.0"
ThisBuild / organization := "dev.profunktor"
ThisBuild / organizationName := "ProfunKtor"

ThisBuild / scalafixDependencies += Libraries.organizeImports
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

resolvers += Resolver.sonatypeRepo("snapshots")

val scalafixCommonSettings = inConfig(IntegrationTest)(scalafixConfigSettings(IntegrationTest))

lazy val root = (project in file("."))
  .settings(
    name := "shopping-cart"
  )
  .aggregate(core, tests)

lazy val tests = (project in file("modules/tests"))
  .configs(IntegrationTest)
  .settings(
    name := "shopping-cart-test-suite",
    scalacOptions ++= List("-Ymacro-annotations", "-Yrangepos", "-Wconf:cat=unused:info"),
    testFrameworks += new TestFramework("munit.Framework"),
    Defaults.itSettings,
    scalafixCommonSettings,
    libraryDependencies ++= Seq(
          CompilerPlugin.kindProjector,
          CompilerPlugin.betterMonadicFor,
          CompilerPlugin.semanticDB,
          Libraries.munitCore,
          Libraries.munitScalacheck
        )
  )
  .dependsOn(core)

lazy val core = (project in file("modules/core"))
  .enablePlugins(DockerPlugin)
  .enablePlugins(AshScriptPlugin)
  .settings(
    name := "shopping-cart-core",
    packageName in Docker := "shopping-cart",
    scalacOptions ++= List("-Ymacro-annotations", "-Yrangepos", "-Wconf:cat=unused:info"),
    scalafmtOnCompile := true,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    Defaults.itSettings,
    scalafixCommonSettings,
    dockerBaseImage := "openjdk:8u201-jre-alpine3.9",
    dockerExposedPorts ++= Seq(8080),
    makeBatScripts := Seq(),
    dockerUpdateLatest := true,
    libraryDependencies ++= Seq(
          CompilerPlugin.kindProjector,
          CompilerPlugin.betterMonadicFor,
          CompilerPlugin.semanticDB,
          Libraries.cats,
          Libraries.catsEffect,
          Libraries.catsMeowMtl,
          Libraries.catsRetry,
          Libraries.circeCore,
          Libraries.circeGeneric,
          Libraries.circeParser,
          Libraries.circeRefined,
          Libraries.cirisCore,
          Libraries.cirisEnum,
          Libraries.cirisRefined,
          Libraries.derevoCore,
          Libraries.derevoCats,
          Libraries.derevoCirce,
          Libraries.fs2,
          Libraries.http4sDsl,
          Libraries.http4sServer,
          Libraries.http4sClient,
          Libraries.http4sCirce,
          Libraries.http4sJwtAuth,
          Libraries.javaxCrypto,
          Libraries.log4cats,
          Libraries.logback % Runtime,
          Libraries.newtype,
          Libraries.redis4catsEffects,
          Libraries.redis4catsLog4cats,
          Libraries.refinedCore,
          Libraries.refinedCats,
          Libraries.skunkCore,
          Libraries.skunkCirce,
          Libraries.squants
        )
  )

addCommandAlias("runLinter", ";scalafixAll --rules OrganizeImports")

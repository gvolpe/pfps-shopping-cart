import Dependencies._

ThisBuild / scalaVersion := "2.13.0"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "dev.profunktor"
ThisBuild / organizationName := "ProfunKtor"

resolvers += Resolver.sonatypeRepo("snapshots")

lazy val root = (project in file("."))
  .settings(
    name := "shopping-cart"
  )
  .aggregate(core, tests)

lazy val tests = (project in file("modules/tests"))
  .configs(IntegrationTest)
  .settings(
    name := "shopping-cart-test-suite",
    scalacOptions += "-Ymacro-annotations",
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      compilerPlugin(Libraries.kindProjector cross CrossVersion.full),
      compilerPlugin(Libraries.betterMonadicFor),
      Libraries.scalaTest,
      Libraries.scalaCheck
    )
  )
  .dependsOn(core)

lazy val core = (project in file("modules/core"))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(
    name := "shopping-cart-core",
    packageName := "shopping-cart",
    scalacOptions += "-Ymacro-annotations",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    Defaults.itSettings,
    dockerExposedPorts ++= Seq(8080),
    dockerEnvVars ++= Map(
      "SC_APP_ENV" -> System.get("SC_APP_ENV"),
      "SC_JWT_CLAIM" -> System.getenv("SC_JWT_CLAIM"),
      "SC_JWT_SECRET_KEY" -> System.getenv("SC_JWT_SECRET_KEY"),
      "SC_PASSWORD_SALT" -> System.getenv("SC_PASSWORD_SALT"),
      "SC_ACCESS_TOKEN_SECRET_KEY" -> System.getenv("SC_ACCESS_TOKEN_SECRET_KEY"),
      "SC_ADMIN_USER_TOKEN" -> System.getenv("SC_ADMIN_USER_TOKEN")
    ),
    libraryDependencies ++= Seq(
      compilerPlugin(Libraries.kindProjector cross CrossVersion.full),
      compilerPlugin(Libraries.betterMonadicFor),
      Libraries.cats,
      Libraries.catsEffect,
      Libraries.catsMeowMtl,
      Libraries.catsRetryCore,
      Libraries.catsRetryEffect,
      Libraries.circeCore,
      Libraries.circeGeneric,
      Libraries.circeParser,
      Libraries.circeRefined,
      Libraries.cirisCore,
      Libraries.cirisEnum,
      Libraries.cirisRefined,
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
      Libraries.skunkCirce
      //Libraries.squants, // TODO: Re-enable when there's a release for 2.13.x
    )
  )

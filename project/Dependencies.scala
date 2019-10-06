import sbt._

object Dependencies {

  object Versions {
    val cats          = "2.0.0"
    val catsEffect    = "2.0.0"
    val catsMeowMtl   = "0.4.0"
    val circe         = "0.12.1"
    val ciris         = "0.13.0-RC1"
    val fs2           = "2.0.0"
    val http4s        = "0.21.0-M5"
    val http4sJwtAuth = "0.0.1"
    val log4cats      = "1.0.0"
    val newtype       = "0.4.3"
    val refined       = "0.9.10"
    val skunk         = "0.0.4"
    val squants       = "1.5.0"

    val betterMonadicFor = "0.3.0"
    val kindProjector    = "0.10.3"
    val logback          = "1.2.1"

    val scalaCheck = "1.14.1"
    val scalaTest  = "3.0.8"
  }

  object Libraries {
    def circe(artifact: String): ModuleID  = "io.circe"   %% artifact % Versions.circe
    def ciris(artifact: String): ModuleID  = "is.cir"     %% artifact % Versions.ciris
    def http4s(artifact: String): ModuleID = "org.http4s" %% artifact % Versions.http4s

    lazy val cats        = "org.typelevel" %% "cats-core"     % Versions.cats
    lazy val catsMeowMtl = "com.olegpy"    %% "meow-mtl-core" % Versions.catsMeowMtl
    lazy val catsEffect  = "org.typelevel" %% "cats-effect"   % Versions.catsEffect
    lazy val squants     = "org.typelevel" %% "squants"       % Versions.squants
    lazy val fs2         = "co.fs2"        %% "fs2-core"      % Versions.fs2

    lazy val circeCore       = circe("circe-core")
    lazy val circeGeneric    = circe("circe-generic")
    lazy val circeGenericExt = circe("circe-generic-extras")
    lazy val circeParser     = circe("circe-parser")

    lazy val cirisCore       = ciris("ciris-core")
    lazy val cirisCats       = ciris("ciris-cats")
    lazy val cirisCatsEffect = ciris("ciris-cats-effect")
    lazy val cirisRefined    = ciris("ciris-refined")

    lazy val http4sDsl    = http4s("http4s-dsl")
    lazy val http4sServer = http4s("http4s-blaze-server")
    lazy val http4sCirce  = http4s("http4s-circe")

    lazy val http4sJwtAuth = "dev.profunktor" %% "http4s-jwt-auth" % Versions.http4sJwtAuth

    lazy val refinedCore = "eu.timepit" %% "refined"      % Versions.refined
    lazy val refinedCats = "eu.timepit" %% "refined-cats" % Versions.refined

    lazy val log4cats = "io.chrisdavenport" %% "log4cats-slf4j" % Versions.log4cats
    lazy val newtype  = "io.estatico"       %% "newtype"        % Versions.newtype

    lazy val skunk = "org.tpolecat" %% "skunk-core" % Versions.skunk

    // Compiler plugins
    lazy val betterMonadicFor = "com.olegpy"    %% "better-monadic-for" % Versions.betterMonadicFor
    lazy val kindProjector    = "org.typelevel" %% "kind-projector"     % Versions.kindProjector

    // Runtime
    lazy val logback = "ch.qos.logback" % "logback-classic" % Versions.logback

    // Test
    lazy val scalaTest      = "org.scalatest"  %% "scalatest"        % Versions.scalaTest
    lazy val scalaCheck     = "org.scalacheck" %% "scalacheck"       % Versions.scalaCheck
    lazy val catsEffectLaws = "org.typelevel"  %% "cats-effect-laws" % Versions.catsEffect
  }

}

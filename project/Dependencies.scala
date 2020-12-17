import sbt._

object Dependencies {

  object Versions {
    val cats          = "2.2.0"
    val catsEffect    = "2.2.0"
    val catsMeowMtl   = "0.4.1"
    val catsRetry     = "2.0.0"
    val circe         = "0.13.0"
    val ciris         = "1.2.1"
    val javaxCrypto   = "1.0.1"
    val fs2           = "2.4.6"
    val http4s        = "0.21.13"
    val http4sJwtAuth = "0.0.5"
    val log4cats      = "1.1.1"
    val newtype       = "0.4.3"
    val refined       = "0.9.19"
    val redis4cats    = "0.10.3"
    val skunk         = "0.0.21"
    val squants       = "1.7.0"

    val betterMonadicFor = "0.3.1"
    val kindProjector    = "0.11.2"
    val logback          = "1.2.3"

    val scalaCheck    = "1.15.2"
    val scalaTest     = "3.2.3"
    val scalaTestPlus = "3.2.2.0"
  }

  object Libraries {
    def circe(artifact: String): ModuleID  = "io.circe"   %% artifact % Versions.circe
    def ciris(artifact: String): ModuleID  = "is.cir"     %% artifact % Versions.ciris
    def http4s(artifact: String): ModuleID = "org.http4s" %% artifact % Versions.http4s

    val cats        = "org.typelevel"    %% "cats-core"     % Versions.cats
    val catsMeowMtl = "com.olegpy"       %% "meow-mtl-core" % Versions.catsMeowMtl
    val catsEffect  = "org.typelevel"    %% "cats-effect"   % Versions.catsEffect
    val catsRetry   = "com.github.cb372" %% "cats-retry"    % Versions.catsRetry
    val squants     = "org.typelevel"    %% "squants"       % Versions.squants
    val fs2         = "co.fs2"           %% "fs2-core"      % Versions.fs2

    val circeCore    = circe("circe-core")
    val circeGeneric = circe("circe-generic")
    val circeParser  = circe("circe-parser")
    val circeRefined = circe("circe-refined")

    val cirisCore    = ciris("ciris")
    val cirisEnum    = ciris("ciris-enumeratum")
    val cirisRefined = ciris("ciris-refined")

    val http4sDsl    = http4s("http4s-dsl")
    val http4sServer = http4s("http4s-blaze-server")
    val http4sClient = http4s("http4s-blaze-client")
    val http4sCirce  = http4s("http4s-circe")

    val http4sJwtAuth = "dev.profunktor" %% "http4s-jwt-auth" % Versions.http4sJwtAuth

    val refinedCore = "eu.timepit" %% "refined"      % Versions.refined
    val refinedCats = "eu.timepit" %% "refined-cats" % Versions.refined

    val log4cats = "io.chrisdavenport" %% "log4cats-slf4j" % Versions.log4cats
    val newtype  = "io.estatico"       %% "newtype"        % Versions.newtype

    val javaxCrypto = "javax.xml.crypto" % "jsr105-api" % Versions.javaxCrypto

    val redis4catsEffects  = "dev.profunktor" %% "redis4cats-effects"  % Versions.redis4cats
    val redis4catsLog4cats = "dev.profunktor" %% "redis4cats-log4cats" % Versions.redis4cats

    val skunkCore  = "org.tpolecat" %% "skunk-core"  % Versions.skunk
    val skunkCirce = "org.tpolecat" %% "skunk-circe" % Versions.skunk

    // Compiler plugins
    val betterMonadicFor = "com.olegpy"    %% "better-monadic-for" % Versions.betterMonadicFor
    val kindProjector    = "org.typelevel" % "kind-projector"      % Versions.kindProjector

    // Runtime
    val logback = "ch.qos.logback" % "logback-classic" % Versions.logback

    // Test
    val scalaCheck    = "org.scalacheck"    %% "scalacheck"      % Versions.scalaCheck
    val scalaTest     = "org.scalatest"     %% "scalatest"       % Versions.scalaTest
    val scalaTestPlus = "org.scalatestplus" %% "scalacheck-1-14" % Versions.scalaTestPlus
  }

}

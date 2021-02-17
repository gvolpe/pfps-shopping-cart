import sbt._

object Dependencies {

  object V {
    val cats          = "2.4.2"
    val catsEffect    = "2.3.3"
    val catsMeowMtl   = "0.4.1"
    val catsRetry     = "2.1.0"
    val circe         = "0.13.0"
    val ciris         = "1.2.1"
    val derevo        = "0.12.0"
    val javaxCrypto   = "1.0.1"
    val fs2           = "2.5.0"
    val http4s        = "0.21.13"
    val http4sJwtAuth = "0.0.5"
    val log4cats      = "1.1.1"
    val newtype       = "0.4.4"
    val refined       = "0.9.20"
    val redis4cats    = "0.10.3"
    val skunk         = "0.0.23"
    val squants       = "1.7.0"

    val betterMonadicFor = "0.3.1"
    val kindProjector    = "0.11.3"
    val logback          = "1.2.3"
    val organizeImports  = "0.5.0"
    val semanticDB       = "4.4.8"

    val munit = "0.7.22"
  }

  object Libraries {
    def circe(artifact: String): ModuleID  = "io.circe"   %% s"circe-$artifact"  % V.circe
    def ciris(artifact: String): ModuleID  = "is.cir"     %% artifact            % V.ciris
    def derevo(artifact: String): ModuleID = "tf.tofu"    %% s"derevo-$artifact" % V.derevo
    def http4s(artifact: String): ModuleID = "org.http4s" %% s"http4s-$artifact" % V.http4s

    val cats        = "org.typelevel"    %% "cats-core"     % V.cats
    val catsMeowMtl = "com.olegpy"       %% "meow-mtl-core" % V.catsMeowMtl
    val catsEffect  = "org.typelevel"    %% "cats-effect"   % V.catsEffect
    val catsRetry   = "com.github.cb372" %% "cats-retry"    % V.catsRetry
    val squants     = "org.typelevel"    %% "squants"       % V.squants
    val fs2         = "co.fs2"           %% "fs2-core"      % V.fs2

    val circeCore    = circe("core")
    val circeGeneric = circe("generic")
    val circeParser  = circe("parser")
    val circeRefined = circe("refined")

    val cirisCore    = ciris("ciris")
    val cirisEnum    = ciris("ciris-enumeratum")
    val cirisRefined = ciris("ciris-refined")

    val derevoCore  = derevo("core")
    val derevoCats  = derevo("cats")
    val derevoCirce = derevo("circe")

    val http4sDsl    = http4s("dsl")
    val http4sServer = http4s("blaze-server")
    val http4sClient = http4s("blaze-client")
    val http4sCirce  = http4s("circe")

    val http4sJwtAuth = "dev.profunktor" %% "http4s-jwt-auth" % V.http4sJwtAuth

    val refinedCore = "eu.timepit" %% "refined"      % V.refined
    val refinedCats = "eu.timepit" %% "refined-cats" % V.refined

    val log4cats = "io.chrisdavenport" %% "log4cats-slf4j" % V.log4cats
    val newtype  = "io.estatico"       %% "newtype"        % V.newtype

    val javaxCrypto = "javax.xml.crypto" % "jsr105-api" % V.javaxCrypto

    val redis4catsEffects  = "dev.profunktor" %% "redis4cats-effects"  % V.redis4cats
    val redis4catsLog4cats = "dev.profunktor" %% "redis4cats-log4cats" % V.redis4cats

    val skunkCore  = "org.tpolecat" %% "skunk-core"  % V.skunk
    val skunkCirce = "org.tpolecat" %% "skunk-circe" % V.skunk

    // Runtime
    val logback = "ch.qos.logback" % "logback-classic" % V.logback

    // Test
    val munitCore       = "org.scalameta" %% "munit"            % V.munit
    val munitScalacheck = "org.scalameta" %% "munit-scalacheck" % V.munit

    // Scalafix rules
    val organizeImports = "com.github.liancheng" %% "organize-imports" % V.organizeImports
  }

  object CompilerPlugin {
    val betterMonadicFor = compilerPlugin(
      "com.olegpy" %% "better-monadic-for" % V.betterMonadicFor
    )
    val kindProjector = compilerPlugin(
      "org.typelevel" % "kind-projector" % V.kindProjector cross CrossVersion.full
    )
    val semanticDB = compilerPlugin(
      "org.scalameta" % "semanticdb-scalac" % V.semanticDB cross CrossVersion.full
    )
  }

}

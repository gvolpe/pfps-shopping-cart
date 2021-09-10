import sbt._

object Dependencies {

  object V {
    val cats          = "2.6.1"
    val catsEffect    = "3.2.8"
    val catsRetry     = "3.1.0"
    val circe         = "0.14.1"
    val ciris         = "2.1.1"
    val derevo        = "0.12.6"
    val javaxCrypto   = "1.0.1"
    val fs2           = "3.1.1"
    val http4s        = "0.23.1"
    val http4sJwtAuth = "1.0.0"
    val log4cats      = "2.1.1"
    val monocle       = "3.1.0"
    val newtype       = "0.4.4"
    val refined       = "0.9.27"
    val redis4cats    = "1.0.0"
    val skunk         = "0.2.2"
    val squants       = "1.8.3"

    val betterMonadicFor = "0.3.1"
    val kindProjector    = "0.13.2"
    val logback          = "1.2.6"
    val organizeImports  = "0.5.0"
    val semanticDB       = "4.4.26"

    val weaver = "0.7.6"
  }

  object Libraries {
    def circe(artifact: String): ModuleID  = "io.circe"   %% s"circe-$artifact"  % V.circe
    def ciris(artifact: String): ModuleID  = "is.cir"     %% artifact            % V.ciris
    def derevo(artifact: String): ModuleID = "tf.tofu"    %% s"derevo-$artifact" % V.derevo
    def http4s(artifact: String): ModuleID = "org.http4s" %% s"http4s-$artifact" % V.http4s

    val cats       = "org.typelevel"    %% "cats-core"   % V.cats
    val catsEffect = "org.typelevel"    %% "cats-effect" % V.catsEffect
    val catsRetry  = "com.github.cb372" %% "cats-retry"  % V.catsRetry
    val squants    = "org.typelevel"    %% "squants"     % V.squants
    val fs2        = "co.fs2"           %% "fs2-core"    % V.fs2

    val circeCore    = circe("core")
    val circeGeneric = circe("generic")
    val circeParser  = circe("parser")
    val circeRefined = circe("refined")

    val cirisCore    = ciris("ciris")
    val cirisEnum    = ciris("ciris-enumeratum")
    val cirisRefined = ciris("ciris-refined")

    val derevoCore  = derevo("core")
    val derevoCats  = derevo("cats")
    val derevoCirce = derevo("circe-magnolia")

    val http4sDsl    = http4s("dsl")
    val http4sServer = http4s("ember-server")
    val http4sClient = http4s("ember-client")
    val http4sCirce  = http4s("circe")

    val http4sJwtAuth = "dev.profunktor" %% "http4s-jwt-auth" % V.http4sJwtAuth

    val monocleCore = "dev.optics" %% "monocle-core" % V.monocle

    val refinedCore = "eu.timepit" %% "refined"      % V.refined
    val refinedCats = "eu.timepit" %% "refined-cats" % V.refined

    val log4cats = "org.typelevel" %% "log4cats-slf4j" % V.log4cats
    val newtype  = "io.estatico"   %% "newtype"        % V.newtype

    val javaxCrypto = "javax.xml.crypto" % "jsr105-api" % V.javaxCrypto

    val redis4catsEffects  = "dev.profunktor" %% "redis4cats-effects"  % V.redis4cats
    val redis4catsLog4cats = "dev.profunktor" %% "redis4cats-log4cats" % V.redis4cats

    val skunkCore  = "org.tpolecat" %% "skunk-core"  % V.skunk
    val skunkCirce = "org.tpolecat" %% "skunk-circe" % V.skunk

    // Runtime
    val logback = "ch.qos.logback" % "logback-classic" % V.logback

    // Test
    val catsLaws          = "org.typelevel"       %% "cats-laws"          % V.cats
    val log4catsNoOp      = "org.typelevel"       %% "log4cats-noop"      % V.log4cats
    val monocleLaw        = "dev.optics"          %% "monocle-law"        % V.monocle
    val refinedScalacheck = "eu.timepit"          %% "refined-scalacheck" % V.refined
    val weaverCats        = "com.disneystreaming" %% "weaver-cats"        % V.weaver
    val weaverDiscipline  = "com.disneystreaming" %% "weaver-discipline"  % V.weaver
    val weaverScalaCheck  = "com.disneystreaming" %% "weaver-scalacheck"  % V.weaver

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

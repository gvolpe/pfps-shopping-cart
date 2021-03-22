package shop.domain

import shop.optics.Iso

import derevo.circe.magnolia.encoder
import derevo.derive
import io.circe.Encoder
import io.estatico.newtype.macros._

object healthcheck {
  sealed trait Status
  object Status {
    case object Okay extends Status
    case object Unreachable extends Status

    val _Bool: Iso[Boolean, Status] =
      Iso(b => if (b) Okay else Unreachable, {
        case Okay        => true
        case Unreachable => false
      })

    implicit val jsonEncoder: Encoder[Status] =
      Encoder.forProduct1("status")(_.toString)
  }

  @derive(encoder)
  @newtype
  case class RedisStatus(value: Status)

  @derive(encoder)
  @newtype
  case class PostgresStatus(value: Status)

  @derive(encoder)
  case class AppStatus(
      redis: RedisStatus,
      postgres: PostgresStatus
  )
}

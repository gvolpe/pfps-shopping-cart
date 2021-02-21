package shop.domain

import derevo.circe.{ decoder, encoder }
import derevo.derive
import io.estatico.newtype.macros._

object healthcheck {
  @derive(decoder, encoder)
  @newtype
  case class RedisStatus(value: Boolean)

  @derive(decoder, encoder)
  @newtype
  case class PostgresStatus(value: Boolean)

  @derive(decoder, encoder)
  case class AppStatus(
      redis: RedisStatus,
      postgres: PostgresStatus
  )
}

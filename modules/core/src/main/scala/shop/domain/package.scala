package shop

import dev.profunktor.auth.jwt.JwtToken
import io.circe.{ Decoder, Encoder }
import squants.market.{ Money, USD }

package object domain extends OrphanInstances

// instances for types we don't control
trait OrphanInstances {
  implicit val moneyDecoder: Decoder[Money] =
    Decoder[BigDecimal].map(USD.apply)

  implicit val moneyEncoder: Encoder[Money] =
    Encoder[BigDecimal].contramap(_.amount)

  implicit val tokenEncoder: Encoder[JwtToken] =
    Encoder.forProduct1("access_token")(_.value)
}

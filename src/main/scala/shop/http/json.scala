package shop.http

import cats.effect.Sync
import dev.profunktor.auth.jwt.JwtToken
import io.circe._
import io.circe.generic.semiauto._
import io.circe.refined._
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import java.{ util => ju }
import org.http4s.{ EntityDecoder, EntityEncoder }
import org.http4s.circe.{ jsonEncoderOf, jsonOf }
import shop.domain.auth._
import shop.domain.brand._
import shop.domain.cart._
import shop.domain.category._
import shop.domain.checkout._
import shop.domain.healthcheck._
import shop.domain.item._
import shop.domain.order._
import shop.ext.refined._
import shop.http.auth.roles._

object json {

  implicit def jsonDecoder[F[_]: Sync, A: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]
  implicit def jsonEncoder[F[_]: Sync, A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]

  // ----- Coercible codecs -----
  implicit def coercibleDecoder[A: Coercible[B, ?], B: Decoder]: Decoder[A] =
    Decoder[B].map(_.coerce[A])

  implicit def coercibleEncoder[A: Coercible[B, ?], B: Encoder]: Encoder[A] =
    Encoder[B].contramap(_.repr.asInstanceOf[B])

  implicit def coercibleKeyDecoder[A: Coercible[B, ?], B: KeyDecoder]: KeyDecoder[A] =
    KeyDecoder[B].map(_.coerce[A])

  implicit def coercibleKeyEncoder[A: Coercible[B, ?], B: KeyEncoder]: KeyEncoder[A] =
    KeyEncoder[B].contramap[A](_.repr.asInstanceOf[B])

  // ----- Domain codecs -----

  implicit val brandDecoder: Decoder[Brand] = deriveDecoder[Brand]
  implicit val brandEncoder: Encoder[Brand] = deriveEncoder[Brand]

  implicit val categoryDecoder: Decoder[Category] = deriveDecoder[Category]
  implicit val categoryEncoder: Encoder[Category] = deriveEncoder[Category]

  implicit val itemDecoder: Decoder[Item] = deriveDecoder[Item]
  implicit val itemEncoder: Encoder[Item] = deriveEncoder[Item]

  implicit val createItemDecoder: Decoder[CreateItemParam] = deriveDecoder[CreateItemParam]
  implicit val updateItemDecoder: Decoder[UpdateItemParam] = deriveDecoder[UpdateItemParam]

  implicit val cartItemDecoder: Decoder[CartItem] = deriveDecoder[CartItem]
  implicit val cartItemEncoder: Encoder[CartItem] = deriveEncoder[CartItem]

  implicit val cartTotalEncoder: Encoder[CartTotal] = deriveEncoder[CartTotal]

  implicit val orderEncoder: Encoder[Order] = deriveEncoder[Order]

  implicit val cardDecoder: Decoder[Card] = deriveDecoder[Card]

  implicit val tokenEncoder: Encoder[JwtToken] =
    Encoder.forProduct1("access_token")(_.value)

  implicit val cartDecoder: Decoder[Cart] =
    Decoder.forProduct1("items")(Cart.apply)

  implicit val userDecoder: Decoder[User] = deriveDecoder[User]
  implicit val userEncoder: Encoder[User] = deriveEncoder[User]

  implicit val appStatusEncoder: Encoder[AppStatus] = deriveEncoder[AppStatus]

  implicit val createUserDecoder: Decoder[CreateUser] = deriveDecoder[CreateUser]

  implicit val loginUserDecoder: Decoder[LoginUser] = deriveDecoder[LoginUser]

}

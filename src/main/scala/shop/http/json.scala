package shop.http

import cats.effect.Sync
import dev.profunktor.auth.jwt.JwtToken
import io.circe._
import io.circe.generic.extras.decoding.UnwrappedDecoder
import io.circe.generic.extras.encoding.UnwrappedEncoder
import io.circe.generic.semiauto._
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import java.{ util => ju }
import org.http4s.{ EntityDecoder, EntityEncoder }
import org.http4s.circe.{ jsonEncoderOf, jsonOf }
import shop.domain.auth._
import shop.domain.brand._
import shop.domain.cart._
import shop.domain.category._
import shop.domain.item._

object json {
  implicit def valueClassEncoder[A: UnwrappedEncoder]: Encoder[A] = implicitly
  implicit def valueClassDecoder[A: UnwrappedDecoder]: Decoder[A] = implicitly

  //implicit val itemNameDecoder: Decoder[ItemName]               = deriveDecoder[ItemName]
  //implicit val itemDescriptionDecoder: Decoder[ItemDescription] = deriveDecoder[ItemDescription]
  //implicit val itemPriceDecoder: Decoder[ItemPrice]             = deriveDecoder[ItemPrice]
  //implicit val itemEncoder: Encoder[Item] = deriveEncoder[Item]

  //implicit def mapEncoder[K: Encoder, V: Encoder]: Encoder[Map[K, V]] = deriveEncoder[Map[K, V]]

  implicit val itemDecoder: Decoder[Item] =
    Decoder.forProduct4("uuid", "name", "description", "price")(
      (i: ju.UUID, n: String, d: String, p: BigDecimal) => Item(ItemId(i), ItemName(n), ItemDescription(d), USD(p))
    )

  // TODO: Get semiauto derivation working
  implicit val itemEncoder: Encoder[Item] =
    Encoder.forProduct4("uuid", "name", "description", "price")(
      i => (i.uuid.value, i.name.value, i.description.value, i.price.value)
    )

  implicit val itemKeyDecoder: KeyDecoder[ItemId] =
    new KeyDecoder[ItemId] {
      def apply(key: String): Option[ItemId] =
        KeyDecoder.decodeKeyUUID(key).map(_.coerce[ItemId])
    }

  implicit val itemKeyEncoder: KeyEncoder[ItemId] =
    new KeyEncoder[ItemId] {
      def apply(key: ItemId): String =
        KeyEncoder.encodeKeyUUID(key.value)
    }

  implicit val cartDecoder: Decoder[Cart] =
    Decoder.forProduct1("items")(Cart.apply)

  implicit val cartItemEncoder: Encoder[CartItem] =
    Encoder.forProduct2("item", "quantity")(ci => (ci.item, ci.quantity))

  implicit val tokenEncoder: Encoder[JwtToken] =
    Encoder.forProduct1("accessToken")(_.value)

  implicit def coercibleStringDecoder[A: Coercible[String, ?]]: Decoder[A] =
    Decoder[String].map(_.coerce[A])

  implicit val createUserDecoder: Decoder[CreateUser] = deriveDecoder[CreateUser]

  implicit val loginUserDecoder: Decoder[LoginUser] = deriveDecoder[LoginUser]

  implicit val brandEncoder: Encoder[Brand] =
    Encoder.forProduct1("brand")(_.value)

  implicit val categoryEncoder: Encoder[Category] =
    Encoder.forProduct1("category")(_.value)

  object protocol {
    implicit def jsonDecoder[F[_]: Sync, A: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]
    implicit def jsonEncoder[F[_]: Sync, A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
  }

}

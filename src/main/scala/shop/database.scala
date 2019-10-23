package shop

import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import skunk.Codec
import skunk.codec.all._
import java.{ util => ju }

object database {

  def coercibleVarchar[A: Coercible[String, ?]]: Codec[A] =
    varchar.imap(_.coerce[A])(_.repr.toString)

  def coercibleUuid[A: Coercible[ju.UUID, ?]]: Codec[A] =
    varchar.imap(s => ju.UUID.fromString(s).coerce[A])(_.repr.toString)

}

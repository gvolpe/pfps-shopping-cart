package shop

import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import skunk.Codec
import skunk.codec.all._

object database {

  def coercibleVarchar[A: Coercible[String, ?]]: Codec[A] =
    varchar.imap(_.coerce[A])(_.repr.asInstanceOf[String])

}


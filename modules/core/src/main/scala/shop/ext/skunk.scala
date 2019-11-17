package shop.ext

import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import skunk.Codec

object skunkx {

  implicit class CodecOps[B](codec: Codec[B]) {
    def cimap[A: Coercible[B, *]]: Codec[A] =
      codec.imap(_.coerce[A])(_.repr.asInstanceOf[B])
  }

}

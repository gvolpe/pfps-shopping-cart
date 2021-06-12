package shop.ext.ciris

import shop.ext.derevo.Derive

import _root_.ciris.ConfigDecoder

object configDecoder extends Derive[Decoder.Id]

object Decoder {
  type Id[A] = ConfigDecoder[String, A]
}

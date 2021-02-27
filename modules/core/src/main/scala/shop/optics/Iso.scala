package shop.optics

// If you are a Monocle user, go with monocle.Iso instead
final case class Iso[A, B](get: A => B, reverse: B => A)

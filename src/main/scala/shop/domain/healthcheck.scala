package shop.domain

object healthcheck {
  case class AppStatus(
      redis: Boolean,
      postgres: Boolean
  )
}

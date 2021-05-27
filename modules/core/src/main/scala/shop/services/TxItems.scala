package shop.services

import shop.domain.ID
import shop.domain.brand._
import shop.domain.category._
import shop.domain.item._
import shop.effects.GenUUID

import cats.effect._
import cats.syntax.all._
import skunk._
import skunk.implicits._
import squants.market.Money

// Example of transactions, not used in the application
trait TxItems[F[_]] {
  def create(item: TxItems.ItemCreation): F[ItemId]
}

object TxItems {
  import BrandSQL._, CategorySQL._, ItemSQL._

  case class ItemCreation(
      brand: BrandName,
      category: CategoryName,
      name: ItemName,
      description: ItemDescription,
      price: Money
  )

  def make[F[_]: GenUUID: MonadCancelThrow](
      postgres: Resource[F, Session[F]]
  ): TxItems[F] =
    new TxItems[F] {
      def create(item: ItemCreation): F[ItemId] =
        postgres.use { s =>
          (s.prepare(insertBrand), s.prepare(insertCategory), s.prepare(insertItem)).tupled.use {
            case (ib, ic, it) =>
              s.transaction.surround {
                for {
                  bid <- ID.make[F, BrandId]
                  _   <- ib.execute(Brand(bid, item.brand)).void
                  cid <- ID.make[F, CategoryId]
                  _   <- ic.execute(Category(cid, item.category)).void
                  tid <- ID.make[F, ItemId]
                  itm = CreateItem(item.name, item.description, item.price, bid, cid)
                  _ <- it.execute(tid ~ itm).void
                } yield tid
              }
          }
        }
    }
}

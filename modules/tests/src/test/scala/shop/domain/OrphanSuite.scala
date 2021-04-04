package shop.domain

import cats.kernel.laws.discipline.MonoidTests
import org.scalacheck.{ Arbitrary, Gen }
import squants.market.{ Money, USD }
import suite.DisciplineSuite

object OrphanSuite extends DisciplineSuite {

  implicit val arbMoney: Arbitrary[Money] =
    Arbitrary {
      Gen.posNum[Double].map(USD.apply[Double])
    }

  checkAll("Monoid[Money]", MonoidTests[Money].monoid)

}

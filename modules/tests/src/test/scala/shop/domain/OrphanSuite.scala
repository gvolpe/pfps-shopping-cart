package shop.domain

import shop.generators.moneyGen

import cats.kernel.laws.discipline.MonoidTests
import org.scalacheck.Arbitrary
import squants.market.Money
import weaver.FunSuite
import weaver.discipline.Discipline

object OrphanSuite extends FunSuite with Discipline {

  implicit val arbMoney: Arbitrary[Money] = Arbitrary(moneyGen)

  checkAll("Monoid[Money]", MonoidTests[Money].monoid)

}

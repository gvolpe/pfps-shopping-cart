package shop.domain

import java.util.UUID

import shop.domain.brand.BrandId
import shop.domain.category.CategoryId
import shop.domain.healthcheck.Status
import shop.generators._
import shop.optics.IsUUID

import monocle.law.discipline._
import org.scalacheck.{ Arbitrary, Cogen, Gen }
import weaver.FunSuite
import weaver.discipline.Discipline

object OpticsSuite extends FunSuite with Discipline {

  implicit val arbStatus: Arbitrary[Status] =
    Arbitrary(Gen.oneOf(Status.Okay, Status.Unreachable))

  implicit val uuidCogen: Cogen[UUID] =
    Cogen[(Long, Long)].contramap { uuid =>
      uuid.getLeastSignificantBits -> uuid.getMostSignificantBits
    }

  implicit val brandIdArb: Arbitrary[BrandId] =
    Arbitrary(brandIdGen)

  implicit val brandIdCogen: Cogen[BrandId] =
    Cogen[UUID].contramap[BrandId](_.value)

  implicit val catIdArb: Arbitrary[CategoryId] =
    Arbitrary(categoryIdGen)

  implicit val catIdCogen: Cogen[CategoryId] =
    Cogen[UUID].contramap[CategoryId](_.value)

  checkAll("Iso[Status._Bool]", IsoTests(Status._Bool))

  // we don't really need to test these as they are derived, just showing we can
  checkAll("IsUUID[UUID]", IsoTests(IsUUID[UUID]._UUID))
  checkAll("IsUUID[BrandId]", IsoTests(IsUUID[BrandId]._UUID))
  checkAll("IsUUID[CategoryId]", IsoTests(IsUUID[CategoryId]._UUID))

}

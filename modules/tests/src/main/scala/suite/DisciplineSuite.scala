package suite

import org.scalacheck.Test
import org.scalacheck.Test.{ Passed, Proved }
import org.typelevel.discipline.Laws
import weaver.SimpleIOSuite

trait DisciplineSuite extends SimpleIOSuite {

  override def maxParallelism = 1

  def checkAll(name: String, ruleSet: Laws#RuleSet): Unit =
    ruleSet.all.properties.toList.foreach {
      case (id, prop) =>
        pureTest(s"$name: $id") {
          expect {
            Test.check(prop)(identity).status match {
              case Passed | Proved(_) => true
              case _                  => false
            }
          }
        }
    }

}

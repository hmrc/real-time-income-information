package test_utils

import uk.gov.hmrc.domain.Generator

import java.util.UUID
import scala.util.Random

trait IntegrationBaseSpec extends UnitSpec with ResourceProvider {
  def generateUUId: String           = UUID.randomUUID().toString
  def generateNino: String           = new Generator(new Random).nextNino.toString()
}

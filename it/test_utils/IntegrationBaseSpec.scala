package test_utils

import java.util.UUID

import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.domain.Generator

import scala.util.Random

trait IntegrationBaseSpec extends WordSpec with MustMatchers with MockitoSugar with ResourceProvider {
  def generateUUId: String           = UUID.randomUUID().toString
  def generateNino: String           = new Generator(new Random).nextNino.toString()
}

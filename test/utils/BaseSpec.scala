/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import java.util.UUID

import org.scalatest.{MustMatchers, Suite}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.test.UnitSpec

import scala.util.Random

trait BaseSpec extends UnitSpec with MustMatchers with MockitoSugar with ResourceProvider {
  def generateUUId: String = UUID.randomUUID().toString
  def generateNino: String = new Generator(new Random).nextNino.toString()
}

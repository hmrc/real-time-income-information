/*
 * Copyright 2018 HM Revenue & Customs
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

import models.response.DesSuccessResponse
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._
import services.RealTimeIncomeInformationService

class RealTimeIncomeInformationServiceSpec extends PlaySpec with MustMatchers with BaseSpec {

  "RealTimeIncomeInformationService" when {

    "pickOneValue is called" must {

      "return the request value if it is present in the given DesSuccessResponse object" in {
        val service = new RealTimeIncomeInformationService
        val desResponse = DesSuccessResponse(successMatchOneYear)
        val result = service.pickOneValue("surname", desResponse)
        result mustBe Map("surname" -> JsString("Surname"))
      }

      "return a None if it the requested value is not present in the given DesSuccessResponse object" in {
        val service = new RealTimeIncomeInformationService
        val desResponse = DesSuccessResponse(successMatchOneYear)
        val result = service.pickOneValue("test", desResponse)
        result mustBe Map("test" -> JsString("undefined"))
      }

    }

    "pickAll is called" must {

      "return all requested keys when all keys are present" in {
        val service = new RealTimeIncomeInformationService
        val desResponse = DesSuccessResponse(successMatchOneYear)
        val result = service.pickAll(List("surname", "nationalInsuranceNumber"), desResponse)
        result mustBe JsObject(Map("surname" -> JsString("Surname"), "nationalInsuranceNumber" -> JsString("AB123456C")))
      }

      "return all requested keys when all keys except one are present" in {
        val service = new RealTimeIncomeInformationService
        val desResponse = DesSuccessResponse(successMatchOneYear)
        val result = service.pickAll(List("surname", "nationalInsuranceNumber", "test"), desResponse)
        result mustBe JsObject(Map("surname" -> JsString("Surname"), "nationalInsuranceNumber" -> JsString("AB123456C"),
          "test" -> JsString("undefined")))
      }

    }

  }

}

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

    val service = new RealTimeIncomeInformationService
    val desResponse = DesSuccessResponse(successMatchOneYear)

    "pickOneValue is called" must {

      "return the corresponding value if the requested key is present in the given DesSuccessResponse object" in {
        val result = service.pickOneValue("surname", desResponse.response)
        result mustBe "surname" -> JsString("Surname")
      }

      "return a value of 'undefined' if the requested key is not present in the given DesSuccessResponse object" in {
        val result = service.pickOneValue("test", desResponse.response)
        result mustBe "test" -> JsString("undefined")
      }

    }

    "pickAll is called" when {

      "when a single tax year is requested" must {

        "return all requested values when all keys are present" in {
          val result = service.pickAll(List("surname", "nationalInsuranceNumber"), desResponse)

          result mustBe Json.parse(
            """
              |{
              |"taxYears" : [ {
              |"surname": "Surname",
              |"nationalInsuranceNumber":"AB123456C"
              |}
              |]
              |}
            """.stripMargin)
        }

        "return all requested values plus an 'undefined' when all keys except one are present" in {
          val result = service.pickAll(List("surname", "nationalInsuranceNumber", "test"), desResponse)

          result mustBe Json.parse(
            """
              |{ "taxYears" : [ {
              |"surname": "Surname",
              |"nationalInsuranceNumber":"AB123456C",
              |"test":"undefined"
              |}
              |]
              |}
            """.stripMargin)
        }

      }

      "when multiple tax years are requested" must {

        "return all requested values when all keys are present and the data covers multiple years" in {
          val desResponse = DesSuccessResponse(successMatchTwoYear)

          val expectedJson = Json.parse(
            """
              |{
              |"taxYears" : [
              |{
              |"surname": "Surname",
              |"nationalInsuranceNumber":"AA123456C"
              |},
              |{
              |"surname": "Surname",
              |"nationalInsuranceNumber":"AA123456C"
              |}
              |]
              |}
            """.stripMargin)

          val result = service.pickAll(List("surname", "nationalInsuranceNumber"), desResponse)

          result mustBe expectedJson
        }

      }

    }

  }

}

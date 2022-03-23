/*
 * Copyright 2022 HM Revenue & Customs
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

package models

import org.scalacheck.Gen
import org.scalacheck.Gen._
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsBoolean, JsNull, JsString, Json}
import utils.{BaseSpec, ResourceProvider}

class DesResponseSpec extends BaseSpec with ResourceProvider with ScalaCheckPropertyChecks {

  "DesErrorResponse reads" must {
    "formatting DesSingleError" in {
      val desErrorResponse: DesErrorResponse = noDataFoundNinoJson.as[DesErrorResponse]
      desErrorResponse mustBe DesSingleFailureResponse(
        "NOT_FOUND",
        "The remote endpoint has indicated that there is no data for the Nino."
      )
    }
    "formatting DesMultipleError" in {
      val desErrorResponse: DesErrorResponse = multipleErrors.as[DesErrorResponse]
      desErrorResponse mustBe DesMultipleFailureResponse(
        List(
          DesSingleFailureResponse("INVALID_NINO", "Submission has not passed validation. Invalid parameter nino."),
          DesSingleFailureResponse("INVALID_PAYLOAD", "Submission has not passed validation. Invalid Payload.")
        )
      )
    }

    implicit class GenOps[A](gen: Gen[A]) {
      def zip[B](other: Gen[B]): Gen[(A, B)] =
        gen.flatMap(a => other.map(b => (a, b)))

      def *>[B](other: Gen[B]): Gen[(A, B)] = zip(other)

      def <*[B](other: Gen[B]): Gen[(B, A)] = other.zip(gen)
    }

    val jsValueGen = Gen.oneOf(alphaStr.map(JsString), Gen.oneOf(true, false).map(JsBoolean), const(JsNull))

    val desSuccessResponseGen =
      (posNum[Int] *> some(listOf(jsValueGen))).map(DesSuccessResponse.tupled)

    val desFilteredSuccessResponseGen =
      (posNum[Int] *> listOf(jsValueGen)).map(DesFilteredSuccessResponse.tupled)

    val desSingleFailureResponseGen =
      (alphaStr *> alphaStr).map(DesSingleFailureResponse.tupled)

    val desMultipleFailureResponseGen =
      listOf(desSingleFailureResponseGen).map(DesMultipleFailureResponse)

    val desUnexpectedResponseGen =
      (alphaStr *> alphaStr).map(DesUnexpectedResponse.tupled)

    val desNoResponseGen =
      (alphaStr *> alphaStr).map(DesNoResponse.tupled)

    val desResponseGen: Gen[DesResponse] =
      Gen.oneOf(
        desSuccessResponseGen,
        desFilteredSuccessResponseGen,
        desSingleFailureResponseGen,
        desMultipleFailureResponseGen,
        desUnexpectedResponseGen,
        desNoResponseGen
      )

    "serialize and deserialize a DesResponse type" in {

      forAll(desResponseGen) { response =>
        Json.toJson(response).as[DesResponse] shouldBe response
      }
    }
  }
}

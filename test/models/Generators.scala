/*
 * Copyright 2025 HM Revenue & Customs
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
import org.scalacheck.Gen.{alphaStr, const, listOf, oneOf, posNum, some}
import play.api.libs.json.{JsBoolean, JsNull, JsString, JsValue}

object Generators {

  implicit class GenOps[A](gen: Gen[A]) {
    def zip[B](other: Gen[B]): Gen[(A, B)] =
      gen.flatMap(a => other.map(b => (a, b)))

    def *>[B](other: Gen[B]): Gen[(A, B)] = zip(other)

    def <*[B](other: Gen[B]): Gen[(B, A)] = other.zip(gen)
  }

  val jsValueGen: Gen[JsValue] = oneOf(alphaStr.map(play.api.libs.json.JsString.apply), oneOf(true, false).map(JsBoolean), const(JsNull))

  val desSuccessResponseGen: Gen[DesSuccessResponse] =
    (posNum[Int] *> some(listOf(jsValueGen))).map(DesSuccessResponse.tupled)

  val desFilteredSuccessResponseGen: Gen[DesFilteredSuccessResponse] =
    (posNum[Int] *> listOf(jsValueGen)).map(DesFilteredSuccessResponse.tupled)

  val desSingleFailureResponseGen: Gen[Nothing] =
    (alphaStr *> alphaStr).map(DesSingleFailureResponse.tupled)

  val desMultipleFailureResponseGen: Gen[DesMultipleFailureResponse] =
    listOf(desSingleFailureResponseGen).map(DesMultipleFailureResponse.apply)

  val desUnexpectedResponseGen: Gen[DesUnexpectedResponse] =
    (alphaStr *> alphaStr).map(DesUnexpectedResponse.tupled)

  val desNoResponseGen: Gen[DesNoResponse] =
    (alphaStr *> alphaStr).map(DesNoResponse.tupled)

  val desResponseGen: Gen[DesResponse] =
    oneOf(
      desSuccessResponseGen,
      desFilteredSuccessResponseGen,
      desSingleFailureResponseGen,
      desMultipleFailureResponseGen,
      desUnexpectedResponseGen,
      desNoResponseGen
    )
}
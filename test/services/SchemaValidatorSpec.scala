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

package services

import utils.BaseSpec

class SchemaValidatorSpec extends BaseSpec {

  val SUT = new SchemaValidator

  "validate" must {
    "return true" in {
      SUT.validate(exampleDwpRequest) mustBe true

    }

    "return false " when {
      List(
        ("the nino is invalid", exampleDwpRequestInvalidNino),
        ("the filter fields array contains an empty string field", exampleInvalidDwpEmptyStringField),
        ("the filter fields array contains duplicate fields", exampleInvalidDwpDuplicateFields),
        ("the filter fields array is empty", exampleInvalidDwpEmptyFieldsRequest),
        ("the request contains an unexpected filter field", exampleInvalidFilterFieldDwpRequest),
        ("the request contains an unexpected matching field", exampleInvalidMatchingFieldDwpRequest)
      ).foreach {
        case (testName, json) => testName in {
          SUT.validate(json) mustBe false
        }
      }
    }
  }
}

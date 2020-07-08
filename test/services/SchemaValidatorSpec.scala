package services

import utils.BaseSpec

class SchemaValidatorSpec extends BaseSpec {

  val SUT = new SchemaValidator

  "validate" must {
    "return true" in {

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

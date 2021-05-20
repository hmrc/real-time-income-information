/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.Json

case class RequestDetails(
    nino: String,
    serviceName: String,
    fromDate: String,
    toDate: String,
    surname: String,
    firstName: Option[String],
    middleName: Option[String],
    gender: Option[String],
    initials: Option[String],
    dateOfBirth: Option[String],
    filterFields: List[String]
) {
  require(nino.matches("^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D ]$"), "Submission has not passed validation. Invalid nino in payload.")
  require(serviceName.matches("^[a-zA-Z0-9 &`\\-\\'\\.^]{1,128}$"), "Submission has not passed validation. Invalid serviceName in payload.")
  require(
    toDate.matches("^(((19|20)([2468][048]|[13579][26]|0[48])|2000)[-]02[-]29|((19|20)[0-9]{2}[-](0[469]|11)[-](0[1-9]|1[0-9]|2[0-9]|30)|(19|20)[0-9]{2}[-](0[13578]|1[02])[-](0[1-9]|[12][0-9]|3[01])|(19|20)[0-9]{2}[-]02[-](0[1-9]|1[0-9]|2[0-8])))$"),
    "Submission has not passed validation. Invalid toDate in payload."
  )
  require(
    fromDate.matches("^(((19|20)([2468][048]|[13579][26]|0[48])|2000)[-]02[-]29|((19|20)[0-9]{2}[-](0[469]|11)[-](0[1-9]|1[0-9]|2[0-9]|30)|(19|20)[0-9]{2}[-](0[13578]|1[02])[-](0[1-9]|[12][0-9]|3[01])|(19|20)[0-9]{2}[-]02[-](0[1-9]|1[0-9]|2[0-8])))$"),
    "Submission has not passed validation. Invalid fromDate in payload."
  )
  require(surname.matches("^[a-zA-Z &`\\-\\'\\.^]{1,35}$"), "Submission has not passed validation. Invalid surname in payload.")
  require(RequestDetails.optionalNameValidation(middleName), "Submission has not passed validation. Invalid middle name in payload.")
  require(RequestDetails.optionalNameValidation(firstName), "Submission has not passed validation. Invalid first name in payload.")
  require(RequestDetails.optionalGenderValidation(gender), "Submission has not passed validation. Invalid gender in payload.")
  require(RequestDetails.optionalInitialsValidation(initials), "Submission has not passed validation. Invalid initials in payload.")
  require(
   RequestDetails.optionalDateValidation(dateOfBirth),
    "Submission has not passed validation. Invalid date of birth in payload."
  )
  require(RequestDetails.filterFieldsValidation(filterFields), "Submission has not passed validation. Invalid filter-fields in payload.")
}

object RequestDetails {
  implicit val formats = Json.format[RequestDetails]

  def toMatchingRequest(r: RequestDetails): DesMatchingRequest =
    DesMatchingRequest(r.fromDate, r.toDate, r.surname, r.firstName, r.middleName, r.gender, r.initials, r.dateOfBirth)

  def optionalNameValidation(userName: Option[String]): Boolean = {
    userName match {
      case None => true
      case Some(name) if name.matches("^[a-zA-Z &`\\-\\'\\.^]{1,35}$") => true
      case _ => false
    }
  }

  def optionalGenderValidation(userGender: Option[String]): Boolean = {
    userGender match {
      case None => true
      case Some(gender) if gender == "M" => true
      case Some(gender) if gender == "F" => true
      case _ => false
    }
  }

  def optionalInitialsValidation(userInitials: Option[String]): Boolean = {
    userInitials match {
      case None => true
      case Some(initials) if initials.matches("^[a-zA-Z &`\\-\\'\\.^]{1,35}$") => true
      case _ => false
    }
  }

  def optionalDateValidation(userDate: Option[String]): Boolean = {
    userDate match {
      case None => true
      case Some(date) if date.matches("^(((19|20)([2468][048]|[13579][26]|0[48])|2000)[-]02[-]29|((19|20)[0-9]{2}[-](0[469]|11)[-](0[1-9]|1[0-9]|2[0-9]|30)|(19|20)[0-9]{2}[-](0[13578]|1[02])[-](0[1-9]|[12][0-9]|3[01])|(19|20)[0-9]{2}[-]02[-](0[1-9]|1[0-9]|2[0-8])))$")
        => true
      case _ => false
    }
  }

  def filterFieldsValidation(filterFields: List[String]): Boolean = {
    val expectedFilterFields = List( "taxYear",
      "taxYearIndicator",
      "hmrcOfficeNumber",
      "employerPayeRef",
      "employerName1",
      "employerName2",
      "employerTradeName1",
      "employerTradeName2",
      "nationalInsuranceNumber",
      "surname",
      "forename",
      "secondForename",
      "initials",
      "dateOfBirth",
      "gender",
      "uniqueEmploymentSequenceNumber",
      "payrollId",
      "employmentStartDate",
      "employmentEndDate",
      "taxablePayInPeriod",
      "paymentsNotSubjectToTaxOrNICS",
      "valueOfDeductions",
      "payAfterStatutoryDeductions",
      "benefitsTaxedViaPayroll",
      "employeePensionContributions",
      "itemsSubjectToClass1NIC",
      "employeePensionContributionsNotPaid",
      "taxDeductedOrRefunded",
      "grossEarningsForNICs",
      "employeesContributionsInPeriod",
      "taxablePayToDate",
      "totalTaxToDate",
      "employeesContributionsYearToDate",
      "numberOfNormalHoursWorked",
      "payFrequency",
      "paymentDate",
      "weeklyPeriodNumber",
      "monthlyPeriodNumber",
      "earningsPeriodsCovered",
      "paymentAfterLeaving",
      "irregularPayment",
      "uniquePaymentId",
      "paymentConfidenceStatus",
      "occPenIndicator",
      "onStrikeWithinPayPeriod",
      "unpaidAbsencePayPeriod",
      "benefitsTaxedYeartoDate",
      "pensionContributionsYearToDate",
      "pensionContributionsNotPaidYeartoDate",
      "taxCode",
      "multipleIPSIndicator",
      "bacsPaymentAmount",
      "statutoryMaternityPayYeartoDate",
      "niLetter",
      "directorsNIC",
      "latePayeReportingReason",
      "schemeExemptFromElectronicFiling",
      "flexiblyAccessingPensionRights",
      "pensionsDeathBenefit",
      "flexibleDrawdownTaxablePayment",
      "flexibleDrawdownNonTaxablePayment",
      "taxRegime",
      "annualAmountOfOccupationalPension",
      "taxCodeBasisisNonCumulative",
      "statutoryPaternityPayYearToDate",
      "valueofStatutoryAdoptionPayYearToDate",
      "sharedParentalPayYearToDate",
      "partnerSurnameOrFamilyName",
      "partnerForenameOrGivenName",
      "partnerInitials",
      "partnerSecondForename",
      "partnerNINO",
      "grossEarningsForNICsYearToDate",
      "taxWeekNumberOfAppointmentOfDirector",
      "totalEmployerNIContributionsInPeriod",
      "trivialCommutationPaymentTypeA",
      "trivialCommutationPaymentTypeB",
      "trivialCommutationPaymentTypeC",
      "seriousillHealthLumpSum",
      "hmrcReceiptTimestamp",
      "rtiReceivedDate",
      "apiAvailableTimestamp",
      "paymentNoLongerValid"
      )

    val isEmptyString = filterFields map (_.isEmpty)
    val isDuplicateString = filterFields.distinct.size == filterFields.size
    val isEmptyList = filterFields.isEmpty
    val isValidFilter = filterFields map (expectedFilterFields.contains(_))

    isEmptyString.filter(_ == true).size == 0 && isDuplicateString && !isEmptyList && isValidFilter.filter(_ == false).size ==0
  }
}

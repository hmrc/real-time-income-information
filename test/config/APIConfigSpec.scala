/*
 * Copyright 2026 HM Revenue & Customs
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

package config

import testUtils.BaseSpec


class APIConfigSpec extends BaseSpec {

  val filterFullAccessScope = "filter:real-time-income-information-full"
  val filterSgAccessScope = "filter:real-time-income-information-sg"
  val filterCspAccessScope = "filter:real-time-income-information-csp"

  private lazy val fullScope: Set[String] = Set(
    "annualAmountOfOccupationalPension",
    "apiAvailableTimestamp",
    "bacsPaymentAmount",
    "benefitsTaxedViaPayroll",
    "benefitsTaxedYeartoDate",
    "dateOfBirth",
    "directorsNIC",
    "earningsPeriodsCovered",
    "employeePensionContributions",
    "employeePensionContributionsNotPaid",
    "employeesContributionsInPeriod",
    "employeesContributionsYearToDate",
    "employerName1",
    "employerName2",
    "employerPayeRef",
    "employerTradeName1",
    "employerTradeName2",
    "employmentEndDate",
    "employmentStartDate",
    "flexibleDrawdownNonTaxablePayment",
    "flexibleDrawdownTaxablePayment",
    "flexiblyAccessingPensionRights",
    "forename",
    "gender",
    "grossEarningsForNICs",
    "grossEarningsForNICsYearToDate",
    "hmrcOfficeNumber",
    "hmrcReceiptTimestamp",
    "initials",
    "irregularPayment",
    "itemsSubjectToClass1NIC",
    "latePayeReportingReason",
    "monthlyPeriodNumber",
    "multipleIPSIndicator",
    "nationalInsuranceNumber",
    "niLetter",
    "numberOfNormalHoursWorked",
    "occPenIndicator",
    "onStrikeWithinPayPeriod",
    "partnerForenameOrGivenName",
    "partnerInitials",
    "partnerNINO",
    "partnerSecondForename",
    "partnerSurnameOrFamilyName",
    "payAfterStatutoryDeductions",
    "payFrequency",
    "paymentAfterLeaving",
    "paymentConfidenceStatus",
    "paymentDate",
    "paymentNoLongerValid",
    "paymentsNotSubjectToTaxOrNICS",
    "payrollId",
    "pensionContributionsNotPaidYeartoDate",
    "pensionContributionsYearToDate",
    "pensionsDeathBenefit",
    "rtiReceivedDate",
    "schemeExemptFromElectronicFiling",
    "secondForename",
    "seriousillHealthLumpSum",
    "sharedParentalPayYearToDate",
    "statutoryMaternityPayYeartoDate",
    "statutoryPaternityPayYearToDate",
    "surname",
    "taxCode",
    "taxCodeBasisisNonCumulative",
    "taxDeductedOrRefunded",
    "taxRegime",
    "taxWeekNumberOfAppointmentOfDirector",
    "taxYear",
    "taxYearIndicator",
    "taxablePayInPeriod",
    "taxablePayToDate",
    "totalEmployerNIContributionsInPeriod",
    "totalTaxToDate",
    "trivialCommutationPaymentTypeA",
    "trivialCommutationPaymentTypeB",
    "trivialCommutationPaymentTypeC",
    "uniqueEmploymentSequenceNumber",
    "uniquePaymentId",
    "unpaidAbsencePayPeriod",
    "valueOfDeductions",
    "valueofStatutoryAdoptionPayYearToDate",
    "weeklyPeriodNumber"
  )

  private lazy val limitedScopeFields : Set[String] = Set("employmentEndDate", "payFrequency", "paymentDate", "taxablePayInPeriod")

  "APIConfig" must {
    "return scope" when {
      "findScope with full filters" in {
        val result = APIConfig.findScope(filterFullAccessScope).get
        result.name mustBe filterFullAccessScope
        result.fields must contain theSameElementsAs fullScope
      }
      "findScope with sg filters" in {
        val result = APIConfig.findScope(filterSgAccessScope).get
        result.name  mustBe filterSgAccessScope
        result.fields must contain theSameElementsAs limitedScopeFields
      }
      "findScope with csp filters" in {
        val result = APIConfig.findScope(filterCspAccessScope).get
        result.name  mustBe filterCspAccessScope
        result.fields must contain theSameElementsAs limitedScopeFields
      }
    }

    "return none" when {
      "scopeName does not exist" in {
        APIConfig.findScope("Test").isDefined mustBe false
      }
    }
  }
}

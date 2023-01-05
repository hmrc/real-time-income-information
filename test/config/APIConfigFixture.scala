/*
 * Copyright 2023 HM Revenue & Customs
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

object APIConfigFixture {

  val fullScope: Option[ApiScope] = Some(ApiScope("filter:real-time-income-information-full",List(
    ApiField(1,"annualAmountOfOccupationalPension"),
    ApiField(2,"apiAvailableTimestamp"),
    ApiField(3,"bacsPaymentAmount"),
    ApiField(4,"benefitsTaxedViaPayroll"),
    ApiField(5,"benefitsTaxedYeartoDate"),
    ApiField(6,"dateOfBirth"),
    ApiField(7,"directorsNIC"),
    ApiField(8,"earningsPeriodsCovered"),
    ApiField(9,"employeePensionContributions"),
    ApiField(10,"employeePensionContributionsNotPaid"),
    ApiField(11,"employeesContributionsInPeriod"),
    ApiField(12,"employeesContributionsYearToDate"),
    ApiField(13,"employerName1"),
    ApiField(14,"employerName2"),
    ApiField(15,"employerPayeRef"),
    ApiField(16,"employerTradeName1"),
    ApiField(17,"employerTradeName2"),
    ApiField(18,"employmentEndDate"),
    ApiField(19,"employmentStartDate"),
    ApiField(20,"flexibleDrawdownNonTaxablePayment"),
    ApiField(21,"flexibleDrawdownTaxablePayment"),
    ApiField(22,"flexiblyAccessingPensionRights"),
    ApiField(23,"forename"),
    ApiField(24,"gender"),
    ApiField(25,"grossEarningsForNICs"),
    ApiField(26,"grossEarningsForNICsYearToDate"),
    ApiField(27,"hmrcOfficeNumber"),
    ApiField(28,"hmrcReceiptTimestamp"),
    ApiField(29,"initials"),
    ApiField(30,"irregularPayment"),
    ApiField(31,"itemsSubjectToClass1NIC"),
    ApiField(32,"latePayeReportingReason"),
    ApiField(33,"monthlyPeriodNumber"),
    ApiField(34,"multipleIPSIndicator"),
    ApiField(35,"nationalInsuranceNumber"),
    ApiField(36,"niLetter"),
    ApiField(37,"numberOfNormalHoursWorked"),
    ApiField(38,"occPenIndicator"),
    ApiField(39,"onStrikeWithinPayPeriod"),
    ApiField(40,"partnerForenameOrGivenName"),
    ApiField(41,"partnerInitials"),
    ApiField(42,"partnerNINO"),
    ApiField(43,"partnerSecondForename"),
    ApiField(44,"partnerSurnameOrFamilyName"),
    ApiField(45,"payAfterStatutoryDeductions"),
    ApiField(46,"payFrequency"),
    ApiField(47,"paymentAfterLeaving"),
    ApiField(48,"paymentConfidenceStatus"),
    ApiField(49,"paymentDate"),
    ApiField(50,"paymentNoLongerValid"),
    ApiField(51,"paymentsNotSubjectToTaxOrNICS"),
    ApiField(52,"payrollId"),
    ApiField(53,"pensionContributionsNotPaidYeartoDate"),
    ApiField(54,"pensionContributionsYearToDate"),
    ApiField(55,"pensionsDeathBenefit"),
    ApiField(56,"rtiReceivedDate"),
    ApiField(57,"schemeExemptFromElectronicFiling"),
    ApiField(58,"secondForename"),
    ApiField(59,"seriousillHealthLumpSum"),
    ApiField(60,"sharedParentalPayYearToDate"),
    ApiField(61,"statutoryMaternityPayYeartoDate"),
    ApiField(62,"statutoryPaternityPayYearToDate"),
    ApiField(63,"surname"), ApiField(64,"taxCode"),
    ApiField(65,"taxCodeBasisisNonCumulative"),
    ApiField(66,"taxDeductedOrRefunded"),
    ApiField(67,"taxRegime"),
    ApiField(68,"taxWeekNumberOfAppointmentOfDirector"),
    ApiField(69,"taxYear"),
    ApiField(70,"taxYearIndicator"),
    ApiField(71,"taxablePayInPeriod"),
    ApiField(72,"taxablePayToDate"),
    ApiField(73,"totalEmployerNIContributionsInPeriod"),
    ApiField(74,"totalTaxToDate"),
    ApiField(75,"trivialCommutationPaymentTypeA"),
    ApiField(76,"trivialCommutationPaymentTypeB"),
    ApiField(77,"trivialCommutationPaymentTypeC"),
    ApiField(78,"uniqueEmploymentSequenceNumber"),
    ApiField(79,"uniquePaymentId"),
    ApiField(80,"unpaidAbsencePayPeriod"),
    ApiField(81,"valueOfDeductions"),
    ApiField(82,"valueofStatutoryAdoptionPayYearToDate"),
    ApiField(83,"weeklyPeriodNumber"))))
  
  val sgScope: Option[ApiScope] = Some(ApiScope("filter:real-time-income-information-sg",List(
    ApiField(18,"employmentEndDate"),
    ApiField(46,"payFrequency"),
    ApiField(49,"paymentDate"),
    ApiField(71,"taxablePayInPeriod"))))
}

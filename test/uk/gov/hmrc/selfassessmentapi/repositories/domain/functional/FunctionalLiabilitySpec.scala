/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.selfassessmentapi.repositories.domain.functional

import uk.gov.hmrc.selfassessmentapi.domain.Deductions
import uk.gov.hmrc.selfassessmentapi.domain.selfemployment._
import uk.gov.hmrc.selfassessmentapi.domain.unearnedincome.SavingsIncomeType._
import uk.gov.hmrc.selfassessmentapi.repositories.domain._
import uk.gov.hmrc.selfassessmentapi.services.live.calculation.steps.SelfAssessment
import uk.gov.hmrc.selfassessmentapi.{SelfEmploymentSugar, UnitSpec, domain}

class FunctionalLiabilitySpec extends UnitSpec with SelfEmploymentSugar {

  private val selfEmploymentId = "selfEmploymentId"

  "Taxable Profit from self employment" should {
    "be equal to the sum of all incomes, balancingCharges, goodsAndServices and basisAdjustment, accountingAdjustment and averagingAdjustment" in {

      val selfEmployment =
        aSelfEmployment(selfEmploymentId).copy(
          incomes = Seq(
            income(IncomeType.Turnover, 1200.01),
            income(IncomeType.Other, 799.99)
          ),
          balancingCharges = Seq(
            balancingCharge(BalancingChargeType.BPRA, 10),
            balancingCharge(BalancingChargeType.Other, 20)
          ),
          goodsAndServicesOwnUse = Seq(
            goodsAndServices(50)
          ),
          adjustments = Some(Adjustments(
            basisAdjustment = Some(200),
            accountingAdjustment = Some(100),
            averagingAdjustment = Some(50)
          ))
        )

      TaxableProfitFromSelfEmployment(selfEmployment) shouldBe BigDecimal(2430)

    }

    "be equal to incomes and outstandingBusinessIncome" in {

      val selfEmployment =
        aSelfEmployment(selfEmploymentId).copy(
          incomes = Seq(
            income(IncomeType.Turnover, 2000)
          ),
          adjustments = Some(Adjustments(
            outstandingBusinessIncome = Some(3000)
          ))
        )

      TaxableProfitFromSelfEmployment(selfEmployment) shouldBe BigDecimal(5000)

    }

    "not contain any expenses apart from depreciation" in {

      val selfEmployment =
        aSelfEmployment(selfEmploymentId).copy(
          incomes = Seq(
            income(IncomeType.Turnover, 2000)
          ),
          expenses = Seq(
            expense(ExpenseType.AdminCosts, 100),
            expense(ExpenseType.BadDebt, 50.01),
            expense(ExpenseType.CISPayments, 49.99),
            expense(ExpenseType.Depreciation, 1000000)
          )
        )

      TaxableProfitFromSelfEmployment(selfEmployment) shouldBe BigDecimal(1800)
    }

    "subtract all allowances from profit" in {

      val selfEmployment =
        aSelfEmployment(selfEmploymentId).copy(
          incomes = Seq(
            income(IncomeType.Turnover, 2000)
          ),
          allowances = Some(Allowances(
            annualInvestmentAllowance = Some(50),
            capitalAllowanceMainPool = Some(10),
            capitalAllowanceSpecialRatePool = Some(10),
            restrictedCapitalAllowance = Some(10),
            businessPremisesRenovationAllowance = Some(10),
            enhancedCapitalAllowance = Some(4.99),
            allowancesOnSales = Some(5.01)
          ))
        )

      TaxableProfitFromSelfEmployment(selfEmployment) shouldBe BigDecimal(1900)

    }

    /*"be rounded down to the nearest pound" in {

      val selfEmployment =
        aSelfEmployment(selfEmploymentId).copy(
          incomes = Seq(
            income(IncomeType.Turnover, 1.99)
          ),
          allowances = Some(Allowances(
            annualInvestmentAllowance = Some(0.02)
          ))
        )

      TaxableProfitFromSelfEmployment(selfEmployment) shouldBe BigDecimal(1298)
    }*/

    "subtract adjustments from profit" in {

      val selfEmployment =
        aSelfEmployment(selfEmploymentId).copy(
          incomes = Seq(
            income(IncomeType.Turnover, 2000)
          ),
          adjustments = Some(Adjustments(
            includedNonTaxableProfits = Some(50),
            basisAdjustment = Some(-15),
            overlapReliefUsed = Some(10),
            averagingAdjustment = Some(-25)
          ))
        )

      TaxableProfitFromSelfEmployment(selfEmployment) shouldBe BigDecimal(1900)

    }

    "reduce cap annualInvestmentAllowance at 200000" in {

      val selfEmployment =
        aSelfEmployment(selfEmploymentId).copy(
          incomes = Seq(
            income(IncomeType.Turnover, 230000)
          ),
          allowances = Some(Allowances(
            annualInvestmentAllowance = Some(230000)
          ))
        )

      TaxableProfitFromSelfEmployment(selfEmployment) shouldBe BigDecimal(30000)

    }

    "be reduced by lossBroughtForward" in {

      val selfEmployment =
        aSelfEmployment(selfEmploymentId).copy(
          incomes = Seq(
            income(IncomeType.Turnover, 2000)
          ),
          adjustments = Some(Adjustments(
            lossBroughtForward = Some(1001)
          ))
        )

      TaxableProfitFromSelfEmployment(selfEmployment) shouldBe BigDecimal(999)
    }

    "return zero as taxable profit if lossBroughtForward is greater than adjusted profit" in {

      val selfEmployment =
        aSelfEmployment(selfEmploymentId).copy(
          incomes = Seq(
            income(IncomeType.Turnover, 2000)
          ),
          adjustments = Some(Adjustments(
            lossBroughtForward = Some(3000)
          ))
        )

      TaxableProfitFromSelfEmployment(selfEmployment) shouldBe BigDecimal(0)

    }

    "be zero if expenses are bigger than incomes (loss)" in {

      val selfEmployment =
        aSelfEmployment(selfEmploymentId).copy(
          incomes = Seq(
            income(IncomeType.Turnover, 2000)
          ),
          expenses = Seq(
            expense(ExpenseType.AdminCosts, 4000)
          ),
          adjustments = Some(Adjustments(
            lossBroughtForward = Some(1000)
          ))
        )

      TaxableProfitFromSelfEmployment(selfEmployment) shouldBe BigDecimal(0)
    }

  }

  "Profit from Self employments" should {
    "be equal to the sum of taxableProfit and lossBroughtForward" in {
      ProfitFromSelfEmployment(taxableProfit = 100, lossBroughtForward = 200) shouldBe 300
    }
  }

  "Personal Allowance" should {
    "be 11,000 if total taxable income is less than 100,000" in {
      PersonalAllowance(totalTaxableIncome = 99999) shouldBe 11000
    }

    "be 11,000 if total taxable income is 100,001" in {
      PersonalAllowance(totalTaxableIncome = 100001) shouldBe 11000
    }

    "be 11,000 - (TotalIncome - 100,000)/2 if total taxable income is greater than 100,000 but less than 122,000" in {
      PersonalAllowance(totalTaxableIncome = 121999) shouldBe (11000 - (121999 - 100000)/2)
      PersonalAllowance(totalTaxableIncome = 120000) shouldBe (11000 - (120000 - 100000)/2)
      PersonalAllowance(totalTaxableIncome = 110000) shouldBe (11000 - (110000 - 100000)/2)
    }

    "be 0 if total taxable income is greater than equal to 122,000" in {
      PersonalAllowance(totalTaxableIncome = 122000) shouldBe 0
      PersonalAllowance(totalTaxableIncome = 122001) shouldBe 0
      PersonalAllowance(totalTaxableIncome = 132000) shouldBe 0
    }
  }

  "Loss brought forward" should {
    "be equal self employment loss brought forward" in {
      val selfEmployment =
        aSelfEmployment(selfEmploymentId).copy(
          adjustments = Some(Adjustments(
            lossBroughtForward = Some(999)
          ))
        )

      LossBroughtForward(selfEmployment, 1000) shouldBe 999
    }

    "be capped at adjusted profits" in {
      val selfEmployment =
        aSelfEmployment(selfEmploymentId).copy(
          adjustments = Some(Adjustments(
            lossBroughtForward = Some(1001)
          ))
        )

      LossBroughtForward(selfEmployment, 1000) shouldBe 1000
    }

    "be 0 if none is provided" in {
      val selfEmployment =
        aSelfEmployment(selfEmploymentId).copy(
          adjustments = None
        )

      LossBroughtForward(selfEmployment, 1000) shouldBe 0
    }
  }

  "Interest from UK banks and building societies" should {
    def taxedInterest(amount: BigDecimal) = MongoUnearnedIncomesSavingsIncomeSummary("", InterestFromBanksTaxed, amount)
    def unTaxedInterest(amount: BigDecimal) = MongoUnearnedIncomesSavingsIncomeSummary("", InterestFromBanksUntaxed, amount)

    "calculate rounded down interest when there are multiple interest of both taxed and unTaxed from uk banks and building societies from multiple unearned income source" in {

        val unearnedIncomes1 = anUnearnedIncomes().copy(savings = Seq(taxedInterest(100.50), unTaxedInterest(200.50)))
        val unearnedIncomes2 = anUnearnedIncomes().copy(savings = Seq(taxedInterest(300.99), unTaxedInterest(400.99)))

      InterestFromUKBanksAndBuildingSocieties(SelfAssessment(unearnedIncomes = Seq(unearnedIncomes1, unearnedIncomes2))) should contain theSameElementsAs
        Seq(domain.InterestFromUKBanksAndBuildingSocieties(sourceId = unearnedIncomes1.sourceId, BigDecimal(326)),
        domain.InterestFromUKBanksAndBuildingSocieties(sourceId = unearnedIncomes2.sourceId, BigDecimal(777)))
      }

    "calculate interest when there is one taxed interest from uk banks and building societies from a single unearned income source" in {
      val unearnedIncomes = anUnearnedIncomes().copy(savings = Seq(taxedInterest(100)))

      InterestFromUKBanksAndBuildingSocieties(SelfAssessment(unearnedIncomes = Seq(unearnedIncomes))) should contain theSameElementsAs
        Seq(domain.InterestFromUKBanksAndBuildingSocieties(unearnedIncomes.sourceId, BigDecimal(125)))

    }

    "calculate interest when there are multiple taxed interest from uk banks and building societies from a single unearned income source" in {
      val unearnedIncomes = anUnearnedIncomes().copy(savings = Seq(taxedInterest(100), taxedInterest(200)))

      InterestFromUKBanksAndBuildingSocieties(SelfAssessment(unearnedIncomes = Seq(unearnedIncomes))) should contain theSameElementsAs
        Seq(domain.InterestFromUKBanksAndBuildingSocieties(unearnedIncomes.sourceId, BigDecimal(375)))

    }

    "calculate round down interest when there is one taxed interest from uk banks and building societies from a single unearned income source" in {
      val unearnedIncomes = anUnearnedIncomes().copy(savings = Seq(taxedInterest(100.50)))

      InterestFromUKBanksAndBuildingSocieties(SelfAssessment(unearnedIncomes = Seq(unearnedIncomes))) should contain theSameElementsAs
        Seq(domain.InterestFromUKBanksAndBuildingSocieties(unearnedIncomes.sourceId, BigDecimal(125)))
    }

    "calculate round down interest when there are multiple taxed interest from uk banks and building societies from a single unearned income source" in {
      val unearnedIncomes = anUnearnedIncomes().copy(savings = Seq(taxedInterest(100.90), taxedInterest(200.99)))

      InterestFromUKBanksAndBuildingSocieties(SelfAssessment(unearnedIncomes = Seq(unearnedIncomes))) should contain theSameElementsAs
        Seq(domain.InterestFromUKBanksAndBuildingSocieties(unearnedIncomes.sourceId, BigDecimal(377)))

    }

    "calculate interest when there is one unTaxed interest from uk banks and building societies from a single unearned income source" in {
      val unearnedIncomes = anUnearnedIncomes().copy(savings = Seq(unTaxedInterest(100)))

      InterestFromUKBanksAndBuildingSocieties(SelfAssessment(unearnedIncomes = Seq(unearnedIncomes))) should contain theSameElementsAs
        Seq(domain.InterestFromUKBanksAndBuildingSocieties(unearnedIncomes.sourceId, BigDecimal(100)))

    }

    "calculate interest when there are multiple unTaxed interest from uk banks and building societies from a single unearned income source" in {
      val unearnedIncomes = anUnearnedIncomes().copy(savings = Seq(unTaxedInterest(100), unTaxedInterest(200)))

      InterestFromUKBanksAndBuildingSocieties(SelfAssessment(unearnedIncomes = Seq(unearnedIncomes))) should contain theSameElementsAs
        Seq(domain.InterestFromUKBanksAndBuildingSocieties(unearnedIncomes.sourceId, BigDecimal(300)))
    }


    "calculate rounded down interest when there is one unTaxed interest from uk banks and building societies from a single unearned income source" in {
      val unearnedIncomes = anUnearnedIncomes().copy(savings = Seq(unTaxedInterest(100.50)))

      InterestFromUKBanksAndBuildingSocieties(SelfAssessment(unearnedIncomes = Seq(unearnedIncomes))) should contain theSameElementsAs
        Seq(domain.InterestFromUKBanksAndBuildingSocieties(unearnedIncomes.sourceId, BigDecimal(100)))
    }

    "calculate rounded down interest when there are multiple unTaxed interest from uk banks and building societies from a single unearned income source" in {
      val unearnedIncomes = anUnearnedIncomes().copy(savings = Seq(unTaxedInterest(100.50), unTaxedInterest(200.99)))

      InterestFromUKBanksAndBuildingSocieties(SelfAssessment(unearnedIncomes = Seq(unearnedIncomes))) should contain theSameElementsAs
        Seq(domain.InterestFromUKBanksAndBuildingSocieties(unearnedIncomes.sourceId, BigDecimal(301)))

    }

  }

  "total income" should {
    "calculate total income" in {

      val selfEmploymentOne = aSelfEmployment(selfEmploymentId).copy(incomes = Seq(income(IncomeType.Turnover, 200)))

      TotalIncomeReceived(SelfAssessment(selfEmployments = Seq(selfEmploymentOne)),
        Seq(domain.InterestFromUKBanksAndBuildingSocieties("ue1", 100),
        domain.InterestFromUKBanksAndBuildingSocieties("ue2", 150)
      ), Seq(domain.DividendsFromUKSources("dividend1", 1000),
        domain.DividendsFromUKSources("dividend2", 2000)
      )) shouldBe 3450
    }

    "calculate total income if there is no income from self employments" in {
      TotalIncomeReceived(SelfAssessment(selfEmployments = Seq()),Seq(), Seq()) shouldBe 0
    }

    "calculate total income if there is no income from self employments but has interest from UK banks and building societies" in {

      TotalIncomeReceived(SelfAssessment(selfEmployments = Seq()),
        Seq(domain.InterestFromUKBanksAndBuildingSocieties("ue1", 100),
          domain.InterestFromUKBanksAndBuildingSocieties("ue2", 150)
        ), Seq()) shouldBe 250
    }


    "calculate total income if there is no income from self employments but has dividends from unearned income" in {

      TotalIncomeReceived(SelfAssessment(selfEmployments = Seq()),
        Seq(), Seq(domain.DividendsFromUKSources("dividend1", 1000),
          domain.DividendsFromUKSources("dividend2", 2000)
        )) shouldBe 3000

    }
  }

  "Total deduction" should {
    "be sum of income tax relief and personal allowance" in {
      TotalDeduction(1000.00, 10000.00) shouldBe 11000
    }
  }

  "Income tax Relief" should {
    "rounded up sum of all self employments loss brought forward values" in {

      val selfEmploymentOne =
        aSelfEmployment(selfEmploymentId).copy(
          incomes = Seq(income(IncomeType.Turnover, 2000)),
          adjustments = Some(Adjustments(lossBroughtForward = Some(200.25)))
        )

      val selfEmploymentTwo =
        aSelfEmployment(selfEmploymentId).copy(
          incomes = Seq(income(IncomeType.Turnover, 2000)),
          adjustments = Some(Adjustments(lossBroughtForward = Some(100.34)))
        )

      IncomeTaxRelief(SelfAssessment(selfEmployments = Seq(selfEmploymentOne, selfEmploymentTwo))) shouldBe 301

    }
  }

  "Total incomes on which tax is due" should {
    "be totalIncomeReceived - totalDeduction" in {
      TotalIncomeOnWhichTaxIsDue(100, 50) shouldBe 50
    }

    "zero if totalIncomeReceived is less than totalDeductions" in {
      TotalIncomeOnWhichTaxIsDue(50, 100) shouldBe 0
    }

  }

}

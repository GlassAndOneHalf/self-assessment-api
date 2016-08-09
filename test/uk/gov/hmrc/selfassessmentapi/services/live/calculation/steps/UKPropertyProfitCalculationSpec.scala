package uk.gov.hmrc.selfassessmentapi.services.live.calculation.steps

import org.joda.time.{DateTime, DateTimeZone}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.selfassessmentapi.{SelfEmploymentSugar, UnitSpec}
import uk.gov.hmrc.selfassessmentapi.domain.{SourceId, TaxYear}
import uk.gov.hmrc.selfassessmentapi.domain.ukproperty.SourceType.UKProperties
import uk.gov.hmrc.selfassessmentapi.domain.ukproperty.{Allowances, ExpenseType, IncomeType, UKProperty}
import uk.gov.hmrc.selfassessmentapi.repositories.domain.{MongoUKProperties, MongoUKPropertiesExpenseSummary, MongoUKPropertiesIncomeSummary}

/**
  * Created by JacobTaylorHindle on 09/08/2016.
  */
class UKPropertyProfitCalculationSpec extends UnitSpec with SelfEmploymentSugar {
  private val liability = aLiability()

  def aUkProperty(id: SourceId): MongoUKProperties = {
    val ukp = MongoUKProperties(BSONObjectID.generate, id, generateSaUtr(), taxYear)
    val incomes = Seq(MongoUKPropertiesIncomeSummary("", IncomeType.RentIncome, 500),
      MongoUKPropertiesIncomeSummary("", IncomeType.PremiumsOfLeaseGrant, 500),
      MongoUKPropertiesIncomeSummary("", IncomeType.ReversePremiums, 500))

    val balancingCharge = Seq()
    val expenses = Seq(MongoUKPropertiesExpenseSummary("", ExpenseType.PremisesRunningCosts, 500),
      MongoUKPropertiesExpenseSummary("", ExpenseType.RepairsAndMaintenance, 500),
      MongoUKPropertiesExpenseSummary("", ExpenseType.FinancialCosts, 500),
      MongoUKPropertiesExpenseSummary("", ExpenseType.ProfessionalFees, 500),
      MongoUKPropertiesExpenseSummary("", ExpenseType.CostOfServices, 500),
      MongoUKPropertiesExpenseSummary("", ExpenseType.Other, 500))

    val allowances = Seq(Allowances())

    ukp.copy(incomes = incomes, expenses = expenses)
  }

  "run" should {
    val sa = SelfAssessment(ukProperties = Seq(MongoUKProperties()))

    "foobar" in {
      UKPropertyProfitCalculation.run(sa, liability)

    }
  }
}

package com.eqh.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payout_payee", schema = "public")
public class PayoutPayee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "banking_number", length = 15)
    private String bankingNumber;

    @Column(name = "capital_gains", precision = 17, scale = 2)
    private BigDecimal capitalGains;

    @Column(name = "combine_payouts", nullable = false)
    private Boolean combinePayouts;

    @Column(name = "disburse_to_payee", nullable = false)
    private Boolean disburseToPayee;

    @Column(name = "distribution_code", length = 10)
    private String distributionCode;

    @Column(name = "distribution_pct", precision = 9, scale = 3)
    private BigDecimal distributionPct;

    @Column(name = "do_not_withhold_for_federal")
    private Boolean doNotWithholdForFederal;

    @Column(name = "do_not_withhold_for_state")
    private Boolean doNotWithholdForState;

    @Column(name = "due_and_unpaid_amt", precision = 17, scale = 2)
    private BigDecimal dueAndUnpaidAmt;

    @Column(name = "federal_deduction_other_than_standard", precision = 17, scale = 2)
    private BigDecimal federalDeductionOtherThanStandard;

    @Column(name = "federal_dependents_other_credit_amt", precision = 17, scale = 2)
    private BigDecimal federalDependentsOtherCreditAmt;

    @Column(name = "federal_other_income_pension_amt", precision = 17, scale = 2)
    private BigDecimal federalOtherIncomePensionAmt;

    @Column(name = "federal_other_than_job_pension_income_amt", precision = 17, scale = 2)
    private BigDecimal federalOtherThanJobPensionIncomeAmt;

    @Column(name = "federal_standard_deduction_ind")
    private Boolean federalStandardDeductionInd;

    @Column(name = "federal_withholding_additional_amt", precision = 17, scale = 2)
    private BigDecimal federalWithholdingAdditionalAmt;

    @Column(name = "federal_withholding_override_amt", precision = 17, scale = 2)
    private BigDecimal federalWithholdingOverrideAmt;

    @Column(name = "garnishment_memo", length = 40)
    private String garnishmentMemo;

    @Column(name = "inhibit_tax_reporting", nullable = false)
    private Boolean inhibitTaxReporting;

    @Column(name = "legal_representative", length = 10)
    private String legalRepresentative;

    @Column(name = "marital_filing_status_federal", length = 10)
    private String maritalFilingStatusFederal;

    @Column(name = "marital_filing_status_state", length = 10)
    private String maritalFilingStatusState;

    @Column(name = "number_of_federal_primary_exemptions")
    private Short numberOfFederalPrimaryExemptions;

    @Column(name = "number_of_state_blind_exemptions")
    private Short numberOfStateBlindExemptions;

    @Column(name = "number_of_state_pensioner_exemptions")
    private Short numberOfStatePensionerExemptions;

    @Column(name = "number_of_state_primary_exemptions")
    private Short numberOfStatePrimaryExemptions;

    @Column(name = "number_of_state_secondary_exemptions")
    private Short numberOfStateSecondaryExemptions;

    @Column(name = "payable_to1party_number", length = 15)
    private String payableTo1PartyNumber;

    @Column(name = "payable_to2party_number", length = 15)
    private String payableTo2PartyNumber;

    @Column(name = "payee_amt", precision = 17, scale = 2)
    private BigDecimal payeeAmt;

    @Column(name = "payee_amt_type", length = 10)
    private String payeeAmtType;

    @Column(name = "payee_is_annuitant", nullable = false)
    private Boolean payeeIsAnnuitant;

    @Column(name = "payee_party_number", length = 15, nullable = false)
    private String payeePartyNumber;

    @Column(name = "payee_pct", precision = 9, scale = 3)
    private BigDecimal payeePct;

    @Column(name = "payee_status", length = 10)
    private String payeeStatus;

    @Column(name = "payment_form", length = 10)
    private String paymentForm;

    @Column(name = "policy_payout_id")
    private Long policyPayoutId;

    @Column(name = "reason_code", length = 10)
    private String reasonCode;

    @Column(name = "state_withholding_additional_amt", precision = 17, scale = 2)
    private BigDecimal stateWithholdingAdditionalAmt;

    @Column(name = "state_withholding_override_amt", precision = 17, scale = 2)
    private BigDecimal stateWithholdingOverrideAmt;

    @Column(name = "tax_disbursement_type", length = 10)
    private String taxDisbursementType;

    @Column(name = "tax_form", length = 10)
    private String taxForm;

    @Column(name = "taxable_to", length = 10)
    private String taxableTo;

    @Column(name = "update_timestamp", nullable = false)
    private LocalDateTime updateTimestamp;

    @Column(name = "wager_type", length = 10)
    private String wagerType;

    @Column(name = "wager_won_date")
    private LocalDate wagerWonDate;
}


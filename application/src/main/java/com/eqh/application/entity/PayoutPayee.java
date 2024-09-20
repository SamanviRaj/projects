package com.eqh.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PAYOUT_PAYEE", schema = "public")
public class PayoutPayee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "banking_number")
    private String bankingNumber;

    @Column(name = "capital_gains")
    private BigDecimal capitalGains;

    @Column(name = "combine_payouts")
    private Boolean combinePayouts;

    @Column(name = "disburse_to_payee")
    private Boolean disburseToPayee;

    @Column(name = "distribution_code")
    private String distributionCode;

    @Column(name = "distribution_pct")
    private BigDecimal distributionPct;

    @Column(name = "do_not_withhold_for_state")
    private Boolean doNotWithholdForState;

    @Column(name = "due_and_unpaid_amt")
    private BigDecimal dueAndUnpaidAmt;

    @Column(name = "federal_withholding_additional_amt")
    private BigDecimal federalWithholdingAdditionalAmt;

    @Column(name = "federal_withholding_override_amt")
    private BigDecimal federalWithholdingOverrideAmt;

    @Column(name = "inhibit_tax_reporting")
    private Boolean inhibitTaxReporting;

    @Column(name = "legal_representative")
    private String legalRepresentative;

    @Column(name = "marital_filing_status_federal")
    private String maritalFilingStatusFederal;

    @Column(name = "marital_filing_status_state")
    private String maritalFilingStatusState;

    @Column(name = "number_of_federal_primary_exemptions")
    private Integer numberOfFederalPrimaryExemptions;

    @Column(name = "number_of_state_blind_exemptions")
    private Integer numberOfStateBlindExemptions;

    @Column(name = "number_of_state_pensioner_exemptions")
    private Integer numberOfStatePensionerExemptions;

    @Column(name = "number_of_state_primary_exemptions")
    private Integer numberOfStatePrimaryExemptions;

    @Column(name = "number_of_state_secondary_exemptions")
    private Integer numberOfStateSecondaryExemptions;

    @Column(name = "payable_to1party_number")
    private String payableTo1partyNumber;

    @Column(name = "payable_to2party_number")
    private String payableTo2partyNumber;

    @Column(name = "payee_amt")
    private BigDecimal payeeAmt;

    @Column(name = "payee_amt_type")
    private String payeeAmtType;

    @Column(name = "payee_is_annuitant")
    private Boolean payeeIsAnnuitant;

    @Column(name = "payee_party_number", nullable = false)
    private String payeePartyNumber;

    @Column(name = "payee_pct")
    private BigDecimal payeePct;

    @Column(name = "payee_status")
    private String payeeStatus;

    @Column(name = "payment_form")
    private String paymentForm;

    @Column(name = "state_withholding_additional_amt")
    private BigDecimal stateWithholdingAdditionalAmt;

    @Column(name = "state_withholding_override_amt")
    private BigDecimal stateWithholdingOverrideAmt;

    @Column(name = "tax_disbursement_type")
    private String taxDisbursementType;

    @Column(name = "tax_form")
    private String taxForm;

    @Column(name = "taxable_to")
    private String taxableTo;

    @Column(name = "wager_type")
    private String wagerType;

    @Column(name = "wager_won_date")
    private LocalDate wagerWonDate;

    @Column(name = "policy_payout_id")
    private Long policyPayoutId;

    @Column(name = "update_timestamp", nullable = false)
    private LocalDateTime updateTimestamp;

    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "garnishment_memo")
    private String garnishmentMemo;

    @Column(name = "do_not_withhold_for_federal")
    private Boolean doNotWithholdForFederal;

    @Column(name = "federal_deduction_other_than_standard")
    private BigDecimal federalDeductionOtherThanStandard;

    @Column(name = "federal_dependents_other_credit_amt")
    private BigDecimal federalDependentsOtherCreditAmt;

    @Column(name = "federal_other_income_pension_amt")
    private BigDecimal federalOtherIncomePensionAmt;

    @Column(name = "federal_other_than_job_pension_income_amt")
    private BigDecimal federalOtherThanJobPensionIncomeAmt;

    @Column(name = "federal_standard_deduction_ind")
    private Boolean federalStandardDeductionInd;
}

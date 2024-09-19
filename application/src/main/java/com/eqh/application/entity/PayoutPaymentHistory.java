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
@Table(name = "payout_payment_history", schema = "public")
public class PayoutPaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "accumulated_interest_earned", precision = 17, scale = 2)
    private BigDecimal accumulatedInterestEarned;

    @Column(name = "accumulated_interest_paid", precision = 17, scale = 2)
    private BigDecimal accumulatedInterestPaid;

    @Column(name = "bank_acct_number", length = 20)
    private String bankAcctNumber;

    @Column(name = "bank_acct_type", length = 10)
    private String bankAcctType;

    @Column(name = "bank_branch_name", length = 50)
    private String bankBranchName;

    @Column(name = "bank_name", length = 60)
    private String bankName;

    @Column(name = "bank_routing_number", length = 9)
    private String bankRoutingNumber;

    @Column(name = "banking_number", length = 15)
    private String bankingNumber;

    @Column(name = "combine_payouts", nullable = false)
    private Boolean combinePayouts;

    @Column(name = "disburse_to_payee", nullable = false)
    private Boolean disburseToPayee;

    @Column(name = "do_not_withhold_federal", nullable = false)
    private Boolean doNotWithholdFederal;

    @Column(name = "do_not_withhold_state", nullable = false)
    private Boolean doNotWithholdState;

    @Column(name = "encrypted_account_number", length = 75)
    private String encryptedAccountNumber;

    @Column(name = "excess_interest_accumulated", precision = 17, scale = 2)
    private BigDecimal excessInterestAccumulated;

    @Column(name = "excess_interest_earned", precision = 17, scale = 2)
    private BigDecimal excessInterestEarned;

    @Column(name = "excess_interest_method", length = 10)
    private String excessInterestMethod;

    @Column(name = "excess_interest_paid", precision = 17, scale = 2)
    private BigDecimal excessInterestPaid;

    @Column(name = "federal_deduction_other_than_standard", precision = 17, scale = 2)
    private BigDecimal federalDeductionOtherThanStandard;

    @Column(name = "federal_dependents_other_credit_amt", precision = 17, scale = 2)
    private BigDecimal federalDependentsOtherCreditAmt;

    @Column(name = "federal_non_taxable_amt", precision = 17, scale = 2)
    private BigDecimal federalNonTaxableAmt;

    @Column(name = "federal_other_income_pension_amt", precision = 17, scale = 2)
    private BigDecimal federalOtherIncomePensionAmt;

    @Column(name = "federal_other_than_job_pension_income_amt", precision = 17, scale = 2)
    private BigDecimal federalOtherThanJobPensionIncomeAmt;

    @Column(name = "federal_standard_deduction_ind")
    private Boolean federalStandardDeductionInd;

    @Column(name = "federal_taxable_amt", precision = 17, scale = 2)
    private BigDecimal federalTaxableAmt;

    @Column(name = "federal_withholding_additional_amt", precision = 17, scale = 2)
    private BigDecimal federalWithholdingAdditionalAmt;

    @Column(name = "federal_withholding_override_amt", precision = 17, scale = 2)
    private BigDecimal federalWithholdingOverrideAmt;

    @Column(name = "federal_withholding_override_pct", precision = 9, scale = 3)
    private BigDecimal federalWithholdingOverridePct;

    @Column(name = "fin_activity_type", length = 10)
    private String finActivityType;

    @Column(name = "garnishment_memo", length = 40)
    private String garnishmentMemo;

    @Column(name = "gross_amt", precision = 17, scale = 2)
    private BigDecimal grossAmt;

    @Column(name = "guaranteed_floor_amt", precision = 17, scale = 2)
    private BigDecimal guaranteedFloorAmt;

    @Column(name = "guaranteed_interest_accumulated", precision = 17, scale = 2)
    private BigDecimal guaranteedInterestAccumulated;

    @Column(name = "guaranteed_interest_earned", precision = 17, scale = 2)
    private BigDecimal guaranteedInterestEarned;

    @Column(name = "guaranteed_interested_method", length = 10)
    private String guaranteedInterestedMethod;

    @Column(name = "guaranteed_interest_paid", precision = 17, scale = 2)
    private BigDecimal guaranteedInterestPaid;

    @Column(name = "inhibit_tax_reporting", nullable = false)
    private Boolean inhibitTaxReporting;

    @Column(name = "is_adjusted", nullable = false)
    private Boolean isAdjusted;

    @Column(name = "is_reprocess")
    private Boolean isReprocess;

    @Column(name = "legal_representative", length = 10)
    private String legalRepresentative;

    @Column(name = "line2", length = 50)
    private String line2;

    @Column(name = "marital_filing_status_federal", length = 10)
    private String maritalFilingStatusFederal;

    @Column(name = "marital_filing_status_state", length = 10)
    private String maritalFilingStatusState;

    @Column(name = "net_amt", precision = 17, scale = 2)
    private BigDecimal netAmt;

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

    @Column(name = "payable1address", length = 60)
    private String payable1Address;

    @Column(name = "payable1address_line2", length = 60)
    private String payable1AddressLine2;

    @Column(name = "payable1address_line3", length = 60)
    private String payable1AddressLine3;

    @Column(name = "payable1address_line4", length = 60)
    private String payable1AddressLine4;

    @Column(name = "payable1address_line5", length = 60)
    private String payable1AddressLine5;

    @Column(name = "payable1city", length = 30)
    private String payable1City;

    @Column(name = "payable1country", length = 10)
    private String payable1Country;

    @Column(name = "payable1name", length = 150)
    private String payable1Name;

    @Column(name = "payable1state", length = 10)
    private String payable1State;

    @Column(name = "payable1zip", length = 10)
    private String payable1Zip;

    @Column(name = "payable2address", length = 60)
    private String payable2Address;

    @Column(name = "payable2address_line2", length = 60)
    private String payable2AddressLine2;

    @Column(name = "payable2address_line3", length = 60)
    private String payable2AddressLine3;

    @Column(name = "payable2address_line4", length = 60)
    private String payable2AddressLine4;

    @Column(name = "payable2address_line5", length = 60)
    private String payable2AddressLine5;

    @Column(name = "payable2city", length = 30)
    private String payable2City;

    @Column(name = "payable2country", length = 10)
    private String payable2Country;

    @Column(name = "payable2name", length = 150)
    private String payable2Name;

    @Column(name = "payable2state", length = 10)
    private String payable2State;

    @Column(name = "payable2zip", length = 10)
    private String payable2Zip;

    @Column(name = "payee_full_name", length = 150)
    private String payeeFullName;

    @Column(name = "payee_party_number", length = 15)
    private String payeePartyNumber;

    @Column(name = "payee_status", length = 10)
    private String payeeStatus;

    @Column(name = "payment_form", length = 10)
    private String paymentForm;

    @Column(name = "payment_history_seq", nullable = false)
    private Long paymentHistorySeq;

    @Column(name = "payout_due_date")
    private LocalDate payoutDueDate;

    @Column(name = "payout_extract_date")
    private LocalDate payoutExtractDate;

    @Column(name = "payout_payee_id")
    private Long payoutPayeeId;

    @Column(name = "reason_code", length = 10)
    private String reasonCode;

    @Column(name = "reprocessed_seq", length = 20)
    private String reprocessedSeq;

    @Column(name = "reversed", nullable = false)
    private Boolean reversed;

    @Column(name = "settlement_interest_paid", precision = 17, scale = 2)
    private BigDecimal settlementInterestPaid;

    @Column(name = "state_withholding_additional_amt", precision = 17, scale = 2)
    private BigDecimal stateWithholdingAdditionalAmt;

    @Column(name = "state_withholding_override_amt", precision = 17, scale = 2)
    private BigDecimal stateWithholdingOverrideAmt;

    @Column(name = "state_withholding_override_pct", precision = 9, scale = 3)
    private BigDecimal stateWithholdingOverridePct;

    @Column(name = "surrender_charge_amt", precision = 17, scale = 2)
    private BigDecimal surrenderChargeAmt;

    @Column(name = "tax_disbursement_type", length = 10)
    private String taxDisbursementType;

    @Column(name = "tax_form", length = 10)
    private String taxForm;

    @Column(name = "taxable_party_name", length = 150)
    private String taxablePartyName;

    @Column(name = "taxable_party_number", length = 15)
    private String taxablePartyNumber;

    @Column(name = "taxable_to", length = 10)
    private String taxableTo;

    @Column(name = "taxable_to_fatca_reportable_status", length = 10)
    private String taxableToFatcaReportableStatus;

    @Column(name = "taxable_to_govtid", length = 9)
    private String taxableToGovtId;

    @Column(name = "taxable_to_govtidstat", length = 10)
    private String taxableToGovtIdStat;

    @Column(name = "taxable_to_govt_idtc", length = 10)
    private String taxableToGovtIdTc;

    @Column(name = "taxable_to_residence_country", length = 10)
    private String taxableToResidenceCountry;

    @Column(name = "taxable_to_residence_county", length = 10)
    private String taxableToResidenceCounty;

    @Column(name = "taxable_to_residence_state", length = 10)
    private String taxableToResidenceState;

    @Column(name = "total_distribution_ind", length = 10)
    private String totalDistributionInd;

    @Column(name = "trans_eff_date")
    private LocalDate transEffDate;

    @Column(name = "trans_exe_date")
    private LocalDate transExeDate;

    @Column(name = "trans_run_date")
    private LocalDate transRunDate;

    @Column(name = "update_timestamp", nullable = false)
    private LocalDateTime updateTimestamp;

    @Column(name = "zip", length = 50)
    private String zip;
}


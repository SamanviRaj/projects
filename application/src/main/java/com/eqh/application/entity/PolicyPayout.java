package com.eqh.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "POLICY_PAYOUT", schema = "public")
public class PolicyPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payee_share_method", length = 10)
    private String payeeShareMethod;

    @Column(name = "payout_due_day1")
    private Short payoutDueDay1;

    @Column(name = "payout_due_day2")
    private Short payoutDueDay2;

    @Column(name = "payout_mode", length = 10)
    private String payoutMode;

    @Column(name = "payout_status", length = 10)
    private String payoutStatus;

    @Column(name = "period_certain_mode", length = 10)
    private String periodCertainMode;

    @Column(name = "period_certain_periods")
    private Short periodCertainPeriods;

    @Column(name = "primary_reduction_pct", precision = 9, scale = 3)
    private BigDecimal primaryReductionPct;

    @Column(name = "prior_payout_status", length = 10)
    private String priorPayoutStatus;

    @Column(name = "product_description", length = 40)
    private String productDescription;

    @Column(name = "retirement_age")
    private Short retirementAge;

    @Column(name = "secondary_reduction_pct", precision = 9, scale = 3)
    private BigDecimal secondaryReductionPct;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "tax_code", length = 10)
    private String taxCode;

    @Column(name = "taxable_amount_not_determined", nullable = false)
    private Boolean taxableAmountNotDetermined;

    @Column(name = "total_payout_overpayment", precision = 17, scale = 2)
    private BigDecimal totalPayoutOverpayment;

    @Column(name = "contract_continuation_ind", nullable = false)
    private Boolean contractContinuationInd;

    @Column(name = "advance_payout_type", length = 10)
    private String advancePayoutType;

    @Column(name = "policy_id")
    private Long policyId;

    // Additional foreign key mappings can be added as needed
}


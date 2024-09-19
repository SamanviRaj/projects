package com.eqh.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payout_payment_history_deduction", schema = "public")
public class PayoutPaymentHistoryDeduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fee_amt", precision = 17, scale = 2)
    private BigDecimal feeAmt;

    @Column(name = "fee_type", length = 10)
    private String feeType;

    @Column(name = "payout_payment_history_id")
    private Long payoutPaymentHistoryId;

    @Column(name = "update_timestamp", nullable = false)
    private LocalDateTime updateTimestamp;
}


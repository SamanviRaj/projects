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
@Table(name = "payout_payment_history_adjustment", schema = "public")
public class PayoutPaymentHistoryAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "adjustment_fin_act_type", length = 10)
    private String adjustmentFinActType;

    @Column(name = "adjustment_type", length = 10)
    private String adjustmentType;

    @Column(name = "adjustment_value", precision = 17, scale = 2)
    private BigDecimal adjustmentValue;

    @Column(name = "field_adjustment", length = 10)
    private String fieldAdjustment;

    @Column(name = "payout_payment_history_id")
    private Long payoutPaymentHistoryId;

    @Column(name = "remarks", length = 200)
    private String remarks;

    @Column(name = "update_timestamp", nullable = false)
    private LocalDateTime updateTimestamp;
}


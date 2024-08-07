package com.eqh.application.entity;

import jakarta.persistence.*;
import lombok.*;


import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

@Entity
@Data
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Table(name = "TRANSACTION_HISTORY")
public class TransactionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "parent")
    private Boolean parent;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "file_control_id")
    private String fileControlId;

    @Column(name = "fin_activity_type")
    private String finActivityType;

    @Column(name = "gross_amt")
    private BigDecimal grossAmt;

    @Column(name = "internal")
    private Boolean internal;

    @Column(name = "net_amt")
    private BigDecimal netAmt;

    @Column(name = "prime_event_id")
    private Long primeEventId;

    @Column(name = "request_name")
    private String requestName;

    @Column(name = "request_type")
    private String requestType;

    @Column(name = "reversed")
    private Boolean reversed;

    @Column(name = "trans_eff_date")
    private Date transEffDate;

    @Column(name = "trans_exe_date")
    private Timestamp transExeDate;

    @Column(name = "trans_run_date")
    private Date transRunDate;

    @Column(name = "trans_seq")
    private Short transSeq;

    @Column(name = "version")
    private Long version;

    @Column(name = "message_image")
    private String messageImage;

    @Column(name = "update_timestamp")
    private Timestamp updateTimestamp;

    @Column(name = "reapplied")
    private Boolean reapplied;

    @Column(name = "role_consumer")
    private Boolean roleConsumer;

    // Getters and Setters
}


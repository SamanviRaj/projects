package com.eqh.party.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ADDRESS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "address_bar_code_ind", nullable = false)
    private Boolean addressBarCodeInd;

    @Column(name = "address_countrytc", length = 10)
    private String addressCountrytc;

    @Column(name = "address_countytc", length = 10)
    private String addressCountytc;

    @Column(name = "address_formattc", length = 10)
    private String addressFormattc;

    @Column(name = "address_statetc", length = 10)
    private String addressStatetc;

    @Column(name = "address_type_code", length = 10, nullable = false)
    private String addressTypeCode;

    @Column(name = "address_validation_date")
    private LocalDate addressValidationDate;

    @Column(name = "address_valid_ind", nullable = false)
    private Boolean addressValidInd;

    @Column(name = "attention_line", length = 30)
    private String attentionLine;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "foreign_address_ind", nullable = false)
    private Boolean foreignAddressInd;

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "legal_address_ind", nullable = false)
    private Boolean legalAddressInd;

    @Column(name = "line1", length = 50, nullable = false)
    private String line1;

    @Column(name = "line2", length = 50)
    private String line2;

    @Column(name = "line3", length = 50)
    private String line3;

    @Column(name = "line4", length = 50)
    private String line4;

    @Column(name = "line5", length = 50)
    private String line5;

    @Column(name = "postal_drop_code", length = 20)
    private String postalDropCode;

    @Column(name = "pref_addr", nullable = false)
    private Boolean prefAddr;

    @Column(name = "prevent_override_ind", nullable = false)
    private Boolean preventOverrideInd;

    @Column(name = "recurring_end_mo_day", length = 4)
    private String recurringEndMoDay;

    @Column(name = "recurring_start_mo_day", length = 4)
    private String recurringStartMoDay;

    @Column(name = "returned_mail_ind", nullable = false)
    private Boolean returnedMailInd;

    @Column(name = "returned_mail_reason", length = 30)
    private String returnedMailReason;

    @Column(name = "returned_mail_start_date")
    private LocalDate returnedMailStartDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "zip", length = 50, nullable = false)
    private String zip;

    @Column(name = "address_state_others", length = 50)
    private String addressStateOthers;

    @Column(name = "party_id")
    private Long partyId;

    @Column(name = "update_timestamp", nullable = false)
    private LocalDateTime updateTimestamp;
}

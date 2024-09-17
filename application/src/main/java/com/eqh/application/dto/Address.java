package com.eqh.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Address {

    private Long id;

    private Boolean addressBarCodeInd;

    private String addressCountrytc;

    private String addressCountytc;

    private String addressFormattc;

    private String addressStatetc;

    private String addressTypeCode;

    private LocalDate addressValidationDate;

    private Boolean addressValidInd;

    private String attentionLine;

    private String city;

    private LocalDate endDate;

    private Boolean foreignAddressInd;

    private String language;

    private Boolean legalAddressInd;

    private String line1;

    private String line2;

    private String line3;

    private String line4;

    private String line5;

    private String postalDropCode;

    private Boolean prefAddr;

    private Boolean preventOverrideInd;

    private String recurringEndMoDay;

    private String recurringStartMoDay;

    private Boolean returnedMailInd;

    private String returnedMailReason;

    private LocalDate returnedMailStartDate;

    private LocalDate startDate;

    private String zip;

    private String addressStateOthers;

    private Long partyId;

    private LocalDateTime updateTimestamp;
}

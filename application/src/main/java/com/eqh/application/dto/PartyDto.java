package com.eqh.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PartyDto {
    private Long id;
    private String currencyTypeCode;
    private String effDate;
    private String fatcaReportableStatus;
    private Boolean fitbitAuthInd;
    private String govtid;
    private String govtidstat;
    private String govtIdtc;
    private String hoCreateDate;
    private String hoExpiryDate;
    private Boolean humanApiAuthInd;
    private String partyNumber;
    private String partyTypeCode;
    private String prefComm;
    private String residenceCountry;
    private String residenceCounty;
    private String residenceState;
    private String residenceTaxLocality;
    private String userId;
    private String updateTimestamp;
    private Boolean fraudLockInd;
    private Boolean fraudAlertInd;
    private String fraudStatus;

    // Getters and setters
}


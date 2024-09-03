package com.eqh.application.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Party {

    private Long id;

    private String currencyTypeCode;

    private Date effDate;

    private String fatcaReportableStatus;

    private Boolean fitbitAuthInd;

    private String govtId;

    private String govtIdStat;

    private String govtIdtc;

    private Date hoCreateDate;

    private Date hoExpiryDate;

    private Boolean humanApiAuthInd;

    private String partyNumber;

    private String partyTypeCode;

    private String prefComm;

    private String residenceCountry;

    private String residenceCounty;

    private String residenceState;

    private String residenceTaxLocality;

    private String userId;

    private Date updateTimestamp;

    private Boolean fraudLockInd;

    private Boolean fraudAlertInd;

    private String fraudStatus;
}


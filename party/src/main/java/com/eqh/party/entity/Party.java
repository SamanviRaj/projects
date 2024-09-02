package com.eqh.party.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "PARTY")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "currency_type_code", length = 10)
    private String currencyTypeCode;

    @Column(name = "eff_date")
    @Temporal(TemporalType.DATE)
    private Date effDate;

    @Column(name = "fatca_reportable_status", length = 10)
    private String fatcaReportableStatus;

    @Column(name = "fitbit_auth_ind", nullable = false)
    private Boolean fitbitAuthInd;

    @Column(name = "govtid", length = 9)
    private String govtId;

    @Column(name = "govtidstat", length = 10)
    private String govtIdStat;

    @Column(name = "govt_idtc", length = 10)
    private String govtIdtc;

    @Column(name = "ho_create_date")
    @Temporal(TemporalType.DATE)
    private Date hoCreateDate;

    @Column(name = "ho_expiry_date")
    @Temporal(TemporalType.DATE)
    private Date hoExpiryDate;

    @Column(name = "human_api_auth_ind", nullable = false)
    private Boolean humanApiAuthInd;

    @Column(name = "party_number", length = 15, nullable = false, unique = true)
    private String partyNumber;

    @Column(name = "party_type_code", length = 10, nullable = false)
    private String partyTypeCode;

    @Column(name = "pref_comm", length = 10)
    private String prefComm;

    @Column(name = "residence_country", length = 10)
    private String residenceCountry;

    @Column(name = "residence_county", length = 10)
    private String residenceCounty;

    @Column(name = "residence_state", length = 10)
    private String residenceState;

    @Column(name = "residence_tax_locality", length = 20)
    private String residenceTaxLocality;

    @Column(name = "user_id", length = 254)
    private String userId;

    @Column(name = "update_timestamp", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTimestamp;

    @Column(name = "fraud_lock_ind")
    private Boolean fraudLockInd;

    @Column(name = "fraud_alert_ind")
    private Boolean fraudAlertInd;

    @Column(name = "fraud_status", length = 10)
    private String fraudStatus;
}


package com.eqh.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Person {
    private Long id;
    private String alternateTaxId;
    private String alternateTaxIdTypeCode;
    private String birthCountryTC;
    private Date birthDate;
    private String birthJurisdiction;
    private String causeOfDeath;
    private String citizenship;
    private Date dateOfArrival;
    private Date dateOfDeath;
    private Boolean disabilityInd;
    private String driversLicenseNum;
    private String driversLicenseState;
    private String firstName;
    private String gender;
    private String heightMeasureUnits;
    private Long heightMeasureValue;
    private String immigrationStatus;
    private String lastName;
    private boolean legalNameInd;
    private String lifeStatus;
    private String marStat;
    private String middleName;
    private String occupation;
    private String prefix;
    private Date proofOfDeathReceivedDate;
    private Date proofOfDeathRequestedDate;
    private boolean restrictionInd;
    private String restrictionReason;
    private String smokerStat;
    private Date statusChangeDate;
    private String suffix;
    private Boolean usCitizenInd;
    private String weightMeasureUnits;
    private Long weightMeasureValue;
    private Long partyId;
    private String greencardNumber;
    private String visaNumber;
    private String passportNumber;
    private Date passportExpiryDate;
    private String passportIssueCountry;
    private String countryOfResidenship;
    private Date updateTimestamp;
}

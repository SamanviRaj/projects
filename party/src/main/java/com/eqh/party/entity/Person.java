package com.eqh.party.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "PERSON")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Person {

    @Id
    @Column(name = "id", nullable = false, length = 20)
    private Long id;

    @Column(name = "alternate_tax_id", length = 9)
    private String alternateTaxId;

    @Column(name = "alternate_tax_id_type_code", length = 10)
    private String alternateTaxIdTypeCode;

    @Column(name = "birth_country_tc", length = 10)
    private String birthCountryTC;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "birth_jurisdiction", length = 10)
    private String birthJurisdiction;

    @Column(name = "cause_of_death", length = 10)
    private String causeOfDeath;

    @Column(name = "citizenship", length = 10)
    private String citizenship;

    @Column(name = "date_of_arrival")
    private LocalDate dateOfArrival;

    @Column(name = "date_of_death")
    private LocalDate dateOfDeath;

    @Column(name = "disability_ind")
    private Boolean disabilityInd;

    @Column(name = "drivers_license_num", length = 20)
    private String driversLicenseNum;

    @Column(name = "drivers_license_state", length = 10)
    private String driversLicenseState;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "height_measure_units", length = 10)
    private String heightMeasureUnits;

    @Column(name = "height_measure_value", precision = 15)
    private Double heightMeasureValue;

    @Column(name = "immigration_status", length = 10)
    private String immigrationStatus;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "legal_name_ind")
    private Boolean legalNameInd;

    @Column(name = "life_status", length = 10)
    private String lifeStatus;

    @Column(name = "mar_stat", length = 10)
    private String marStat;

    @Column(name = "middle_name", length = 50)
    private String middleName;

    @Column(name = "occupation", length = 20)
    private String occupation;

    @Column(name = "prefix", length = 5)
    private String prefix;

    @Column(name = "proof_of_death_received_date")
    private LocalDate proofOfDeathReceivedDate;

    @Column(name = "proof_of_death_requested_date")
    private LocalDate proofOfDeathRequestedDate;

    @Column(name = "restriction_ind")
    private Boolean restrictionInd;

    @Column(name = "restriction_reason", length = 10)
    private String restrictionReason;

    @Column(name = "smoker_stat", length = 10)
    private String smokerStat;

    @Column(name = "status_change_date")
    private LocalDate statusChangeDate;

    @Column(name = "suffix", length = 5)
    private String suffix;

    @Column(name = "us_citizen_ind")
    private Boolean usCitizenInd;

    @Column(name = "weight_measure_units", length = 10)
    private String weightMeasureUnits;

    @Column(name = "weight_measure_value", precision = 15)
    private Double weightMeasureValue;

    @Column(name = "party_id")
    private Long partyId;

    @Column(name = "greencard_number", length = 20)
    private String greencardNumber;

    @Column(name = "visa_number", length = 20)
    private String visaNumber;

    @Column(name = "passport_number", length = 20)
    private String passportNumber;

    @Column(name = "passport_expiry_date")
    private LocalDate passportExpiryDate;

    @Column(name = "passport_issue_country", length = 20)
    private String passportIssueCountry;

    @Column(name = "country_of_residenship", length = 20)
    private String countryOfResidenship;

    @Column(name = "update_timestamp", nullable = false)
    private LocalDateTime updateTimestamp;
}

package com.eqh.application.dto;

import lombok.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Component
public class ProcessedRow {
    private String runYear;
    private String formattedRunDate;
    private String managementCode;
    private String productCode;
    private String polNumber;
    private String transformedPolicyStatus;
    private String transformedQualPlanType;
    private String transformedSuspendCode;
    private String taxablePartyNumber;
    private String taxablePartyName;
    private String taxableToGovtID;
    private String transformedGovtIDStatus;
    private String transformedGovtIdTCode;
    private String transformedPayeeStatus;
    private String transformedResidenceState;
    private String transformedResidenceCountry;
    private String preferredMailingAddress;
    private String mailingAddress;

}

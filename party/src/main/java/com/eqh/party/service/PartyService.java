package com.eqh.party.service;

import com.eqh.party.entity.Address;
import com.eqh.party.entity.EmailAddress;
import com.eqh.party.entity.Party;
import com.eqh.party.entity.Person;
import com.eqh.party.repository.PartyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PartyService {

    private final PartyRepository partyRepository;

    @Autowired
    public PartyService(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    public Party getPartyDetailsByPartyNumber(String partyNumber) {
        Optional<Party> party = partyRepository.findByPartyNumber(partyNumber);
        return party.orElse(null);
    }

    public Person getPersonDetailsByPartyNumber(String partyNumber) {
        // Retrieve party details
        Party party = getPartyDetailsByPartyNumber(partyNumber);

        if (party != null) {
            Long partyId = party.getId();

            // Fetch person details by party ID
            Optional<Object> results = partyRepository.findPersonByPartyId(partyId);

            if (results.isPresent()) {
                Object personObject = results.get();

                // Check if the object is of type Person
                if (personObject instanceof Object[]) {
                    Object[] result = (Object[]) personObject;
                    Person person = new Person();

                    // Map values from the result array to the Person entity
                    person.setId(((Number) result[0]).longValue());
                    person.setAlternateTaxId((String) result[1]);
                    person.setAlternateTaxIdTypeCode((String) result[2]);
                    person.setBirthCountryTC((String) result[3]);
                    person.setBirthDate((LocalDate) convertSqlDateToLocalDate(result[4]));
                    person.setBirthJurisdiction((String) result[5]);
                    person.setCauseOfDeath((String) result[6]);
                    person.setCitizenship((String) result[7]);
                    person.setDateOfArrival((LocalDate) convertSqlDateToLocalDate(result[8]));
                    person.setDateOfDeath((LocalDate) convertSqlDateToLocalDate(result[9]));
                    person.setDisabilityInd((Boolean) result[10]);
                    person.setDriversLicenseNum((String) result[11]);
                    person.setDriversLicenseState((String) result[12]);
                    person.setFirstName((String) result[13]);
                    person.setGender((String) result[14]);
                    person.setHeightMeasureUnits((String) result[15]);
                    person.setHeightMeasureValue((Double) result[16]);
                    person.setImmigrationStatus((String) result[17]);
                    person.setLastName((String) result[18]);
                    person.setLegalNameInd((Boolean) result[19]);
                    person.setLifeStatus((String) result[20]);
                    person.setMarStat((String) result[21]);
                    person.setMiddleName((String) result[22]);
                    person.setOccupation((String) result[23]);
                    person.setPrefix((String) result[24]);
                    person.setProofOfDeathReceivedDate((LocalDate) convertSqlDateToLocalDate(result[25]));
                    person.setProofOfDeathRequestedDate((LocalDate) convertSqlDateToLocalDate(result[26]));
                    person.setRestrictionInd((Boolean) result[27]);
                    person.setRestrictionReason((String) result[28]);
                    person.setSmokerStat((String) result[29]);
                    person.setStatusChangeDate((LocalDate) convertSqlDateToLocalDate(result[30]));
                    person.setSuffix((String) result[31]);
                    person.setUsCitizenInd((Boolean) result[32]);
                    person.setWeightMeasureUnits((String) result[33]);
                    person.setWeightMeasureValue((Double) result[34]);
                    person.setPartyId(((Number) result[35]).longValue());
                    person.setGreencardNumber((String) result[36]);
                    person.setVisaNumber((String) result[37]);
                    person.setPassportNumber((String) result[38]);
                    person.setPassportExpiryDate((LocalDate) convertSqlDateToLocalDate(result[39]));
                    person.setPassportIssueCountry((String) result[40]);
                    person.setCountryOfResidenship((String) result[41]);
                    //person.setUpdateTimestamp((LocalDateTime) result[42]);

                    Timestamp updateTimestampSql = (Timestamp) result[42];
                    LocalDateTime updateTimestamp = updateTimestampSql != null ? updateTimestampSql.toLocalDateTime() : null;
                    person.setUpdateTimestamp(updateTimestamp);


                    return person; // Return the populated Person entity
                }
            }
        }

        return null; // Return null if no person found
    }

    // Helper method to convert java.sql.Date to java.time.LocalDate
    private LocalDate convertSqlDateToLocalDate(Object sqlDateObj) {
        if (sqlDateObj instanceof java.sql.Date) {
            return ((java.sql.Date) sqlDateObj).toLocalDate();
        }
        return null; // or throw an exception if preferred
    }


    public List<EmailAddress> getEmailAddressesByPartyNumber(String partyNumber) {
        Party party = getPartyDetailsByPartyNumber(partyNumber);

        if (party != null) {
            Long partyId = party.getId();
            List<Object[]> results = partyRepository.findEmailAddressesByPartyId(partyId);

            List<EmailAddress> emailAddresses = new ArrayList<>();
            for (Object[] result : results) {
                EmailAddress emailAddress = new EmailAddress();
                emailAddress.setId(((Number) result[0]).longValue());
                emailAddress.setAddrLine((String) result[1]);
                emailAddress.setAttachmentInd((Boolean) result[2]);
                emailAddress.setEmailType((String) result[3]);

                Date endDateSql = (Date) result[4];
                LocalDate endDate = endDateSql != null ? endDateSql.toLocalDate() : null;
                emailAddress.setEndDate(endDate);

                Date invalidemailStartDateSql = (Date) result[5];
                LocalDate invalidemailStartDate = invalidemailStartDateSql != null ? invalidemailStartDateSql.toLocalDate() : null;
                emailAddress.setInvalidemailStartDate(invalidemailStartDate);

                Date startDateSql = (Date) result[8];
                LocalDate startDate = startDateSql != null ? startDateSql.toLocalDate() : null;
                emailAddress.setStartDate(startDate);

                Timestamp updateTimestampSql = (Timestamp) result[11];
                LocalDateTime updateTimestamp = updateTimestampSql != null ? updateTimestampSql.toLocalDateTime() : null;
                emailAddress.setUpdateTimestamp(updateTimestamp);

                emailAddress.setLanguage((String) result[6]);
                emailAddress.setPrefemailAddr((Boolean) result[7]);
                emailAddress.setUndeliverableInd((Boolean) result[9]);
                emailAddress.setPartyId(((Number) result[10]).longValue());

                emailAddresses.add(emailAddress);
            }
            return emailAddresses;
        } else {
            return List.of();
        }
    }

    public List<Address> getAddressesByPartyNumber(String partyNumber) {
        Party party = getPartyDetailsByPartyNumber(partyNumber);

        if (party != null) {
            Long partyId = party.getId();
            List<Object[]> results = partyRepository.findAddressesByPartyId(partyId);

            List<Address> addresses = new ArrayList<>();
            for (Object[] result : results) {
                Address address = new Address();
                address.setId(((Number) result[0]).longValue());
                address.setAddressBarCodeInd((Boolean) result[1]);
                address.setAddressCountrytc((String) result[2]);
                address.setAddressCountytc((String) result[3]);
                address.setAddressFormattc((String) result[4]);
                address.setAddressStatetc((String) result[5]);
                address.setAddressTypeCode((String) result[6]);

                Date addressValidationDateSql = (Date) result[7];
                LocalDate addressValidationDate = addressValidationDateSql != null ? addressValidationDateSql.toLocalDate() : null;
                address.setAddressValidationDate(addressValidationDate);

                address.setAddressValidInd((Boolean) result[8]);
                address.setAttentionLine((String) result[9]);
                address.setCity((String) result[10]);

                Date endDateSql = (Date) result[11];
                LocalDate endDate = endDateSql != null ? endDateSql.toLocalDate() : null;
                address.setEndDate(endDate);

                address.setForeignAddressInd((Boolean) result[12]);
                address.setLanguage((String) result[13]);
                address.setLegalAddressInd((Boolean) result[14]);
                address.setLine1((String) result[15]);
                address.setLine2((String) result[16]);
                address.setLine3((String) result[17]);
                address.setLine4((String) result[18]);
                address.setLine5((String) result[19]);
                address.setPostalDropCode((String) result[20]);
                address.setPrefAddr((Boolean) result[21]);
                address.setPreventOverrideInd((Boolean) result[22]);
                address.setRecurringEndMoDay((String) result[23]);
                address.setRecurringStartMoDay((String) result[24]);
                address.setReturnedMailInd((Boolean) result[25]);
                address.setReturnedMailReason((String) result[26]);
                address.setReturnedMailStartDate((Date) result[27] != null ? ((Date) result[27]).toLocalDate() : null);
                address.setStartDate((Date) result[28] != null ? ((Date) result[28]).toLocalDate() : null);
                address.setZip((String) result[29]);
                address.setAddressStateOthers((String) result[30]);
                address.setPartyId(((Number) result[31]).longValue());
                address.setUpdateTimestamp((Timestamp) result[32] != null ? ((Timestamp) result[32]).toLocalDateTime() : null);

                addresses.add(address);
            }
            return addresses;
        } else {
            return List.of();
        }
    }
}

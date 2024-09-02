package com.eqh.party.service;

import com.eqh.party.entity.Address;
import com.eqh.party.entity.EmailAddress;
import com.eqh.party.entity.Party;
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

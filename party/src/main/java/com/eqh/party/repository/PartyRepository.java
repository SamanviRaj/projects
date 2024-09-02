package com.eqh.party.repository;

import com.eqh.party.entity.EmailAddress;
import com.eqh.party.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartyRepository extends JpaRepository<Party, Long> {

    @Query(
            value = "SELECT party.id, party.currency_type_code, party.eff_date, party.fatca_reportable_status, party.fitbit_auth_ind, " +
                    "party.govtid, party.govtidstat, party.govt_idtc, party.ho_create_date, party.ho_expiry_date, party.human_api_auth_ind, " +
                    "party.party_number, party.party_type_code, party.pref_comm, party.residence_country, party.residence_county, " +
                    "party.residence_state, party.residence_tax_locality, party.user_id, party.fraud_lock_ind, party.fraud_alert_ind, " +
                    "party.fraud_status, party.update_timestamp " +
                    "FROM \"PARTY\" party WHERE party.party_number = :partyNumber",
            nativeQuery = true
    )
    Optional<Party> findByPartyNumber(@Param("partyNumber") String partyNumber);

    @Query(
            value = "SELECT id, addr_line, attachment_ind, email_type, end_date, invalidemail_start_date, " +
                    "language, prefemail_addr, start_date, undeliverable_ind, party_id, update_timestamp " +
                    "FROM \"E_MAIL_ADDRESS\" WHERE party_id = :partyId",
            nativeQuery = true
    )
    List<Object[]>  findEmailAddressesByPartyId(@Param("partyId") Long partyId);

    @Query(
            value = "SELECT id, address_bar_code_ind, address_countrytc, address_countytc, address_formattc, address_statetc, address_type_code, " +
                    "address_validation_date, address_valid_ind, attention_line, city, end_date, foreign_address_ind, language, legal_address_ind, " +
                    "line1, line2, line3, line4, line5, postal_drop_code, pref_addr, prevent_override_ind, recurring_end_mo_day, recurring_start_mo_day, " +
                    "returned_mail_ind, returned_mail_reason, returned_mail_start_date, start_date, zip, address_state_others, party_id, update_timestamp " +
                    "FROM \"ADDRESS\" WHERE party_id = :partyId",
            nativeQuery = true
    )
    List<Object[]> findAddressesByPartyId(@Param("partyId") Long partyId);
}

package com.eqh.party.repository;

import com.eqh.party.entity.EmailAddress;
import com.eqh.party.entity.Party;
import com.eqh.party.entity.Person;
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


    @Query(
            value = "SELECT person.id, person.alternate_tax_id, person.alternate_tax_id_type_code, person.birth_countrytc, person.birth_date, " +
                    "person.birth_jurisdiction, person.cause_of_death, person.citizenship, person.date_of_arrival, person.date_of_death, " +
                    "person.disability_ind, person.drivers_license_num, person.drivers_license_state, person.first_name, person.gender, " +
                    "person.height_measure_units, person.height_measure_value, person.immigration_status, person.last_name, person.legal_name_ind, " +
                    "person.life_status, person.mar_stat, person.middle_name, person.occupation, person.prefix, person.proof_of_death_received_date, " +
                    "person.proof_of_death_requested_date, person.restriction_ind, person.restriction_reason, person.smoker_stat, person.status_change_date, " +
                    "person.suffix, person.us_citizen_ind, person.weight_measure_units, person.weight_measure_value, person.party_id, " +
                    "person.greencard_number, person.visa_number, person.passport_number, person.passport_expiry_date, person.passport_issue_country, " +
                    "person.country_of_residenship, person.update_timestamp " +
                    "FROM \"PERSON\" person, \"PARTY\" party WHERE person.party_id = party.id AND party.id = :partyId",
            nativeQuery = true
    )
    Optional<Object> findPersonByPartyId(@Param("partyId") Long partyId);

}

package com.eqh.application.repository;

import com.eqh.application.entity.PayoutPayee;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.*;

@Repository
public interface PayoutPayeeRepository extends JpaRepository<PayoutPayee, Long> {

    @Query(value = "SELECT id,banking_number FROM public.\"PAYOUT_PAYEE\" pp WHERE pp.policy_payout_id = :policyPayoutId AND pp.payee_party_number = :payeePartyNumber", nativeQuery = true)
    Long findPayoutPayeeByPolicyPayoutIdAndPartyNumber(
            @Param("policyPayoutId") Long policyPayoutId,
            @Param("payeePartyNumber") String payeePartyNumber
    );

}

package com.eqh.application.repository;

import com.eqh.application.entity.PayoutPayee;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.*;

import java.util.List;

@Repository
public interface PayoutPayeeRepository extends JpaRepository<PayoutPayee, Long> {

    @Query(value = "SELECT * FROM public.\"PAYOUT_PAYEE\" pp WHERE pp.policy_payout_id = :policyPayoutId", nativeQuery = true)
    List<PayoutPayee> findPayoutPayeeByPolicyPayoutIdAndPartyNumber(
            @Param("policyPayoutId") Long policyPayoutId
    );

}

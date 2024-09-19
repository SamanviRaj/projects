package com.eqh.application.repository;

import com.eqh.application.entity.Policy;
import com.eqh.application.entity.PolicyPayout;
import com.eqh.application.entity.PayoutPayee;
import com.eqh.application.entity.PayoutPaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyPayoutRepository extends JpaRepository<PolicyPayout, Long> {

    @Query(value = "SELECT * FROM public.\"POLICY_PAYOUT\" pp WHERE pp.policy_id = CAST(:policyId AS bigint)", nativeQuery = true)
    Long findPolicyPayoutByPolicyId(@Param("policyId") Long policyId);

}

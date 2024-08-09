package com.eqh.application.repository;

import com.eqh.application.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    @Query(value="SELECT product_code FROM public.\"POLICY\" WHERE pol_number = :polNumber AND policy_status != 'R'", nativeQuery = true)
    String findProductCodeByPolicyNumber(@Param("polNumber") String polNumber);
}


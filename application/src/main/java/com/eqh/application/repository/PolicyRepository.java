package com.eqh.application.repository;

import com.eqh.application.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    @Query(value = "SELECT pol_number, product_code FROM public.\"POLICY\" WHERE policy_status NOT IN ('R', '14', '13')", nativeQuery = true)
    List<Object[]> findAllPolicyNumbersWithProductCodes();

    @Query(value = "SELECT product_code FROM public.\"POLICY\" WHERE pol_number = :polNumber AND policy_status NOT IN ('R', '14', '13')", nativeQuery = true)
    String findProductCodeByPolicyNumber(@Param("polNumber") String polNumber);
}

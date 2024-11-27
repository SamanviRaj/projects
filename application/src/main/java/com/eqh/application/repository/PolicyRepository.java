package com.eqh.application.repository;

import com.eqh.application.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    @Query(value = "SELECT pol_number, product_code FROM public.\"POLICY\" WHERE policy_status NOT IN ('R', '14', '13')", nativeQuery = true)
    List<Object[]> findAllPolicyNumbersWithProductCodes();

    @Query(value = "SELECT pol_number, product_code FROM public.\"POLICY\" WHERE policy_status NOT IN ('R', '13')", nativeQuery = true)
    List<Object[]> findAllPolicyNumbersWithProductCodesOverduePayment();

    @Query(value = "SELECT product_code,management_code,policy_status FROM public.\"POLICY\" WHERE pol_number = :polNumber AND policy_status NOT IN ('R', '14', '13')", nativeQuery = true)
    List<Object[]> findProductInfoByPolicyNumber(@Param("polNumber") String polNumber);

    @Query(value = "SELECT pol_number, management_code ,policy_status ,product_code,qual_plan_type " +
            "FROM public.\"POLICY\" " +
            "WHERE pol_number IN :policyNumbers " +
            "AND policy_status NOT IN ('R', '14', '13')", nativeQuery = true)
    List<Object[]> findProductInfoByPolicyNumbers(@Param("policyNumbers") List<String> policyNumbers);

    @Query(value = "SELECT pol_number, management_code ,policy_status ,product_code,qual_plan_type " +
            "FROM public.\"POLICY\" " +
            "WHERE pol_number IN :policyNumbers " +
            "AND policy_status NOT IN ('13')", nativeQuery = true)
    List<Object[]> findProductInfoByPolicyNumbersOverduePayment(@Param("policyNumbers") List<String> policyNumbers);

    @Query(value = "SELECT id FROM \"POLICY\"  WHERE pol_number = :polNumber AND policy_status <> 'R'", nativeQuery = true)
    Long findPolicyByNumberAndStatus(@Param("polNumber") String polNumber);
}

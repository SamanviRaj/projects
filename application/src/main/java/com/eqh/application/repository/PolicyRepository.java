package com.eqh.application.repository;

import com.eqh.application.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    /*
    *
    * policy status - R reprsents soft deleted.
    * policy status - 14 is terminated.
    * policy status - 13 is Home Office Cancellation.
    *
    * */
    @Query(value="SELECT product_code FROM public.\"POLICY\" WHERE pol_number = :polNumber AND policy_status NOT IN ('R', '14', '13')", nativeQuery = true)
    String findProductCodeByPolicyNumber(@Param("polNumber") String polNumber);

}


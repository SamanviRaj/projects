package com.eqh.application.repository;

import com.eqh.application.entity.TransactionHistory;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface PeriodicPayoutTransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {

    @Query(value = "SELECT message_image, gross_amt, trans_eff_date, trans_run_date " +
            "FROM public.\"TRANSACTION_HISTORY\" " +
            "WHERE reversed = false " +
            "AND entity_type = 'Policy' " +
            "AND request_name = 'PeriodicPayout' " +
            "AND trans_exe_date BETWEEN :startDate AND :endDate " +
            "ORDER BY trans_eff_date DESC",
            nativeQuery = true)
    List<Object[]> findPayoutTransactionsInRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT message_image, gross_amt, trans_eff_date, trans_run_date " +
            "FROM public.\"TRANSACTION_HISTORY\" " +
            "WHERE reversed = false " +
            "AND entity_type = 'Policy' " +
            "AND request_name = 'PeriodicPayout' " +
            "AND trans_exe_date > :startDate " +
            "ORDER BY trans_eff_date DESC LIMIT 50",
            nativeQuery = true)
    List<Object[]> findPayoutTransactionsInRange(@Param("startDate") LocalDateTime  startDate);

    @Query(value = "SELECT message_image, gross_amt, trans_eff_date ,trans_run_date FROM public.\"TRANSACTION_HISTORY\" " +
            "WHERE reversed = false AND entity_type='Policy' AND " +
            "request_name = 'PayoutDeathClaim' AND EXTRACT(YEAR FROM trans_eff_date) " +
            "IN (2024, 2025) ORDER BY trans_eff_date DESC",nativeQuery = true)
    List<Object[]> findPayoutTransactionsInRangeOfYear();

    @Query(value = "SELECT message_image, gross_amt, trans_eff_date, trans_run_date " +
            "FROM public.\"TRANSACTION_HISTORY\" " +
            "WHERE reversed = false " +
            "AND entity_type = 'Policy' " +
            "AND request_name = 'PeriodicPayout' " +
            "AND message_image \\:\\: json->>'polNumber' = :policyNumber " +
            "AND trans_exe_date > :startDate " +
            "ORDER BY trans_eff_date DESC LIMIT 50",
            nativeQuery = true)
    List<Object[]> findPayoutTransactionsByPolicyNumber(
            @Param("policyNumber") String policyNumber,
            @Param("startDate") LocalDateTime startDate);

}

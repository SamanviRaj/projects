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
            "ORDER BY trans_eff_date DESC LIMIT 50 ",
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
            "ORDER BY trans_eff_date DESC LIMIT 50 ",
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
            "AND trans_exe_date >= :startDate " + // Added a space here
            "AND trans_exe_date <= :endDate " + // Added a space here
            "ORDER BY trans_eff_date DESC LIMIT 1",
            nativeQuery = true)
    List<Object[]> findPayoutTransactionsByPolicyNumber(
            @Param("policyNumber") String policyNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT message_image, gross_amt, trans_eff_date, trans_run_date " +
            "FROM public.\"TRANSACTION_HISTORY\" " +
            "WHERE reversed = false " +
            "AND entity_type = 'Policy' " +
            "AND request_name = 'PeriodicPayout' " +
            "AND message_image \\:\\: json->>'polNumber' = :policyNumber " +
            "AND trans_exe_date > :startDate " +
            "ORDER BY trans_eff_date DESC Limit 1 ",
            nativeQuery = true)
    List<Object[]> findPayoutTransactionsByPolicyNumber(
            @Param("policyNumber") String policyNumber,
            @Param("startDate") LocalDateTime startDate);

    @Query(value = """
            WITH RankedTransactions AS (
                                                 SELECT
                                                     message_image\\:\\:json->>'polNumber' AS policy_number,
                                                     trans_exe_date,
                                                     request_name,
                                                     message_image,
                                                     ROW_NUMBER() OVER (PARTITION BY message_image\\:\\:json->>'polNumber' ORDER BY trans_exe_date DESC) AS rn
                                                 FROM
                                                     public."TRANSACTION_HISTORY"
                                                 WHERE
                                                     request_name IN ('PeriodicPayout')
                                                     AND reversed = false
                                                     AND trans_exe_date >= :startDate
                                                     AND trans_exe_date <= :endDate
                                             )
                                             SELECT
                                                 policy_number,
                                                 trans_exe_date,
                                                 request_name,
                                                 message_image
                                             FROM
                                                 RankedTransactions
                                             WHERE
                                                 rn = 1
                                             ORDER BY
                                                 trans_exe_date DESC
            """, nativeQuery = true)
    List<Object[]> findLatestTransactions(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = """
            WITH RankedTransactions AS (
                SELECT 
                    message_image\\:\\:json->>'polNumber' AS policy_number,
                    trans_exe_date,
                    request_name,
                    message_image,
                    ROW_NUMBER() OVER (PARTITION BY message_image\\:\\:json->>'polNumber' ORDER BY trans_exe_date DESC) AS rn
                FROM 
                    public."TRANSACTION_HISTORY"
                WHERE 
                    request_name = 'PeriodicPayout'
                    AND reversed = false
                    AND trans_exe_date > ?
            )
            SELECT 
                policy_number,
                trans_exe_date,
                request_name,
                message_image
            FROM 
                RankedTransactions
            WHERE 
                rn = 1
            ORDER BY 
                trans_exe_date DESC
            """, nativeQuery = true)
    List<Object[]> findLatestTransactions(@Param("date") LocalDateTime date);

    @Query(value = """
        SELECT message_image, gross_amt, trans_eff_date, trans_run_date
        FROM public."TRANSACTION_HISTORY"
        WHERE reversed = false 
        AND entity_type = 'Policy' 
        AND request_name = 'OverduePayment'
        AND message_image\\:\\:json->>'polNumber' = :policyNumber
        AND trans_exe_date > :executionDate
        ORDER BY trans_eff_date DESC
    """, nativeQuery = true)
    List<Object[]> findOverduePaymentsByPolicyNumberAndDate(
            @Param("policyNumber") String policyNumber,
            @Param("executionDate") LocalDateTime executionDate);

    @Query(value = """
    SELECT 
        message_image::json->>'polNumber' AS policy_number,
        trans_exe_date,
        request_name,
        message_image
    FROM 
        public."TRANSACTION_HISTORY" 
    WHERE 
        request_name IN ('OverduePayment', 'PeriodicPayout')
        AND reversed = false
        AND message_image::json->>'polNumber' = :policyNumber
        AND trans_exe_date > :executionDate
    ORDER BY 
        trans_exe_date DESC Limit 1
""", nativeQuery = true)
    List<Object[]> findOverdueAndPeriodicPayoutTransactionsByPolicyNumber(
            @Param("policyNumber") String policyNumber,
            @Param("executionDate") LocalDateTime executionDate);


    @Query(value = """
    SELECT 
        message_image\\:\\:json->>'polNumber' AS policy_number,
        trans_exe_date,
        request_name,
        message_image
    FROM 
        public."TRANSACTION_HISTORY" 
    WHERE 
        request_name IN ('OverduePayment') 
        AND reversed = false
        AND message_image\\:\\:json->>'polNumber' = :policyNumber
        AND message_image\\:\\:json->'adjustments'->'arrDestinations'->0->'payeeInfo'->>'taxablePartyNumber' = :taxablePartyNumber
        AND trans_exe_date > :executionDate
    ORDER BY 
        trans_exe_date DESC 
    LIMIT 1
""", nativeQuery = true)
    List<Object[]> findOverduePaymentsByPolicyNumberAndTaxablePartyNumber(
            @Param("policyNumber") String policyNumber,
            @Param("taxablePartyNumber") String taxablePartyNumber,
            @Param("executionDate") LocalDateTime executionDate);

}

package com.eqh.application.repository;

import com.eqh.application.entity.PayoutPaymentHistoryDeduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PayoutPaymentHistoryDeductionRepository extends JpaRepository<PayoutPaymentHistoryDeduction, Long> {

    @Query(value = """
        SELECT * FROM payout_payment_history_deduction
        WHERE payout_payment_history_id IN (
            SELECT id FROM payout_payment_history pph
            WHERE payee_party_number = :payeePartyNumber
            AND reversed = false
            AND payout_payee_id IN (
                SELECT id FROM payout_payee pp
                WHERE policy_payout_id IN (
                    SELECT id FROM policy_payout pp
                    WHERE policy_id IN (
                        SELECT id FROM policy
                        WHERE pol_number = :policyNumber
                        AND policy_status <> 'R'
                    )
                    AND payee_party_number = :payeePartyNumber
                )
            )
        )
        ORDER BY id
    """, nativeQuery = true)
    List<PayoutPaymentHistoryDeduction> findByPayeePartyNumberAndPolicyNumber(
            @Param("payeePartyNumber") String payeePartyNumber,
            @Param("policyNumber") String policyNumber
    );

    @Query(value = "SELECT id, fee_type, fee_amt, payout_payment_history_id, update_timestamp " +
            "FROM public.\"PAYOUT_PAYMENT_HISTORY_DEDUCTION\" " +
            "WHERE payout_payment_history_id = :payoutPaymentHistoryId", nativeQuery = true)
    List<PayoutPaymentHistoryDeduction> findFeeDetailsByPayoutPaymentHistoryId(
            @Param("payoutPaymentHistoryId") Long payoutPaymentHistoryId);

    @Query(value = """
         SELECT payout_payment_history_id
                FROM public."PAYOUT_PAYMENT_HISTORY_DEDUCTION"
    """, nativeQuery = true)
    List<Long> findAllPaymentHistoryIdsOfDeductions();

    @Query(value = """
                SELECT COALESCE(SUM(fee_amt), 0)
                FROM "PAYOUT_PAYMENT_HISTORY_DEDUCTION" 
                WHERE payout_payment_history_id IN (
                    SELECT id 
                    FROM "PAYOUT_PAYMENT_HISTORY" pph 
                    WHERE payee_party_number = :payeePartyNumber 
                      AND reversed = false 
                      AND trans_exe_date >= :startDate
                      AND trans_exe_date <= :endDate
                      AND payout_payee_id IN (
                          SELECT id 
                          FROM "PAYOUT_PAYEE" pp 
                          WHERE policy_payout_id IN (
                              SELECT id 
                              FROM "POLICY_PAYOUT" pp 
                              WHERE policy_id IN (
                                  SELECT id 
                                  FROM "POLICY" 
                                  WHERE pol_number = :policyNumber 
                                  AND policy_status <> 'R'
                              )
                          ) 
                          AND payee_party_number = :payeePartyNumber
                      )
                ) 
                AND fee_type = '21'
            """, nativeQuery = true)
    Double sumFeeAmtByFeeTypeState(
            @Param("payeePartyNumber") String payeePartyNumber,
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
            @Param("policyNumber") String policyNumber
    );

    @Query(value = """
        SELECT COALESCE(SUM(fee_amt), 0)
        FROM "PAYOUT_PAYMENT_HISTORY_DEDUCTION" 
        WHERE payout_payment_history_id IN (
            SELECT id 
            FROM "PAYOUT_PAYMENT_HISTORY" pph 
            WHERE payee_party_number = :payeePartyNumber 
              AND reversed = false 
              AND trans_exe_date > :transExeDate 
              AND payout_payee_id IN (
                  SELECT id 
                  FROM "PAYOUT_PAYEE" pp 
                  WHERE policy_payout_id IN (
                      SELECT id 
                      FROM "POLICY_PAYOUT" pp 
                      WHERE policy_id IN (
                          SELECT id 
                          FROM "POLICY" 
                          WHERE pol_number = :policyNumber 
                          AND policy_status <> 'R'
                      )
                  ) 
                  AND payee_party_number = :payeePartyNumber
              )
        ) 
        AND fee_type = '21'
    """, nativeQuery = true)
    Double sumFeeAmtByFeeTypeState(
            @Param("payeePartyNumber") String payeePartyNumber,
            @Param("transExeDate") LocalDateTime transExeDate,
            @Param("policyNumber") String policyNumber
    );

    @Query(value = """
                SELECT COALESCE(SUM(fee_amt), 0)
                FROM "PAYOUT_PAYMENT_HISTORY_DEDUCTION" 
                WHERE payout_payment_history_id IN (
                    SELECT id 
                    FROM "PAYOUT_PAYMENT_HISTORY" pph 
                    WHERE payee_party_number = :payeePartyNumber 
                      AND reversed = false 
                      AND trans_exe_date >= :startDate
                      AND trans_exe_date <= :endDate
                      AND payout_payee_id IN (
                          SELECT id 
                          FROM "PAYOUT_PAYEE" pp 
                          WHERE policy_payout_id IN (
                              SELECT id 
                              FROM "POLICY_PAYOUT" pp 
                              WHERE policy_id IN (
                                  SELECT id 
                                  FROM "POLICY" 
                                  WHERE pol_number = :policyNumber 
                                  AND policy_status <> 'R'
                              )
                          ) 
                          AND payee_party_number = :payeePartyNumber
                      )
                ) 
                AND fee_type = '20'
            """, nativeQuery = true)
    Double sumFeeAmtByFeeTypeFederal(
            @Param("payeePartyNumber") String payeePartyNumber,
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
            @Param("policyNumber") String policyNumber
    );


    @Query(value = """
        SELECT COALESCE(SUM(fee_amt), 0)
        FROM "PAYOUT_PAYMENT_HISTORY_DEDUCTION" 
        WHERE payout_payment_history_id IN (
            SELECT id 
            FROM "PAYOUT_PAYMENT_HISTORY" pph 
            WHERE payee_party_number = :payeePartyNumber 
              AND reversed = false 
              AND trans_exe_date > :transExeDate 
              AND payout_payee_id IN (
                  SELECT id 
                  FROM "PAYOUT_PAYEE" pp 
                  WHERE policy_payout_id IN (
                      SELECT id 
                      FROM "POLICY_PAYOUT" pp 
                      WHERE policy_id IN (
                          SELECT id 
                          FROM "POLICY" 
                          WHERE pol_number = :policyNumber 
                          AND policy_status <> 'R'
                      )
                  ) 
                  AND payee_party_number = :payeePartyNumber
              )
        ) 
        AND fee_type = '20'
    """, nativeQuery = true)
    Double sumFeeAmtByFeeTypeFederal(
            @Param("payeePartyNumber") String payeePartyNumber,
            @Param("transExeDate") LocalDateTime transExeDate,
            @Param("policyNumber") String policyNumber
    );
}


package com.eqh.application.repository;

import com.eqh.application.entity.PayoutPaymentHistory;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PayoutPaymentHistoryRepository extends JpaRepository<PayoutPaymentHistory, Long> {

    @Query(value = "SELECT * FROM public.\"PAYOUT_PAYMENT_HISTORY\" pph WHERE " +
            "pph.payee_party_number = :payeePartyNumber " +
            "AND pph.payout_payee_id = :payoutPayeeId " +
            "AND trans_exe_date >= :startDate " +
            "AND pph.reversed = false ORDER BY pph.id", nativeQuery = true)
    List<PayoutPaymentHistory> findPayoutPaymentHistoryByPayeePartyNumberAndPayeeId(
            @Param("payeePartyNumber") String payeePartyNumber,
            @Param("payoutPayeeId") Long payoutPayeeId,
            @Param("startDate") LocalDateTime startDate
    );

    @Query(value = """
        SELECT pph.*
        FROM "POLICY" p
        JOIN "POLICY_PAYOUT" pp ON pp.policy_id = p.id
        JOIN "PAYOUT_PAYEE" ppy ON ppy.policy_payout_id = pp.id
        JOIN public."PAYOUT_PAYMENT_HISTORY" pph ON pph.payee_party_number = ppy.payee_party_number
        WHERE p.pol_number = :policyNumber
          AND pph.payout_payee_id = ppy.id
          AND pph.reversed = false
          AND pph.trans_exe_date > :startDate
        ORDER BY pph.trans_exe_date DESC
        """, nativeQuery = true)
    List<PayoutPaymentHistory> findPolicyPayouts(
            @Param("policyNumber") String policyNumber,
            @Param("startDate") LocalDateTime startDate
    );

    @Query(value = """
    SELECT COALESCE(SUM(pph.gross_amt), 0) 
    FROM "POLICY" p
    JOIN "POLICY_PAYOUT" pp ON pp.policy_id = p.id
    JOIN "PAYOUT_PAYEE" pp2 ON pp2.policy_payout_id = pp.id
    JOIN public."PAYOUT_PAYMENT_HISTORY" pph ON pph.payout_payee_id = pp2.id
    WHERE p.pol_number = :policyNumber
      AND pph.reversed = false 
      AND pph.trans_exe_date > :date
""", nativeQuery = true)
    Double findPolicyPayoutWithGrossAmount(
            @Param("policyNumber") String policyNumber,
            @Param("date") LocalDateTime date // Ensure the parameter name matches
    );

    @Query(value = """
            SELECT pph.*
            FROM "POLICY" p
            JOIN "POLICY_PAYOUT" pp ON pp.policy_id = p.id
            JOIN "PAYOUT_PAYEE" ppy ON ppy.policy_payout_id = pp.id
            JOIN public."PAYOUT_PAYMENT_HISTORY" pph ON pph.payee_party_number = ppy.payee_party_number
            WHERE p.pol_number = :policyNumber
              AND pph.payout_payee_id = ppy.id
              AND pph.reversed = false
              AND pph.trans_exe_date > :startDate
              AND pph.payout_due_date <= :transEffDate
              AND pph.payout_due_date >= :ytdCalendarTime
            ORDER BY pph.trans_exe_date DESC
            """, nativeQuery = true)
    List<PayoutPaymentHistory> findPolicyPayoutsWithDueDateCriteria(
            @Param("policyNumber") String policyNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("transEffDate") LocalDateTime transEffDate,  // For payoutDueDate comparison
            @Param("ytdCalendarTime") LocalDateTime ytdCalendarTime // For ytdCalendar comparison
    );

}

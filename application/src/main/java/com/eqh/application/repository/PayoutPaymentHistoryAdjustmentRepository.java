package com.eqh.application.repository;

import com.eqh.application.entity.PayoutPaymentHistoryAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayoutPaymentHistoryAdjustmentRepository extends JpaRepository<PayoutPaymentHistoryAdjustment, Long> {

    @Query(value = """
        SELECT * FROM payout_payment_history_adjustment
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
    List<PayoutPaymentHistoryAdjustment> findByPayeePartyNumberAndPolicyNumber(
            @Param("payeePartyNumber") String payeePartyNumber,
            @Param("policyNumber") String policyNumber
    );

    @Query(value = "SELECT id, field_adjustment, adjustment_value, adjustment_type, adjustment_fin_act_type, remarks, payout_payment_history_id, update_timestamp " +
            "FROM public.\"PAYOUT_PAYMENT_HISTORY_ADJUSTMENT\" " +
            "WHERE payout_payment_history_id = :payoutPaymentHistoryId", nativeQuery = true)
    List<PayoutPaymentHistoryAdjustment> findFeeDetailsByPayoutPaymentHistoryId(
            @Param("payoutPaymentHistoryId") Long payoutPaymentHistoryId);

    @Query(value = """
                SELECT payout_payment_history_id
                FROM public."PAYOUT_PAYMENT_HISTORY_ADJUSTMENT"
            """, nativeQuery = true)
    List<Long> findAllPaymentHistoryIdsOfAdjustments();
}

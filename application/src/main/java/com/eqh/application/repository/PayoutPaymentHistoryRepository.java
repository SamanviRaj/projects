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

}

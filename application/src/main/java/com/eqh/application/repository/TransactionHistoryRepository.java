package com.eqh.application.repository;

import com.eqh.application.entity.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {

    @Query(value = "SELECT message_image, gross_amt, trans_eff_date ,trans_run_date FROM public.\"TRANSACTION_HISTORY\" WHERE reversed = false AND entity_type='Policy' AND request_name = 'PayoutDeathClaim' AND EXTRACT(YEAR FROM trans_eff_date) IN (2024, 2025) ORDER BY trans_eff_date DESC", nativeQuery = true)
    List<Object[]> findCustomPayoutDeathClaimTransactions();

    @Query(value = "SELECT message_image, gross_amt, trans_eff_date ,trans_run_date, message_image\\:\\:json->>'polNumber' AS policy_number FROM public.\"TRANSACTION_HISTORY\" WHERE reversed = false AND entity_type='Policy' AND request_name = 'OverduePayment' AND EXTRACT(YEAR FROM trans_eff_date) IN (2025, 2026, 2030, 2024, 2032, 2034, 2035) ORDER BY trans_eff_date DESC", nativeQuery = true)
    List<Object[]> findOverduePaymentTransactions();
}



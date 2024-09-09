package com.eqh.application.repository;

import com.eqh.application.entity.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface PeriodicPayoutTransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {

    @Query(value = "SELECT message_image, gross_amt, trans_eff_date, trans_run_date " +
            "FROM public.\"TRANSACTION_HISTORY\" " +
            "WHERE reversed = false " +
            "AND entity_type = 'Policy' " +
            "AND request_name = 'PeriodicPayout' " +
            "AND EXTRACT(YEAR FROM trans_eff_date) IN (2024, 2025) " +
            "ORDER BY trans_eff_date DESC",
            nativeQuery = true)
    List<Object[]> findCustomPayoutDeathClaimTransactions();

}

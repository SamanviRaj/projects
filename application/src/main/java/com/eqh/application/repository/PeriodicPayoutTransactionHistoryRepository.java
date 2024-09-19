package com.eqh.application.repository;

import com.eqh.application.entity.TransactionHistory;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface PeriodicPayoutTransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {

/*    @Query(value = "SELECT message_image, gross_amt, trans_eff_date, trans_run_date " +
            "FROM public.\"TRANSACTION_HISTORY\" " +
            "WHERE reversed = false " +
            "AND entity_type = 'Policy' " +
            "AND request_name = 'PeriodicPayout' " +
            "AND EXTRACT(YEAR FROM trans_eff_date) IN (2025) " +
            "ORDER BY trans_eff_date DESC",
            nativeQuery = true)
    List<Object[]> findCustomPayoutDeathClaimTransactions();*/

    @Query(value = "SELECT message_image, gross_amt, trans_eff_date, trans_run_date " +
            "FROM public.\"TRANSACTION_HISTORY\" " +
            "WHERE reversed = false " +
            "AND entity_type = 'Policy' " +
            "AND request_name = 'PeriodicPayout' " +
            "AND trans_eff_date > '2024-07-15' " +
            "ORDER BY trans_eff_date DESC", // Ensure correct spacing here
            nativeQuery = true)
    List<Object[]> findCustomPayoutDeathClaimTransactions();

        @Query(value = "SELECT message_image, gross_amt, trans_eff_date, trans_run_date " +
                "FROM public.\"TRANSACTION_HISTORY\" " +
                "WHERE reversed = false " +
                "AND entity_type = 'Policy' " +
                "AND request_name = 'PeriodicPayout' " +
                "AND EXTRACT(YEAR FROM trans_eff_date) IN (2025) " +
                "ORDER BY trans_eff_date DESC " +
                "LIMIT :limit OFFSET :offset",
                nativeQuery = true)
        List<Object[]> findCustomPayoutDeathClaimTransactions(@Param("limit") int limit, @Param("offset") int offset);

    // This method is for paginated results
/*    @Query(value = "SELECT message_image, gross_amt, trans_eff_date, trans_run_date " +
            "FROM public.\"TRANSACTION_HISTORY\" " +
            "WHERE reversed = false " +
            "AND entity_type = 'Policy' " +
            "AND request_name = 'PeriodicPayout' " +
            "AND EXTRACT(YEAR FROM trans_eff_date) IN (2025) " +
            "ORDER BY trans_eff_date DESC " +
            "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<Object[]> findCustomPayoutDeathClaimTransactionsPaginated(@Param("limit") int limit, @Param("offset") int offset);*/

    @Query(value = "SELECT message_image, gross_amt, trans_eff_date, trans_run_date " +
            "FROM public.\"TRANSACTION_HISTORY\" " +
            "WHERE reversed = false " +
            "AND entity_type = 'Policy' " +
            "AND request_name = 'PeriodicPayout' " +
            "AND trans_eff_date > '2024-07-15' " +
            "ORDER BY trans_eff_date DESC " +  // Added space here
            "LIMIT :limit OFFSET :offset",      // Added space before LIMIT
            nativeQuery = true)
    List<Object[]> findCustomPayoutDeathClaimTransactionsPaginated(@Param("limit") int limit, @Param("offset") int offset);

}




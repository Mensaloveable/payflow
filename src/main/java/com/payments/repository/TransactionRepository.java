package com.payments.repository;

import com.payments.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Transaction entities.
 * Backed by PostgreSQL — all queries run against the transactions table.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    // Recent transactions for dashboard (newest first)
    Page<Transaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Filter by payment method
    Page<Transaction> findByPaymentMethodOrderByCreatedAtDesc(String paymentMethod, Pageable pageable);

    // Filter by status
    Page<Transaction> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    // Filter by success flag
    List<Transaction> findTop20BySuccessOrderByCreatedAtDesc(boolean success);

    // Stats queries
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.success = true")
    long countSuccessful();

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.success = false")
    long countFailed();

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.success = true")
    BigDecimal totalVolume();

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.success = true AND t.currency = :currency")
    BigDecimal totalVolumeByCurrency(@Param("currency") String currency);

    @Query("""
        SELECT t.paymentMethod, COUNT(t), SUM(CASE WHEN t.success THEN 1 ELSE 0 END)
        FROM Transaction t
        GROUP BY t.paymentMethod
        ORDER BY COUNT(t) DESC
    """)
    List<Object[]> countByPaymentMethod();

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.createdAt >= :since
        ORDER BY t.createdAt DESC
    """)
    List<Transaction> findRecentSince(@Param("since") ZonedDateTime since);

    // Search by recipient
    List<Transaction> findByRecipientContainingIgnoreCaseOrderByCreatedAtDesc(String recipient);
}

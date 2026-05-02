package com.payments.service;

import com.payments.cache.RateLimiterService;
import com.payments.entity.Transaction;
import com.payments.model.PaymentRequest;
import com.payments.model.PaymentResult;
import com.payments.model.PaymentStats;
import com.payments.repository.TransactionRepository;
import com.payments.util.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * PaymentService — orchestrates the full payment lifecycle:
 *
 *  1. Rate limit check (Redis sliding window counter)
 *  2. Strategy execution (via PaymentContext)
 *  3. Persist result to PostgreSQL (TransactionRepository)
 *  4. Evict stale cache entries from Redis
 *  5. Return result to caller
 *
 * Reads use Redis caching (@Cacheable) with per-cache TTLs
 * defined in RedisConfig. Writes evict affected caches immediately.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentContext        paymentContext;
    private final TransactionRepository transactionRepository;
    private final RateLimiterService    rateLimiterService;

    // ─── Write path ───────────────────────────────────────────

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "payment-stats",       allEntries = true),
        @CacheEvict(value = "recent-transactions", allEntries = true),
        @CacheEvict(value = "method-stats",        allEntries = true)
    })
    public PaymentResult processPayment(PaymentRequest request, String clientIdentifier) {
        rateLimiterService.checkRateLimit(clientIdentifier);

        log.info(
                "Processing {} payment of {} {} for '{}'",
                request.getPaymentMethod(), request.getAmount(),
                request.getCurrency(), request.getRecipient()
        );

        PaymentResult result = paymentContext.executePayment(request);

        Transaction tx = Transaction.builder()
                .transactionId(result.getTransactionId())
                .paymentMethod(result.getPaymentMethod())
                .status(result.getStatus())
                .success(result.isSuccess())
                .amount(BigDecimal.valueOf(result.getAmount()))
                .currency(result.getCurrency())
                .recipient(result.getRecipient())
                .description(request.getDescription())
                .message(result.getMessage())
                .processorDetails(result.getProcessorDetails())
                .build();

        transactionRepository.save(tx);
        log.info("Transaction persisted [{}]: {}", result.getTransactionId(), result.getStatus());

        return result;
    }

    // ─── Read path (Redis cached) ─────────────────────────────

    @Cacheable(value = "recent-transactions", key = "'page:' + #page + ':size:' + #size")
    public PageResponse<Transaction> getRecentTransactions(int page, int size) {
        Page<Transaction> trxPage = transactionRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        return PageResponse.from(trxPage);
    }
//    @Cacheable(value = "recent-transactions", key = "'page:' + #page + ':size:' + #size")
//    public Page<Transaction> getRecentTransactions(int page, int size) {
//        return transactionRepository.findAllByOrderByCreatedAtDesc(
//                PageRequest.of(page, size, Sort.by("createdAt").descending()));
//    }

    @Cacheable(value = "transaction-detail", key = "#transactionId")
    public Optional<Transaction> getByTransactionId(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId);
    }

    @Cacheable(value = "payment-stats", key = "'global'")
    public PaymentStats getStats() {
        long total      = transactionRepository.count();
        long successful = transactionRepository.countSuccessful();
        long failed     = transactionRepository.countFailed();
        BigDecimal vol  = transactionRepository.totalVolume();
        return PaymentStats.of(total, successful, failed, vol);
    }

    @Cacheable(value = "method-stats", key = "'breakdown'")
    public List<Object[]> getMethodBreakdown() {
        return transactionRepository.countByPaymentMethod();
    }

    public int getRemainingQuota(String clientIdentifier) {
        return rateLimiterService.getRemainingRequests(clientIdentifier);
    }
}

package com.payments.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DTO for dashboard stats — serialized to Redis as a cache value.
 * Must implement Serializable for Redis object serialization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStats implements Serializable {

    private long totalTransactions;
    private long successfulTransactions;
    private long failedTransactions;
    private BigDecimal totalVolume;
    private double successRate;

    public static PaymentStats of(long total, long successful, long failed, BigDecimal volume) {
        double rate = total == 0 ? 0.0
                : BigDecimal.valueOf(successful)
                            .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue();
        return PaymentStats.builder()
                .totalTransactions(total)
                .successfulTransactions(successful)
                .failedTransactions(failed)
                .totalVolume(volume == null ? BigDecimal.ZERO : volume)
                .successRate(rate)
                .build();
    }
}

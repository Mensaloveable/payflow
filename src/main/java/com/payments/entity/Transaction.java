package com.payments.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JPA Entity — persisted to the `transactions` table in PostgreSQL.
 * Immutable after creation (only status/updatedAt can change).
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 64)
    private String transactionId;

    @Column(name = "payment_method", nullable = false, length = 32)
    private String paymentMethod;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 8)
    private String currency;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "description")
    private String description;

    @Column(name = "message", length = 512)
    private String message;

    @Column(name = "processor_details", columnDefinition = "TEXT")
    private String processorDetails;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    /** Convenience formatter for the UI layer */
    @Transient
    @JsonIgnore
    public String getFormattedCreatedAt() {
        if (createdAt == null) return "—";
        return createdAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss"));
    }
}

package com.payments.model;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Serializable result DTO — can be cached in Redis and returned to the UI.
 */
@Data
@Builder
public class PaymentResult implements Serializable {

    private String transactionId;
    private boolean success;
    private String message;
    private String paymentMethod;
    private Double amount;
    private String currency;
    private String recipient;
    private String processorDetails;
    private String timestamp;
    private String status;

    public static PaymentResult success(PaymentMethod method, PaymentRequest request, String details) {
        return PaymentResult.builder()
                .transactionId("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .success(true)
                .message("Payment processed successfully")
                .paymentMethod(method.name().replace("_", " "))
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .recipient(request.getRecipient())
                .processorDetails(details)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")))
                .status("COMPLETED")
                .build();
    }

    public static PaymentResult failure(PaymentMethod method, PaymentRequest request, String reason) {
        return PaymentResult.builder()
                .transactionId("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .success(false)
                .message("Payment failed: " + reason)
                .paymentMethod(method.name().replace("_", " "))
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .recipient(request.getRecipient())
                .processorDetails(reason)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")))
                .status("FAILED")
                .build();
    }
}

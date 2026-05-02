package com.payments.model;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private Double amount;

    @NotBlank(message = "Currency is required")
    private String currency = "USD";

    @NotBlank(message = "Recipient is required")
    private String recipient;

    private String description;

    // Credit Card fields
    private String cardNumber;
    private String cardHolder;
    private String expiryDate;
    private String cvv;

    // PayPal fields
    private String paypalEmail;

    // Crypto fields
    private String walletAddress;
    private String cryptoCurrency;

    // Bank Transfer fields
    private String bankAccountNumber;
    private String bankRoutingNumber;
    private String bankName;

    // USSD fields
    private String phoneNumber;
    private String networkProvider;
}

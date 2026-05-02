package com.payments.strategy;

import com.payments.model.PaymentMethod;
import com.payments.model.PaymentRequest;
import com.payments.model.PaymentResult;
import org.springframework.stereotype.Component;

/**
 * Concrete Strategy: Credit Card Payment
 * Handles card validation, authorization, and charge processing.
 */
@Component
public class CreditCardPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResult process(PaymentRequest request) {
        String validationError = validate(request);
        if (validationError != null) {
            return PaymentResult.failure(getPaymentMethod(), request, validationError);
        }

        // Simulate credit card processing pipeline
        String maskedCard = maskCardNumber(request.getCardNumber());
        String details = String.format(
            "Card charged: %s | Holder: %s | Network: %s | Auth Code: AUTH-%s",
            maskedCard,
            request.getCardHolder(),
            detectNetwork(request.getCardNumber()),
            generateAuthCode()
        );

        return PaymentResult.success(getPaymentMethod(), request, details);
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.CREDIT_CARD;
    }

    @Override
    public String validate(PaymentRequest request) {
        if (request.getCardNumber() == null || request.getCardNumber().isBlank()) {
            return "Card number is required";
        }
        if (request.getCardNumber().replaceAll("\\s", "").length() < 13) {
            return "Invalid card number";
        }
        if (request.getCardHolder() == null || request.getCardHolder().isBlank()) {
            return "Cardholder name is required";
        }
        if (request.getExpiryDate() == null || request.getExpiryDate().isBlank()) {
            return "Expiry date is required";
        }
        if (request.getCvv() == null || request.getCvv().length() < 3) {
            return "Invalid CVV";
        }
        return null;
    }

    private String maskCardNumber(String cardNumber) {
        String cleaned = cardNumber.replaceAll("\\s", "");
        return "**** **** **** " + cleaned.substring(cleaned.length() - 4);
    }

    private String detectNetwork(String cardNumber) {
        String first = cardNumber.trim().substring(0, 1);
        return switch (first) {
            case "4" -> "VISA";
            case "5" -> "Mastercard";
            case "3" -> "American Express";
            case "6" -> "Discover";
            default -> "Unknown Network";
        };
    }

    private String generateAuthCode() {
        return String.valueOf((int)(Math.random() * 900000) + 100000);
    }
}

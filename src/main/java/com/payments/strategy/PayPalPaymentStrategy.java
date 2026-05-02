package com.payments.strategy;

import com.payments.model.PaymentMethod;
import com.payments.model.PaymentRequest;
import com.payments.model.PaymentResult;
import org.springframework.stereotype.Component;

/**
 * Concrete Strategy: PayPal Payment
 * Handles PayPal OAuth token flow and payment capture simulation.
 */
@Component
public class PayPalPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResult process(PaymentRequest request) {
        String validationError = validate(request);
        if (validationError != null) {
            return PaymentResult.failure(getPaymentMethod(), request, validationError);
        }

        String details = String.format(
            "PayPal account: %s | Order ID: PP-%s | Capture ID: CAP-%s | Status: CAPTURED",
            request.getPaypalEmail(),
            generateId(8),
            generateId(10)
        );

        return PaymentResult.success(getPaymentMethod(), request, details);
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.PAYPAL;
    }

    @Override
    public String validate(PaymentRequest request) {
        if (request.getPaypalEmail() == null || request.getPaypalEmail().isBlank()) {
            return "PayPal email is required";
        }
        if (!request.getPaypalEmail().contains("@")) {
            return "Invalid PayPal email address";
        }
        return null;
    }

    private String generateId(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt((int)(Math.random() * chars.length())));
        }
        return sb.toString();
    }
}

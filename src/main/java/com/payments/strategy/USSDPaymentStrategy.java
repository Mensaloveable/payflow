package com.payments.strategy;

import com.payments.model.PaymentMethod;
import com.payments.model.PaymentRequest;
import com.payments.model.PaymentResult;
import org.springframework.stereotype.Component;

/**
 * Concrete Strategy: USSD Mobile Money Payment
 * Simulates USSD dial-up session, PIN confirmation, and mobile money transfer.
 */
@Component
public class USSDPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResult process(PaymentRequest request) {
        String validationError = validate(request);
        if (validationError != null) {
            return PaymentResult.failure(getPaymentMethod(), request, validationError);
        }

        String ussdCode = buildUssdCode(request.getNetworkProvider());
        String details = String.format(
            "Network: %s | Phone: %s | USSD: %s | Session ID: SID-%s | Mobile Ref: MM-%s",
            request.getNetworkProvider(),
            maskPhone(request.getPhoneNumber()),
            ussdCode,
            generateSessionId(),
            generateRef()
        );

        return PaymentResult.success(getPaymentMethod(), request, details);
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.USSD;
    }

    @Override
    public String validate(PaymentRequest request) {
        if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
            return "Phone number is required";
        }
        String digitsOnly = request.getPhoneNumber().replaceAll("[^0-9]", "");
        if (digitsOnly.length() < 10) {
            return "Invalid phone number";
        }
        if (request.getNetworkProvider() == null || request.getNetworkProvider().isBlank()) {
            return "Network provider is required";
        }
        return null;
    }

    private String buildUssdCode(String provider) {
        return switch (provider.toUpperCase()) {
            case "MTN" -> "*165#";
            case "AIRTEL" -> "*185#";
            case "VODAFONE" -> "*110#";
            case "GLO" -> "*777#";
            default -> "*100#";
        };
    }

    private String maskPhone(String phone) {
        if (phone.length() <= 4) return "****";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }

    private String generateSessionId() {
        return String.valueOf((int)(Math.random() * 900000) + 100000);
    }

    private String generateRef() {
        return String.valueOf((int)(Math.random() * 90000000) + 10000000);
    }
}

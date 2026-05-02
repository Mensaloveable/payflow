package com.payments.strategy;

import com.payments.model.PaymentMethod;
import com.payments.model.PaymentRequest;
import com.payments.model.PaymentResult;
import org.springframework.stereotype.Component;

/**
 * Concrete Strategy: Bank Transfer (ACH/Wire)
 * <br/>
 * Simulates bank-to-bank wire transfer initiation and settlement.
 */
@Component
public class BankTransferPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResult process(PaymentRequest request) {
        String validationError = validate(request);
        if (validationError != null) {
            return PaymentResult.failure(getPaymentMethod(), request, validationError);
        }

        String maskedAccount = maskAccount(request.getBankAccountNumber());
        String details = String.format(
            "Bank: %s | Account: %s | Routing: %s | Wire Ref: WIRE-%s | ETA: 1-3 business days",
            request.getBankName(),
            maskedAccount,
            request.getBankRoutingNumber(),
            generateRef()
        );

        return PaymentResult.success(getPaymentMethod(), request, details);
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.BANK_TRANSFER;
    }

    @Override
    public String validate(PaymentRequest request) {
        if (request.getBankAccountNumber() == null || request.getBankAccountNumber().isBlank()) {
            return "Bank account number is required";
        }
        if (request.getBankRoutingNumber() == null || request.getBankRoutingNumber().length() != 9) {
            return "Routing number must be 9 digits";
        }
        if (request.getBankName() == null || request.getBankName().isBlank()) {
            return "Bank name is required";
        }
        return null;
    }

    private String maskAccount(String account) {
        if (account.length() <= 4) return "****";
        return "*".repeat(account.length() - 4) + account.substring(account.length() - 4);
    }

    private String generateRef() {
        return String.valueOf((long)(Math.random() * 9_000_000_000L) + 1_000_000_000L);
    }
}

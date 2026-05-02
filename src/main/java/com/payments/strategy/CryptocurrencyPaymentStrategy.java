package com.payments.strategy;

import com.payments.model.PaymentMethod;
import com.payments.model.PaymentRequest;
import com.payments.model.PaymentResult;
import org.springframework.stereotype.Component;

/**
 * Concrete Strategy: Cryptocurrency Payment
 * Simulates on-chain transaction broadcasting and block confirmation.
 */
@Component
public class CryptocurrencyPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResult process(PaymentRequest request) {
        String validationError = validate(request);
        if (validationError != null) {
            return PaymentResult.failure(getPaymentMethod(), request, validationError);
        }

        double convertedAmount = convertToFiat(request.getAmount(), request.getCryptoCurrency());

        String details = String.format(
            "Network: %s | Wallet: %s | Tx Hash: 0x%s | Gas Fee: ~$%.2f | Block: #%d | Confirmations: 12",
            request.getCryptoCurrency(),
            maskWallet(request.getWalletAddress()),
            generateTxHash(),
            calculateGasFee(request.getCryptoCurrency()),
            (int)(Math.random() * 1000000) + 18000000
        );

        return PaymentResult.success(getPaymentMethod(), request, details);
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.CRYPTOCURRENCY;
    }

    @Override
    public String validate(PaymentRequest request) {
        if (request.getWalletAddress() == null || request.getWalletAddress().isBlank()) {
            return "Wallet address is required";
        }
        if (request.getWalletAddress().length() < 26) {
            return "Invalid wallet address length";
        }
        if (request.getCryptoCurrency() == null || request.getCryptoCurrency().isBlank()) {
            return "Cryptocurrency type is required";
        }
        return null;
    }

    private String maskWallet(String address) {
        if (address.length() <= 12) return address;
        return address.substring(0, 6) + "..." + address.substring(address.length() - 6);
    }

    private String generateTxHash() {
        String chars = "0123456789abcdef";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            sb.append(chars.charAt((int)(Math.random() * chars.length())));
        }
        return sb.toString();
    }

    private double convertToFiat(double amount, String crypto) {
        return switch (crypto.toUpperCase()) {
            case "BTC" -> amount * 65000;
            case "ETH" -> amount * 3200;
            case "USDT", "USDC" -> amount;
            default -> amount;
        };
    }

    private double calculateGasFee(String crypto) {
        return switch (crypto.toUpperCase()) {
            case "ETH" -> 2.50 + Math.random() * 5;
            case "BTC" -> 1.20 + Math.random() * 3;
            default -> 0.10 + Math.random() * 0.5;
        };
    }
}

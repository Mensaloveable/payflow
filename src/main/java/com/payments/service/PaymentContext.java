package com.payments.service;

import com.payments.model.PaymentMethod;
import com.payments.model.PaymentRequest;
import com.payments.model.PaymentResult;
import com.payments.strategy.PaymentStrategy;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Payment Context — the heart of the Strategy Pattern.
 *
 * Holds a registry of all available PaymentStrategy implementations
 * (auto-discovered via Spring's DI), selects the appropriate strategy
 * at runtime based on the requested payment method, and delegates execution.
 *
 * The context itself is completely decoupled from any specific payment algorithm.
 * Adding a new payment method = add a new @Component strategy. Nothing else changes.
 */
@Getter
@Component
public class PaymentContext {

    private final Map<PaymentMethod, PaymentStrategy> strategyRegistry;

    /**
     * Spring injects all PaymentStrategy beans automatically.
     * This self-registering pattern means zero configuration.
     */
    public PaymentContext(List<PaymentStrategy> strategies) {
        this.strategyRegistry = strategies.stream()
                .collect(Collectors.toMap(PaymentStrategy::getPaymentMethod, s -> s));
    }

    /**
     * Resolve the strategy and delegate payment processing.
     */
    public PaymentResult executePayment(PaymentRequest request) {
        PaymentStrategy strategy = strategyRegistry.get(request.getPaymentMethod());

        if (strategy == null) {
            return PaymentResult.builder()
                    .success(false)
                    .message("Unsupported payment method: " + request.getPaymentMethod())
                    .status("REJECTED")
                    .paymentMethod(request.getPaymentMethod().name())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .recipient(request.getRecipient())
                    .build();
        }

        return strategy.process(request);
    }

}

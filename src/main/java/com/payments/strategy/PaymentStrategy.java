package com.payments.strategy;

import com.payments.model.PaymentMethod;
import com.payments.model.PaymentRequest;
import com.payments.model.PaymentResult;

/**
 * Strategy Interface — defines the contract every payment processor must fulfill.
 * Each concrete strategy encapsulates a distinct payment algorithm.
 */
public interface PaymentStrategy {

    /**
     * Process the payment according to this strategy's implementation.
     *
     * @param request the payment request containing all necessary details
     * @return a PaymentResult with transaction outcome
     */
    PaymentResult process(PaymentRequest request);

    /**
     * Returns the payment method this strategy handles.
     */
    PaymentMethod getPaymentMethod();

    /**
     * Validates the fields specific to this payment method.
     * Returns an error message, or null if valid.
     */
    String validate(PaymentRequest request);
}

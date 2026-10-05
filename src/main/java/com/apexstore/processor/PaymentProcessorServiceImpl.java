package com.apexstore.processor;

import com.apexstore.generated.PaymentGateway.AckResponse;
import com.apexstore.generated.PaymentGateway.AckStatus;
import java.util.concurrent.atomic.AtomicInteger;

public class PaymentProcessorServiceImpl implements IPaymentProcessorService {

    private final TransactionRepository repository;
    private final PaymentGatewayClient gatewayClient;
    
    private static final AtomicInteger transactionIdGenerator = new AtomicInteger(1000);

    public PaymentProcessorServiceImpl(TransactionRepository repository, PaymentGatewayClient gatewayClient) {
        this.repository = repository;
        this.gatewayClient = gatewayClient;
    }

    @Override
    public AckResponse beginOrderPayment(String orderId, String method, String amount, String currency) {
        int transactionId = transactionIdGenerator.getAndIncrement();

        repository.persistInitialTransaction(transactionId, orderId, method, amount, currency);

        AckResponse ack = gatewayClient.processPayment(transactionId, method, amount, currency);

        if (ack.status == AckStatus.REJECTED) {
            repository.updateTransactionResult(transactionId, "FAILED", "", ack.rejectedCause);
        }

        return ack;
    }
}
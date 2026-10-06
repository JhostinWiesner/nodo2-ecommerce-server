package com.apexstore.processor;

import com.apexstore.generated.PaymentGateway.AckResponse;
import com.apexstore.generated.PaymentGateway.AckStatus;
import java.util.concurrent.atomic.AtomicInteger;

public class PaymentProcessorServiceImpl implements IPaymentProcessorService {

    private final TransactionPersistenceClient persistenceClient;
    private final PaymentGatewayClient gatewayClient;
    
    private static final AtomicInteger transactionIdGenerator = new AtomicInteger(1000);

    public PaymentProcessorServiceImpl(TransactionPersistenceClient persistenceClient, PaymentGatewayClient gatewayClient) {
        this.persistenceClient = persistenceClient;
        this.gatewayClient = gatewayClient;
    }

    @Override
    public AckResponse payOrder(String orderId, String method, String amount, String currency) {
        int transactionId = transactionIdGenerator.getAndIncrement();

        persistenceClient.persistPgTransaction(transactionId, orderId, method, amount, currency);

        AckResponse ack = gatewayClient.processPayment(transactionId, method, amount, currency);

        if (ack.status == AckStatus.REJECTED) {
            persistenceClient.updateTransactionResult(transactionId, "FAILED", "", ack.rejectedCause);
        }

        return ack;
    }
}
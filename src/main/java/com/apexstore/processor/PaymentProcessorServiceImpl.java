package com.apexstore.processor;

import com.apexstore.generated.PaymentGateway.AckResponse;
import com.apexstore.generated.PaymentGateway.AckStatus;

public class PaymentProcessorServiceImpl implements IPaymentProcessorService {

    private final TransactionPersistenceClient persistenceClient;
    private final PaymentGatewayClient gatewayClient;


    public PaymentProcessorServiceImpl(TransactionPersistenceClient persistenceClient, PaymentGatewayClient gatewayClient) {
        this.persistenceClient = persistenceClient;
        this.gatewayClient = gatewayClient;
    }

    @Override
    public AckResponse payOrder(String orderId, String method, String amount, String currency) {

        // 1. Registrar primero la transacción.
        int transactionId = persistenceClient.persistPgTransaction(orderId, method, amount, currency);

        try {
            // 2. Solicitar procesamiento a la pasarela.
            AckResponse ack = gatewayClient.processPayment(transactionId, method, amount, currency);

            // 3. Rechazo de negocio inmediato.
            if (ack.status == AckStatus.REJECTED) {
                persistenceClient.updateTransactionResult(transactionId, "FAILED", ack.externalRef, ack.rejectedCause);
            }

            // ACCEPTED:
            // queda PENDIENTE hasta recibir callback.

            return ack;

        } catch (Exception e) {

            System.err.println("[PaymentProcessor] Error comunicando con la pasarela. " + "TxID: " + transactionId + " | " + e.getMessage());

            /*
            * NO marcamos FAILED.
            *
            * La comunicación falló, pero no sabemos si el gateway
            * alcanzó a procesar el pago.
            *
            * La transacción permanece PENDIENTE.
            */
            throw e;
        }
    }
}
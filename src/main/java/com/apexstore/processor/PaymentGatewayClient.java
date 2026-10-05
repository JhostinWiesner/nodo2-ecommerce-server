package com.apexstore.processor;

import com.apexstore.generated.PaymentGateway.AckResponse;
import com.apexstore.generated.PaymentGateway.AckStatus;
import com.apexstore.generated.PaymentGateway.PaymentGatewayServicePrx;
import com.apexstore.generated.PaymentGateway.PaymentOrder;

public class PaymentGatewayClient {

    private final PaymentGatewayServicePrx gatewayProxy;

    public PaymentGatewayClient(PaymentGatewayServicePrx gatewayProxy) {
        this.gatewayProxy = gatewayProxy;
    }

    public AckResponse processPayment(int transactionId, String method, String amount, String currency) {
        PaymentOrder order = new PaymentOrder(transactionId, method, amount, currency);

        System.out.println("[Procesador -> Pasarela] Invocando beginGatewayPayment para TxID " + transactionId);
        AckResponse ack = gatewayProxy.beginGatewayPayment(order);

        if (ack.status == AckStatus.ACCEPTED) {
            System.out.println("[Procesador] Solicitud ACEPTADA por la pasarela. Ref: " + ack.externalRef);
        } else {
            System.err.println("[Procesador] Solicitud RECHAZADA por la pasarela. Causa: " + ack.rejectedCause);
        }

        return ack;
    }
}
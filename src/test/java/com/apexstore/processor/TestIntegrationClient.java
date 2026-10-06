package com.apexstore.processor;

import org.junit.jupiter.api.Test;
import com.apexstore.generated.PaymentGateway.AckResponse;
import com.apexstore.generated.PaymentGateway.PaymentGatewayServicePrx;
import com.apexstore.generated.TransactionPersistence.TransactionPersistenceServicePrx;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

public class TestIntegrationClient {

    @Test
    public void testIntegrationWithGateway() {
        System.out.println("=== INICIANDO PRUEBA DE INTEGRACIÓN CON PASARELA REAL (NODO 3) ===");

        ProcessorConfig config = ProcessorConfig.load();

        String gatewayHost = config.value("gateway.host", "GATEWAY_HOST");
        int gatewayPort = Integer.parseInt(config.value("gateway.port", "GATEWAY_PORT"));
        String gatewayServant = config.value("gateway.servant", "GATEWAY_SERVANT");
        String persistenceHost = config.value("persistence.host", "PERSISTENCE_HOST");
        int persistencePort = Integer.parseInt(config.value("persistence.port", "PERSISTENCE_PORT"));
        String persistenceServant = config.value("persistence.servant", "PERSISTENCE_SERVANT");

        int transactionId = (int) (System.currentTimeMillis() % 100000);
        String orderId = "ORD-REAL-001";
        String method = "STRIPE";
        String amount = "250.50";
        String currency = "USD";

        try (Communicator communicator = Util.initialize(new String[0])) {
            String persistenceProxyString = String.format(
                "%s:default -h %s -p %d",
                persistenceServant, persistenceHost, persistencePort);
            ObjectPrx persistenceBaseProxy = communicator.stringToProxy(persistenceProxyString);
            TransactionPersistenceServicePrx persistenceProxy =
                TransactionPersistenceServicePrx.checkedCast(persistenceBaseProxy);
            if (persistenceProxy == null) {
                throw new IllegalStateException(
                    "El objeto remoto no implementa TransactionPersistenceService: "
                        + persistenceProxyString
                );
            }

            System.out.println("\n1. Guardando transacción mediante el nodo de persistencia...");
            TransactionPersistenceClient persistenceClient =
                new TransactionPersistenceClient(persistenceProxy);
            persistenceClient.persistPgTransaction(
                transactionId, orderId, method, amount, currency);

            String proxyString = String.format("%s:default -h %s -p %d",
                gatewayServant, gatewayHost, gatewayPort);
            System.out.println("2. Conectando con la Pasarela en: " + proxyString);
            ObjectPrx baseProxy = communicator.stringToProxy(proxyString);
            PaymentGatewayServicePrx gatewayProxy = PaymentGatewayServicePrx.checkedCast(baseProxy);
            if (gatewayProxy == null) {
                throw new IllegalStateException(
                    "El objeto remoto no implementa PaymentGatewayService: " + proxyString
                );
            }

            PaymentGatewayClient gatewayClient = new PaymentGatewayClient(gatewayProxy);

            System.out.println("3. Enviando petición beginGatewayPayment...");
            AckResponse ack = gatewayClient.processPayment(transactionId, method, amount, currency);

            System.out.println("\n--- RESPUESTA SÍNCRONA DE LA PASARELA ---");
            System.out.println("Estado Acuse: " + ack.status);
            System.out.println("Ref Externa: " + ack.externalRef);
            System.out.println("Causa Rechazo: " + ack.rejectedCause);
            System.out.println("----------------------------------------");

        } catch (Exception e) {
            throw new AssertionError(
                "No se pudo comunicar con los nodos de persistencia o pasarela",
                e
            );
        }
    }
}
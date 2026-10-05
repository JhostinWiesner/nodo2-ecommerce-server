package com.apexstore.processor;

import org.junit.jupiter.api.Test;
import com.apexstore.generated.PaymentGateway.AckResponse;
import com.apexstore.generated.PaymentGateway.PaymentGatewayServicePrx;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

public class TestIntegrationClient {

    @Test
    public void testIntegrationWithGateway() {
        System.out.println("=== INICIANDO PRUEBA DE INTEGRACIÓN CON PASARELA REAL (NODO 3) ===");

        ProcessorConfig config = ProcessorConfig.load();
        String dbUrl = config.value("db.url", "DB_URL");
        String dbUser = config.value("db.user", "DB_USER");
        String dbPassword = config.value("db.password", "DB_PASSWORD");

        TransactionRepository repository = new TransactionRepository(dbUrl, dbUser, dbPassword);

        String gatewayHost = config.value("gateway.host", "GATEWAY_HOST");
        int gatewayPort = Integer.parseInt(config.value("gateway.port", "GATEWAY_PORT"));
        String gatewayServant = config.value("gateway.servant", "GATEWAY_SERVANT");

        int transactionId = (int) (System.currentTimeMillis() % 100000);
        String orderId = "ORD-REAL-001";
        String method = "STRIPE";
        String amount = "250.50";
        String currency = "USD";

        System.out.println("\n1. Guardando transacción en PostgreSQL local (PENDIENTE)...");
        repository.persistInitialTransaction(transactionId, orderId, method, amount, currency);

        String proxyString = String.format("%s:default -h %s -p %d",
            gatewayServant, gatewayHost, gatewayPort);
        System.out.println("2. Conectando con la Pasarela en: " + proxyString);

        try (Communicator communicator = Util.initialize(new String[0])) {
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
                "No se pudo comunicar con el nodo de pasarela usando " + proxyString,
                e
            );
        }
    }
}
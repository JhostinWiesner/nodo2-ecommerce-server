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

        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/apexstore_db");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");

        TransactionRepository repository = new TransactionRepository(dbUrl, dbUser, dbPassword);

        // IP de ZeroTier de tu compañero
        String gatewayZeroTierIp = "10.232.215.211"; 
        int gatewayPort = 10000;

        int transactionId = (int) (System.currentTimeMillis() % 100000);
        String orderId = "ORD-REAL-001";
        String method = "STRIPE";
        String amount = "250.50";
        String currency = "USD";

        System.out.println("\n1. Guardando transacción en PostgreSQL local (PENDIENTE)...");
        repository.persistInitialTransaction(transactionId, orderId, method, amount, currency);

        String proxyString = String.format("PaymentGatewayServant:default -h %s -p %d", gatewayZeroTierIp, gatewayPort);
        System.out.println("2. Conectando con la Pasarela en: " + proxyString);

        try (Communicator communicator = Util.initialize(new String[0])) {
            ObjectPrx baseProxy = communicator.stringToProxy(proxyString);
            PaymentGatewayServicePrx gatewayProxy = PaymentGatewayServicePrx.uncheckedCast(baseProxy);

            PaymentGatewayClient gatewayClient = new PaymentGatewayClient(gatewayProxy);

            System.out.println("3. Enviando petición beginGatewayPayment...");
            AckResponse ack = gatewayClient.processPayment(transactionId, method, amount, currency);

            System.out.println("\n--- RESPUESTA SÍNCRONA DE LA PASARELA ---");
            System.out.println("Estado Acuse: " + ack.status);
            System.out.println("Ref Externa: " + ack.externalRef);
            System.out.println("Causa Rechazo: " + ack.rejectedCause);
            System.out.println("----------------------------------------");

        } catch (Exception e) {
            System.err.println("\n[ERROR DE CONEXIÓN] No se pudo comunicar con el Nodo 3: " + e.getMessage());
        }
    }
}
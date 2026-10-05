package com.apexstore.processor;

import com.apexstore.generated.PaymentGateway.PaymentGatewayServicePrx;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

public class ProcessorMain {

    public static void main(String[] args) {
        // 1. Configuración de PostgreSQL desde variables de entorno
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/apexstore_db");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");

        // 2. Configuración de Red/Endpoints para la Pasarela (Nodo 3) y Callback
        String gatewayIp = "10.232.215.211";
        String gatewayPort = "10000";
        String callbackPort = "10001";

        TransactionRepository repository = new TransactionRepository(dbUrl, dbUser, dbPassword);

        try (Communicator communicator = Util.initialize(args)) {
            // 3. Iniciar servidor ICE para escuchar Callbacks (Puerto 10001 por defecto)
            String callbackEndpoints = String.format("default -p %s", callbackPort);
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                "ProcessorCallbackAdapter", 
                callbackEndpoints
            );

            TransactionNotifierI servant = new TransactionNotifierI(repository);
            adapter.add(servant, Util.stringToIdentity("TransactionNotifierService"));
            adapter.activate();

            System.out.println("=== PaymentProcessor activo ===");
            System.out.println("Escuchando callbacks en el puerto: " + callbackPort);

            // 4. Crear Proxy cliente hacia PaymentGateway (Nodo 3)
            String proxyString = String.format("PaymentGatewayServant:default -h %s -p %s", gatewayIp, gatewayPort);
            System.out.println("Configurado proxy hacia Pasarela en: " + proxyString);

            ObjectPrx baseProxy = communicator.stringToProxy(proxyString);
            PaymentGatewayServicePrx gatewayProxy = PaymentGatewayServicePrx.uncheckedCast(baseProxy);

            PaymentGatewayClient gatewayClient = new PaymentGatewayClient(gatewayProxy);

            // Mantener el proceso en ejecución
            communicator.waitForShutdown();
        }
    }
}
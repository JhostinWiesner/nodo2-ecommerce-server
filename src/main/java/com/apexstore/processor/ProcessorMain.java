package com.apexstore.processor;

import com.apexstore.generated.PaymentGateway.PaymentGatewayServicePrx;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

public class ProcessorMain {

    public static void main(String[] args) {
        // Conexión a la base de datos PostgreSQL local
        TransactionRepository repository = new TransactionRepository(
            "jdbc:postgresql://localhost:5432/apexstore_db",
            "postgres",
            "postgres"
        );

        try (Communicator communicator = Util.initialize(args)) {
            // 1. Iniciar servidor ICE para escuchar Callbacks en puerto 10001
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                "ProcessorCallbackAdapter", 
                "default -p 10001"
            );

            TransactionNotifierI servant = new TransactionNotifierI(repository);
            adapter.add(servant, Util.stringToIdentity("TransactionNotifierService"));
            adapter.activate();

            System.out.println("=== PaymentProcessor activo ===");
            System.out.println("Escuchando callbacks en el puerto 10001...");

            // 2. Conectar Proxy cliente hacia PaymentGateway (Nodo 3 en puerto 10000)
            // (Cambiar 'localhost' por la IP de ZeroTier de la pasarela durante las pruebas en red)
            ObjectPrx baseProxy = communicator.stringToProxy("PaymentGatewayAdapter:default -h localhost -p 10000");
            PaymentGatewayServicePrx gatewayProxy = PaymentGatewayServicePrx.checkedCast(baseProxy);

            PaymentGatewayClient gatewayClient = new PaymentGatewayClient(gatewayProxy);

            // Mantener el servidor activo
            communicator.waitForShutdown();
        }
    }
}
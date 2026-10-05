package com.apexstore.processor;

import com.apexstore.generated.PaymentGateway.PaymentGatewayServicePrx;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

public class ProcessorMain {

    public static void main(String[] args) {
        ProcessorConfig config = ProcessorConfig.load();

        String dbUrl = config.value("db.url", "DB_URL");
        String dbUser = config.value("db.user", "DB_USER");
        String dbPassword = config.value("db.password", "DB_PASSWORD");
        String gatewayIp = config.value("gateway.host", "GATEWAY_HOST");
        String gatewayPort = config.value("gateway.port", "GATEWAY_PORT");
        String callbackPort = config.value("processor.callback.port", "CALLBACK_PORT");
        String gatewayServant = config.value("gateway.servant", "GATEWAY_SERVANT");
        String callbackServant = config.value("processor.callback.servant", "CALLBACK_SERVANT");

        TransactionRepository repository = new TransactionRepository(dbUrl, dbUser, dbPassword);

        try (Communicator communicator = Util.initialize(args)) {
            // Iniciar servidor ICE para escuchar Callbacks (Puerto 10001 por defecto)
            String callbackEndpoints = String.format("default -p %s", callbackPort);
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                "ProcessorCallbackAdapter", 
                callbackEndpoints
            );

            TransactionNotifierI servant = new TransactionNotifierI(repository);
            adapter.add(servant, Util.stringToIdentity(callbackServant));
            adapter.activate();

            System.out.println("=== PaymentProcessor activo ===");
            System.out.println("Escuchando callbacks en el puerto: " + callbackPort);

            // Crear Proxy cliente hacia PaymentGateway (Nodo 3)
            String proxyString = String.format(
                "%s:default -h %s -p %s", gatewayServant, gatewayIp, gatewayPort
            );
            System.out.println("Configurado proxy hacia Pasarela en: " + proxyString);

            ObjectPrx baseProxy = communicator.stringToProxy(proxyString);
            PaymentGatewayServicePrx gatewayProxy = PaymentGatewayServicePrx.checkedCast(baseProxy);
            if (gatewayProxy == null) {
                throw new IllegalStateException(
                    "El objeto remoto no implementa PaymentGatewayService: " + proxyString
                );
            }

            PaymentGatewayClient gatewayClient = new PaymentGatewayClient(gatewayProxy);

            // Mantener el proceso en ejecución
            communicator.waitForShutdown();
        }
    }
}
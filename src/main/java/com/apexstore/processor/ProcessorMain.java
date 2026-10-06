package com.apexstore.processor;

import com.apexstore.generated.PaymentGateway.PaymentGatewayServicePrx;
import com.apexstore.checkout.CheckoutServer;
import com.apexstore.generated.TransactionPersistence.TransactionPersistenceServicePrx;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

public class ProcessorMain {

    public static void main(String[] args) {
        ProcessorConfig config = ProcessorConfig.load();
        String persistenceIp = config.value("persistence.host", "PERSISTENCE_HOST");
        String persistencePort = config.value("persistence.port", "PERSISTENCE_PORT");
        String persistenceServant = config.value("persistence.servant", "PERSISTENCE_SERVANT");
        
        String gatewayIp = config.value("gateway.host", "GATEWAY_HOST");
        String gatewayPort = config.value("gateway.port", "GATEWAY_PORT");
        String gatewayServant = config.value("gateway.servant", "GATEWAY_SERVANT");
        
        String callbackServant = config.value("processor.callback.servant", "CALLBACK_SERVANT");
        String callbackPort = config.value("processor.callback.port", "CALLBACK_PORT");

        try (Communicator communicator = Util.initialize(args)) {
            // Iniciar servidor ICE para escuchar Callbacks (Puerto 10001 por defecto)
            String callbackEndpoints = String.format("default -p %s", callbackPort);
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                "ProcessorCallbackAdapter", 
                callbackEndpoints
            );

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

            // Crear Proxy cliente hacia TransactionPersistence (Nodo 4)
            String persistenceProxyString = String.format(
                "%s:default -h %s -p %s",
                persistenceServant,
                persistenceIp,
                persistencePort
            );

            ObjectPrx persistenceBaseProxy =
                communicator.stringToProxy(persistenceProxyString);

            TransactionPersistenceServicePrx persistenceProxy =
                TransactionPersistenceServicePrx.checkedCast(
                    persistenceBaseProxy
                );

            if (persistenceProxy == null) {
                throw new IllegalStateException(
                    "El objeto remoto no implementa " +
                    "TransactionPersistenceService: "
                    + persistenceProxyString
                );
            }

            // Clientes Ice
            PaymentGatewayClient gatewayClient = new PaymentGatewayClient(gatewayProxy);
            TransactionPersistenceClient persistenceClient =
                new TransactionPersistenceClient(persistenceProxy);

            // Servant de callback
            TransactionNotifierI servant = new TransactionNotifierI(persistenceClient);
            adapter.add(servant, Util.stringToIdentity(callbackServant));
            adapter.activate();

            // PaymentProcessor

            PaymentProcessorServiceImpl paymentProcessorService =
                new PaymentProcessorServiceImpl(persistenceClient, gatewayClient);
            
            System.out.println("=== PaymentProcessor activo ===");
            System.out.println("Escuchando callbacks en el puerto: " + callbackPort);
            System.out.println("Servicio local PaymentProcessor listo para CheckoutService");
            System.out.println("Configurado proxy hacia Persistencia en: " + persistenceProxyString);

            // Mantener el proceso en ejecución
            communicator.waitForShutdown();
        }
    }
}
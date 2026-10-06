package com.apexstore.checkout;

import com.apexstore.generated.Checkout.CheckoutRequest;
import com.apexstore.generated.Checkout.CheckoutResponse;
import com.apexstore.generated.Checkout.CheckoutServicePrx;
import com.apexstore.generated.Checkout.CheckoutUnavailable;
import com.apexstore.generated.Checkout.InvalidCheckoutRequest;
import com.apexstore.generated.Checkout.PaymentMethod;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Util;

/**
 * Cliente de prueba: hace el papel de la WebApp / MobileApp llamando a CheckoutService por Ice.
 * Requiere que ProcessorMain esté corriendo (puerto 10002).
 */
public final class CheckoutClientDemo {

    public static void main(String[] args) {
        // try-with-resources: el Communicator se cierra solo al terminar
        try (Communicator communicator = Util.initialize(args)) {

            // "Dirección" del servicio: identidad @ endpoint (TCP, host, puerto)
            var base = communicator.stringToProxy("CheckoutService:default -h localhost -p 10002");
            CheckoutServicePrx checkout = CheckoutServicePrx.checkedCast(base);
            if (checkout == null) {
                System.err.println("El objeto remoto no es un CheckoutService");
                return;
            }

            // Caso 1: compra válida
            String orderId = "ORD-" + System.currentTimeMillis();
            send(checkout, new CheckoutRequest(orderId, PaymentMethod.STRIPE, "250.50", "USD"));

            // Caso 2: monto inválido -> debe fallar con InvalidCheckoutRequest
            send(checkout, new CheckoutRequest(orderId + "-B", PaymentMethod.PSE, "-5", "COP"));
        }
    }

    private static void send(CheckoutServicePrx checkout, CheckoutRequest request) {
        System.out.println("-> Enviando orden " + request.orderId);
        try {
            CheckoutResponse r = checkout.checkout(request);
            System.out.println("<- Estado=" + r.status + " referencia='" + r.gatewayReference
                    + "' motivoRechazo='" + r.rejectionReason + "'");
        } catch (InvalidCheckoutRequest e) {
            System.out.println("<- Solicitud inválida: campo=" + e.field + ", motivo=" + e.reason);
        } catch (CheckoutUnavailable e) {
            System.out.println("<- Servicio no disponible: " + e.reason);
        }
    }
}

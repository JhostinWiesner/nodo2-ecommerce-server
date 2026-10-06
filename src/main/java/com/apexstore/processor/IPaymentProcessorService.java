package com.apexstore.processor;

import com.apexstore.generated.PaymentGateway.AckResponse;

/**
 * Interfaz provista por PaymentProcessor y requerida por CheckoutService (beginOrderPayment)
 */
public interface IPaymentProcessorService {

    /**
     * Inicia la orquestación del pago para una orden de compra.
     *
     * @param orderId  Identificador único de la orden en Checkout
     * @param method   Método de pago seleccionado ("STRIPE", "PSE", "CRYPTO")
     * @param amount   Monto total en formato texto decimal (ej. "250.50")
     * @param currency Moneda de la transacción (ej. "USD", "COP")
     * @return AckResponse Acuse de recibo síncrono que indica si la pasarela aceptó o rechazó la solicitud inicial
     */
    AckResponse payOrder(String orderId, String method, String amount, String currency);
}
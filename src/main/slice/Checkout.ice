#pragma once

// Mismo prefijo de paquete que PaymentGateway.ice -> com.apexstore.generated.Checkout
// (si tu PaymentGateway.ice usa otro mecanismo para el prefijo, replícalo aquí)
[["java:package:com.apexstore.generated"]]

module Checkout
{
    // Valores exactos que espera IPaymentProcessorService.beginOrderPayment
    enum PaymentMethod { STRIPE, PSE, CRYPTO }

    enum CheckoutStatus { ACCEPTED, REJECTED }

    struct CheckoutRequest
    {
        string orderId;
        PaymentMethod method;
        string amount;      // decimal en texto, p. ej. "250.50"
        string currency;    // ISO 4217, p. ej. "COP"
    }

    struct CheckoutResponse
    {
        string orderId;
        CheckoutStatus status;
        string gatewayReference;  // vacío si fue rechazado
        string rejectionReason;   // vacío si fue aceptado
    }

    exception InvalidCheckoutRequest
    {
        string field;
        string reason;
    }

    exception CheckoutUnavailable
    {
        string reason;
    }

    interface CheckoutService
    {
        CheckoutResponse checkout(CheckoutRequest request)
            throws InvalidCheckoutRequest, CheckoutUnavailable;
    }
}

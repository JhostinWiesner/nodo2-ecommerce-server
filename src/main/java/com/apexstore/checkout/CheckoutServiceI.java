package com.apexstore.checkout;

import com.apexstore.generated.Checkout.CheckoutRequest;
import com.apexstore.generated.Checkout.CheckoutResponse;
import com.apexstore.generated.Checkout.CheckoutService;
import com.apexstore.generated.Checkout.CheckoutStatus;
import com.apexstore.generated.Checkout.CheckoutUnavailable;
import com.apexstore.generated.Checkout.InvalidCheckoutRequest;
import com.apexstore.generated.PaymentGateway.AckResponse;
import com.apexstore.generated.PaymentGateway.AckStatus;
import com.apexstore.processor.IPaymentProcessorService;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.LocalException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;

/** Servant de CheckoutService: valida la solicitud y delega el pago en PaymentProcessor. */
public final class CheckoutServiceI implements CheckoutService {

    private static final Pattern ISO_CURRENCY = Pattern.compile("[A-Z]{3}");
    private static final int MAX_DECIMALS = 2;

    private final IPaymentProcessorService paymentProcessor;

    public CheckoutServiceI(IPaymentProcessorService paymentProcessor) {
        this.paymentProcessor = Objects.requireNonNull(paymentProcessor, "paymentProcessor");
    }

    @Override
    public CheckoutResponse checkout(CheckoutRequest request, Current current)
            throws InvalidCheckoutRequest, CheckoutUnavailable {

        if (request == null) {
            throw invalid("request", "es obligatoria");
        }
        String orderId = requireText(request.orderId, "orderId");
        if (request.method == null) {
            throw invalid("method", "es obligatorio");
        }
        String amount = normalizeAmount(request.amount);
        if (request.currency == null || !ISO_CURRENCY.matcher(request.currency).matches()) {
            throw invalid("currency", "debe ser un código ISO 4217 de 3 letras mayúsculas");
        }

        final AckResponse ack;
        try {
            ack = paymentProcessor.payOrder(orderId, request.method.name(), amount, request.currency);
        } catch (LocalException e) {          // fallo de comunicación Ice con la pasarela
            System.err.println("Checkout de la orden " + orderId + " falló: " + e);
            throw new CheckoutUnavailable("La pasarela de pagos no está disponible");
        }

        boolean accepted = ack.status == AckStatus.ACCEPTED;
        // AJUSTAR: 'reference' según el nombre real del campo en AckResponse (PaymentGateway.ice)
        return new CheckoutResponse(
                orderId,
                accepted ? CheckoutStatus.ACCEPTED : CheckoutStatus.REJECTED,
                Objects.requireNonNullElse(ack.externalRef, ""),
                Objects.requireNonNullElse(ack.rejectedCause, ""));
    }

    private static String requireText(String value, String field) throws InvalidCheckoutRequest {
        if (value == null || value.isBlank()) {
            throw invalid(field, "es obligatorio");
        }
        return value.strip();
    }

    /** Valida el monto y lo devuelve en notación plana (sin exponente), p. ej. "250.50". */
    private static String normalizeAmount(String raw) throws InvalidCheckoutRequest {
        if (raw == null || raw.isBlank()) {
            throw invalid("amount", "es obligatorio");
        }
        final BigDecimal amount;
        try {
            amount = new BigDecimal(raw.strip());
        } catch (NumberFormatException e) {
            throw invalid("amount", "no es un número decimal válido");
        }
        if (amount.signum() <= 0) {
            throw invalid("amount", "debe ser mayor que cero");
        }
        if (amount.stripTrailingZeros().scale() > MAX_DECIMALS) {
            throw invalid("amount", "admite máximo " + MAX_DECIMALS + " decimales");
        }
        return amount.toPlainString();
    }

    private static InvalidCheckoutRequest invalid(String field, String reason) {
        return new InvalidCheckoutRequest(field, reason);
    }
}

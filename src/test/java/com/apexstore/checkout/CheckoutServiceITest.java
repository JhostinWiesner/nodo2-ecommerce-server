package com.apexstore.checkout;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.apexstore.generated.Checkout.CheckoutRequest;
import com.apexstore.generated.Checkout.CheckoutResponse;
import com.apexstore.generated.Checkout.CheckoutStatus;
import com.apexstore.generated.Checkout.CheckoutUnavailable;
import com.apexstore.generated.Checkout.InvalidCheckoutRequest;
import com.apexstore.generated.Checkout.PaymentMethod;
import com.apexstore.generated.PaymentGateway.AckResponse;
import com.apexstore.generated.PaymentGateway.AckStatus;
import com.apexstore.processor.IPaymentProcessorService;
import com.zeroc.Ice.ConnectFailedException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Pruebas del servant sin red ni Ice en ejecución: el PaymentProcessor es un doble de prueba. */
class CheckoutServiceITest {

    private static AckResponse ack(AckStatus status, String cause) {
        var ack = new AckResponse();   // constructor por defecto generado por Ice
        ack.status = status;
        ack.rejectedCause = cause;
        return ack;
    }

    @Test
    void validOrderIsDelegatedAndAccepted() throws Exception {
        var received = new AtomicReference<String[]>();
        IPaymentProcessorService processor = (orderId, method, amount, currency) -> {
            received.set(new String[] {orderId, method, amount, currency});
            return ack(AckStatus.ACCEPTED, "");
        };
        var service = new CheckoutServiceI(processor);

        CheckoutResponse response =
                service.checkout(new CheckoutRequest("ORD-1", PaymentMethod.STRIPE, "250.50", "USD"), null);

        assertEquals(CheckoutStatus.ACCEPTED, response.status);
        assertEquals("ORD-1", response.orderId);
        assertArrayEquals(new String[] {"ORD-1", "STRIPE", "250.50", "USD"}, received.get());
    }

    @Test
    void gatewayRejectionIsReportedAsRejected() throws Exception {
        var service = new CheckoutServiceI((o, m, a, c) -> ack(AckStatus.REJECTED, "fondos insuficientes"));

        CheckoutResponse response =
                service.checkout(new CheckoutRequest("ORD-2", PaymentMethod.PSE, "10", "COP"), null);

        assertEquals(CheckoutStatus.REJECTED, response.status);
        assertEquals("fondos insuficientes", response.rejectionReason);
    }

    @Test
    void invalidAmountIsRejectedBeforeCallingProcessor() {
        var called = new AtomicReference<>(false);
        var service = new CheckoutServiceI((o, m, a, c) -> {
            called.set(true);
            return ack(AckStatus.ACCEPTED, "");
        });

        var e = assertThrows(InvalidCheckoutRequest.class,
                () -> service.checkout(new CheckoutRequest("ORD-3", PaymentMethod.CRYPTO, "-5", "USD"), null));

        assertEquals("amount", e.field);
        assertFalse(called.get());
    }

    @Test
    void iceCommunicationFailureBecomesCheckoutUnavailable() {
        var service = new CheckoutServiceI((o, m, a, c) -> {
            throw new ConnectFailedException();
        });

        assertThrows(CheckoutUnavailable.class,
                () -> service.checkout(new CheckoutRequest("ORD-4", PaymentMethod.STRIPE, "1", "USD"), null));
    }
}

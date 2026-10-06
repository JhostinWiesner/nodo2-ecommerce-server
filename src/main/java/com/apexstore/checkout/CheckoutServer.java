package com.apexstore.checkout;

import com.apexstore.processor.IPaymentProcessorService;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Identity;
import com.zeroc.Ice.ObjectAdapter;

/** Registro del servant de CheckoutService en un Communicator existente. */
public final class CheckoutServer {

    public static final String ADAPTER_NAME = "CheckoutAdapter";
    public static final String IDENTITY = "CheckoutService";
    public static final String DEFAULT_ENDPOINTS = "default -p 10002";

    private CheckoutServer() {
    }

    public static ObjectAdapter register(Communicator communicator,
                                         IPaymentProcessorService paymentProcessor,
                                         String endpoints) {
        ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(ADAPTER_NAME, endpoints);
        adapter.add(new CheckoutServiceI(paymentProcessor), new Identity(IDENTITY, ""));
        adapter.activate();
        return adapter;
    }
}

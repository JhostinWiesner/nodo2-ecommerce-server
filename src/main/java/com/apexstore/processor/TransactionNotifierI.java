package com.apexstore.processor;

import com.apexstore.generated.PaymentGateway.PaymentStatus;
import com.apexstore.generated.PaymentGateway.ResultNotification;
import com.apexstore.generated.PaymentGateway.TransactionNotifier;
import com.zeroc.Ice.Current;

public class TransactionNotifierI implements TransactionNotifier {
    private final TransactionRepository repository;

    public TransactionNotifierI(TransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void notifyTransactionResult(ResultNotification result, Current current) {
        String statusStr = (result.status == PaymentStatus.CONFIRMED) ? "CONFIRMED" : "FAILED";

        System.out.println("\n[CALLBACK RECIBIDO] TxID: " + result.transactionId + " | Estado: " + statusStr);

        // Se envía externalRef y failureCause a sus columnas correspondientes en la BD
        repository.updateTransactionResult(
            result.transactionId, 
            statusStr, 
            result.externalRef, 
            result.failureCause
        );
    }
}

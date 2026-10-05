package com.apexstore.processor;

import com.apexstore.generated.PaymentGateway.PaymentStatus;
import com.apexstore.generated.PaymentGateway.ResultNotification;
import com.apexstore.generated.PaymentGateway._TransactionNotifierDisp;
import com.zeroc.Ice.Current;

public class TransactionNotifierI extends _TransactionNotifierDisp {
    private final TransactionRepository repository;

    public TransactionNotifierI(TransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void notifyTransactionResult(ResultNotification result, Current current) {
        String statusStr = (result.status == PaymentStatus.CONFIRMED) ? "CONFIRMED" : "FAILED";
        String detail = (result.status == PaymentStatus.CONFIRMED) 
                ? "Ref Externa: " + result.externalRef 
                : "Causa Falla: " + result.failureCause;

        System.out.println("\n [CALLBACK RECIBIDO] TxID: " + result.transactionId + " | Estado: " + statusStr);

        // Actualizar el estado en PostgreSQL (Nodo 4)
        repository.updateTransactionResult(result.transactionId, statusStr, detail);
    }
}

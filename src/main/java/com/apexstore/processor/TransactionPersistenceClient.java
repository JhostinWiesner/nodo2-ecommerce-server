package com.apexstore.processor;

import com.apexstore.generated.TransactionPersistence.TransactionPersistenceServicePrx;

public class TransactionPersistenceClient {

    private final TransactionPersistenceServicePrx persistenceProxy;

    public TransactionPersistenceClient(
            TransactionPersistenceServicePrx persistenceProxy) {

        this.persistenceProxy = persistenceProxy;
    }

    public void persistPgTransaction(
            int transactionId,
            String orderId,
            String method,
            String amount,
            String currency) {

        persistenceProxy.persistPgTransaction(
            transactionId,
            orderId,
            method,
            amount,
            currency
        );
    }

    public void updateTransactionResult(
            int transactionId,
            String status,
            String externalRef,
            String detail) {

        persistenceProxy.updateTransactionResult(
            transactionId,
            status,
            externalRef,
            detail
        );
    }
}
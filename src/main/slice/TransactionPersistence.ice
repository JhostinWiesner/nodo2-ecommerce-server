[["java:package:com.apexstore.generated"]]

module TransactionPersistence
{
    interface TransactionPersistenceService
    {
        void persistPgTransaction(
            int transactionId,
            string orderId,
            string method,
            string amount,
            string currency
        );

        void updateTransactionResult(
            int transactionId,
            string status,
            string externalRef,
            string detail
        );
    };
};
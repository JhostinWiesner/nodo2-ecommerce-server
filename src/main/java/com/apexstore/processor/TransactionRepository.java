package com.apexstore.processor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TransactionRepository {
    private final String dbUrl;
    private final String user;
    private final String password;

    public TransactionRepository(String dbUrl, String user, String password) {
        this.dbUrl = dbUrl;
        this.user = user;
        this.password = password;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, user, password);
    }

    // Registra la transacción inicial en estado PENDIENTE
    public void persistInitialTransaction(int transactionId, String orderId, String method, String amount, String currency) {
        String sql = "INSERT INTO transacciones (id_transaccion, id_orden, metodo_pago, monto, moneda, estado) " +
                     "VALUES (?, ?, ?, ?::numeric, ?, 'PENDIENTE') " +
                     "ON CONFLICT (id_transaccion) DO NOTHING";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, transactionId);
            stmt.setString(2, orderId);
            stmt.setString(3, method);
            stmt.setString(4, amount);
            stmt.setString(5, currency);

            stmt.executeUpdate();
            System.out.println("[DB] Transacción registrada (PENDIENTE): TxID " + transactionId);

        } catch (SQLException e) {
            System.err.println("[DB Error] Error al guardar transacción inicial: " + e.getMessage());
        }
    }

    // Actualiza el estado cuando llega el Callback asincrónico
    public void updateTransactionResult(int transactionId, String status, String externalRef, String detail) {
        String sql = "UPDATE transacciones SET estado = ?, referencia_externa = ?, detalle_respuesta = ? WHERE id_transaccion = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setString(2, externalRef);
            stmt.setString(3, detail);
            stmt.setInt(4, transactionId);

            stmt.executeUpdate();
            System.out.println("[DB] Transacción actualizada vía Callback: TxID " + transactionId + " -> " + status);

        } catch (SQLException e) {
            System.err.println("[DB Error] Error al actualizar estado de transacción: " + e.getMessage());
        }
    }
}

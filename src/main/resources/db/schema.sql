-- Tabla de Productos (para consulta desde el CLI y verificación de stock)
CREATE TABLE IF NOT EXISTS productos (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    precio NUMERIC(10, 2) NOT NULL,
    stock INT NOT NULL CHECK (stock >= 0)
);

-- Tabla de Transacciones (persistida por el servicio persistPgTransaction)
CREATE TABLE IF NOT EXISTS transacciones (
    id_transaccion VARCHAR(64) PRIMARY KEY,
    id_orden VARCHAR(64) NOT NULL,
    metodo_pago VARCHAR(30) NOT NULL,
    monto NUMERIC(10, 2) NOT NULL,
    estado VARCHAR(20) NOT NULL, -- 'PENDIENTE', 'APROBADO', 'RECHAZADO'
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    detalle_respuesta TEXT
);


INSERT INTO productos (nombre, precio, stock) VALUES
('Apple MacBook Air M5', 1099.00, 10),
('iPhone 18 Pro Max', 1299.00, 25),
('Sony WF-1000XM6', 299.99, 50),
('Asus ROG Swift PG27AQDP', 699.00, 15);
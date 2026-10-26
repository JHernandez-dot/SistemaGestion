
package com.example.sistemagestion.controller;

import com.example.sistemagestion.controller.config.ConexionDB;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Set;

public class VentaDAO {

    private static final Set<String> METODOS_PERMITIDOS =
            Set.of("EFECTIVO", "TARJETA", "TRANSFERENCIA");

    public record ItemVenta(long productoId, BigDecimal cantidad) {}

    public record PagoVenta(
            String metodo,
            BigDecimal monto,
            String referencia
    ) {}

    public record ResultadoVenta(
            long ventaId,
            long numeroVenta,
            BigDecimal subtotal,
            BigDecimal total
    ) {}

    /*
     * Compatibilidad con el controlador anterior.
     * Mientras se conecta la interfaz de pagos, las llamadas antiguas
     * registran el total como efectivo.
     */
    public ResultadoVenta registrarVenta(
            long cajeroId,
            List<ItemVenta> items,
            String observaciones
    ) throws SQLException {
        return registrarVentaInterna(
                cajeroId, items, observaciones, null
        );
    }

    /*
     * Nuevo método: permite registrar uno o varios métodos de pago.
     * Para pago mixto, enviar una lista con un registro por método.
     */
    public ResultadoVenta registrarVenta(
            long cajeroId,
            List<ItemVenta> items,
            String observaciones,
            List<PagoVenta> pagos
    ) throws SQLException {
        if (pagos == null || pagos.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debes indicar al menos un método de pago."
            );
        }

        return registrarVentaInterna(
                cajeroId, items, observaciones, pagos
        );
    }

    private ResultadoVenta registrarVentaInterna(
            long cajeroId,
            List<ItemVenta> items,
            String observaciones,
            List<PagoVenta> pagosSolicitados
    ) throws SQLException {

        if (cajeroId <= 0) {
            throw new IllegalArgumentException("El cajero no es válido.");
        }

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException(
                    "Agrega al menos un producto a la venta."
            );
        }

        try (Connection cn = ConexionDB.conectar()) {
            cn.setAutoCommit(false);

            try {
                long turnoId = obtenerTurnoAbierto(cn, cajeroId);

                BigDecimal subtotal = BigDecimal.ZERO;
                BigDecimal descuento = BigDecimal.ZERO;

                // Validar productos y bloquear sus filas durante la venta.
                for (ItemVenta item : items) {
                    validarItem(item);

                    BigDecimal precio = obtenerPrecioYValidarStock(
                            cn, item.productoId(), item.cantidad()
                    );

                    BigDecimal importe = precio
                            .multiply(item.cantidad())
                            .setScale(2, RoundingMode.HALF_UP);

                    subtotal = subtotal.add(importe);
                }

                subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
                BigDecimal total = subtotal.subtract(descuento)
                        .setScale(2, RoundingMode.HALF_UP);

                List<PagoVenta> pagos = prepararPagos(
                        pagosSolicitados, total
                );

                String sqlVenta = """
                    INSERT INTO ventas (
                        turno_id, cajero_id, subtotal,
                        descuento, total, estado, observaciones
                    )
                    VALUES (?, ?, ?, ?, ?, 'COMPLETADA', ?)
                    RETURNING venta_id, numero_venta
                    """;

                long ventaId;
                long numeroVenta;

                try (PreparedStatement ps = cn.prepareStatement(sqlVenta)) {
                    ps.setLong(1, turnoId);
                    ps.setLong(2, cajeroId);
                    ps.setBigDecimal(3, subtotal);
                    ps.setBigDecimal(4, descuento);
                    ps.setBigDecimal(5, total);

                    if (observaciones == null || observaciones.isBlank()) {
                        ps.setNull(6, Types.VARCHAR);
                    } else {
                        ps.setString(6, observaciones.trim());
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "No se pudo generar el número de venta."
                            );
                        }

                        ventaId = rs.getLong("venta_id");
                        numeroVenta = rs.getLong("numero_venta");
                    }
                }

                String sqlDetalle = """
                    INSERT INTO detalle_venta (
                        venta_id, producto_id, cantidad,
                        precio_unitario, descuento, subtotal
                    )
                    VALUES (?, ?, ?, ?, 0, ?)
                    """;

                String sqlInventario = """
                    UPDATE productos
                    SET existencia = existencia - ?
                    WHERE producto_id = ?
                      AND activo = TRUE
                      AND existencia >= ?
                    """;

                for (ItemVenta item : items) {
                    BigDecimal precio = obtenerPrecioYValidarStock(
                            cn, item.productoId(), item.cantidad()
                    );

                    BigDecimal importe = precio
                            .multiply(item.cantidad())
                            .setScale(2, RoundingMode.HALF_UP);

                    try (PreparedStatement ps =
                                 cn.prepareStatement(sqlDetalle)) {
                        ps.setLong(1, ventaId);
                        ps.setLong(2, item.productoId());
                        ps.setBigDecimal(3, item.cantidad());
                        ps.setBigDecimal(4, precio);
                        ps.setBigDecimal(5, importe);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps =
                                 cn.prepareStatement(sqlInventario)) {
                        ps.setBigDecimal(1, item.cantidad());
                        ps.setLong(2, item.productoId());
                        ps.setBigDecimal(3, item.cantidad());

                        if (ps.executeUpdate() != 1) {
                            throw new SQLException(
                                    "No se pudo actualizar el inventario "
                                            + "del producto ID "
                                            + item.productoId()
                            );
                        }
                    }
                }

                // Los pagos se guardan en la misma transacción.
                String sqlPago = """
                    INSERT INTO pagos (
                        venta_id, metodo, monto, referencia, fecha
                    )
                    VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """;

                for (PagoVenta pago : pagos) {
                    try (PreparedStatement ps =
                                 cn.prepareStatement(sqlPago)) {
                        ps.setLong(1, ventaId);
                        ps.setString(2, pago.metodo());
                        ps.setBigDecimal(3, pago.monto());

                        if (pago.referencia() == null
                                || pago.referencia().isBlank()) {
                            ps.setNull(4, Types.VARCHAR);
                        } else {
                            ps.setString(4, pago.referencia().trim());
                        }

                        ps.executeUpdate();
                    }
                }

                cn.commit();

                return new ResultadoVenta(
                        ventaId, numeroVenta, subtotal, total
                );

            } catch (Exception e) {
                try {
                    cn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                if (e instanceof SQLException sqlException) {
                    throw sqlException;
                }

                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }

                throw new SQLException("No se pudo registrar la venta.", e);
            }
        }
    }

    private List<PagoVenta> prepararPagos(
            List<PagoVenta> pagosSolicitados,
            BigDecimal total
    ) throws SQLException {

        if (pagosSolicitados == null) {
            // Compatibilidad temporal con llamadas del controlador anterior.
            return List.of(new PagoVenta("EFECTIVO", total, null));
        }

        if (pagosSolicitados.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debes indicar al menos un método de pago."
            );
        }

        BigDecimal sumaPagos = BigDecimal.ZERO;

        for (PagoVenta pago : pagosSolicitados) {
            if (pago == null || pago.metodo() == null) {
                throw new IllegalArgumentException(
                        "Hay un método de pago no válido."
                );
            }

            String metodo = pago.metodo().trim().toUpperCase();

            if (!METODOS_PERMITIDOS.contains(metodo)) {
                throw new IllegalArgumentException(
                        "Método de pago no permitido: " + metodo
                );
            }

            if (pago.monto() == null
                    || pago.monto().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "El monto de cada pago debe ser mayor que cero."
                );
            }

            sumaPagos = sumaPagos.add(
                    pago.monto().setScale(2, RoundingMode.HALF_UP)
            );
        }

        sumaPagos = sumaPagos.setScale(2, RoundingMode.HALF_UP);

        if (sumaPagos.compareTo(total) != 0) {
            throw new IllegalArgumentException(
                    "Los pagos suman Q " + sumaPagos
                            + ", pero el total de la venta es Q " + total
                            + ". Los importes deben coincidir exactamente."
            );
        }

        return pagosSolicitados.stream()
                .map(p -> new PagoVenta(
                        p.metodo().trim().toUpperCase(),
                        p.monto().setScale(2, RoundingMode.HALF_UP),
                        p.referencia()
                ))
                .toList();
    }

    private void validarItem(ItemVenta item) {
        if (item == null || item.productoId() <= 0) {
            throw new IllegalArgumentException(
                    "Hay un producto no válido en la venta."
            );
        }

        if (item.cantidad() == null
                || item.cantidad().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Todas las cantidades deben ser mayores que cero."
            );
        }
    }

    private long obtenerTurnoAbierto(
            Connection cn,
            long cajeroId
    ) throws SQLException {

        String sql = """
            SELECT turno_id
            FROM turnos
            WHERE cajero_id = ?
              AND estado = 'ABIERTO'
              AND cierre IS NULL
            FOR UPDATE
            """;

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, cajeroId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("turno_id");
                }
            }
        }

        throw new SQLException(
                "El cajero no tiene un turno abierto. "
                        + "Abra la caja antes de vender."
        );
    }

    private BigDecimal obtenerPrecioYValidarStock(
            Connection cn,
            long productoId,
            BigDecimal cantidad
    ) throws SQLException {

        String sql = """
            SELECT precio_venta, existencia, activo
            FROM productos
            WHERE producto_id = ?
            FOR UPDATE
            """;

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, productoId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException(
                            "No existe el producto ID " + productoId
                    );
                }

                if (!rs.getBoolean("activo")) {
                    throw new SQLException(
                            "El producto ID " + productoId
                                    + " está desactivado."
                    );
                }

                BigDecimal existencia = rs.getBigDecimal("existencia");

                if (existencia.compareTo(cantidad) < 0) {
                    throw new SQLException(
                            "Existencias insuficientes para el producto ID "
                                    + productoId + ". Disponible: "
                                    + existencia + ", solicitado: " + cantidad
                    );
                }

                return rs.getBigDecimal("precio_venta");
            }
        }
    }
}

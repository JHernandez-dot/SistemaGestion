
package com.example.sistemagestion.controller;

import com.example.sistemagestion.controller.config.ConexionDB;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class ReversionDAO {

    public record SolicitudReversion(
            long reversionId,
            long detalleId,
            BigDecimal cantidad,
            BigDecimal monto,
            String estado
    ) {}

    public long solicitarReversion(
            long detalleId,
            BigDecimal cantidad,
            String motivo,
            long solicitadaPor
    ) throws SQLException {

        if (detalleId <= 0 || solicitadaPor <= 0) {
            throw new IllegalArgumentException("Los identificadores no son válidos.");
        }

        if (cantidad == null || cantidad.signum() <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad a revertir debe ser mayor que cero."
            );
        }

        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                    "Debes indicar el motivo de la reversión."
            );
        }

        try (Connection cn = ConexionDB.conectar()) {
            cn.setAutoCommit(false);

            try {
                BigDecimal disponible;
                BigDecimal precioUnitario;
                BigDecimal subtotalOriginal;
                long turnoId;

                String sqlDetalle = """
                    SELECT dv.cantidad,
                           COALESCE(dv.cantidad_revertida, 0)
                               AS cantidad_revertida,
                           dv.precio_unitario,
                           dv.subtotal,
                           v.turno_id
                    FROM detalle_venta dv
                    INNER JOIN ventas v
                        ON v.venta_id = dv.venta_id
                    WHERE dv.detalle_id = ?
                      AND v.estado = 'COMPLETADA'
                    FOR UPDATE OF dv
                    """;

                try (PreparedStatement ps =
                             cn.prepareStatement(sqlDetalle)) {
                    ps.setLong(1, detalleId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "No existe el detalle de venta "
                                            + "o la venta no está completada."
                            );
                        }

                        BigDecimal original = rs.getBigDecimal("cantidad");
                        BigDecimal revertida =
                                rs.getBigDecimal("cantidad_revertida");

                        disponible = original.subtract(revertida);
                        precioUnitario =
                                rs.getBigDecimal("precio_unitario");
                        subtotalOriginal = rs.getBigDecimal("subtotal");
                        turnoId = rs.getLong("turno_id");
                    }
                }

                if (cantidad.compareTo(disponible) > 0) {
                    throw new SQLException(
                            "La cantidad solicitada supera la cantidad "
                                    + "disponible para reversión: " + disponible
                    );
                }

                BigDecimal cantidadOriginal;
                String sqlCantidad = """
                    SELECT cantidad
                    FROM detalle_venta
                    WHERE detalle_id = ?
                    """;

                try (PreparedStatement ps =
                             cn.prepareStatement(sqlCantidad)) {
                    ps.setLong(1, detalleId);

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        cantidadOriginal = rs.getBigDecimal("cantidad");
                    }
                }

                BigDecimal monto;
                if (cantidadOriginal.signum() == 0) {
                    monto = BigDecimal.ZERO.setScale(2);
                } else {
                    monto = subtotalOriginal
                            .multiply(cantidad)
                            .divide(cantidadOriginal, 2, RoundingMode.HALF_UP);
                }

                String sqlInsert = """
                    INSERT INTO reversiones (
                        turno_id,
                        detalle_id,
                        cantidad,
                        monto,
                        motivo,
                        solicitada_por,
                        estado,
                        fecha
                    )
                    VALUES (?, ?, ?, ?, ?, ?, 'PENDIENTE',
                            CURRENT_TIMESTAMP)
                    RETURNING reversion_id
                    """;

                long reversionId;

                try (PreparedStatement ps =
                             cn.prepareStatement(sqlInsert)) {
                    ps.setLong(1, turnoId);
                    ps.setLong(2, detalleId);
                    ps.setBigDecimal(3, cantidad);
                    ps.setBigDecimal(4, monto);
                    ps.setString(5, motivo.trim());
                    ps.setLong(6, solicitadaPor);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "No se pudo registrar la solicitud."
                            );
                        }
                        reversionId = rs.getLong("reversion_id");
                    }
                }

                cn.commit();
                return reversionId;

            } catch (Exception e) {
                cn.rollback();

                if (e instanceof SQLException sqlException) {
                    throw sqlException;
                }
                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }

                throw new SQLException(
                        "No se pudo solicitar la reversión.", e
                );

            } finally {
                cn.setAutoCommit(true);
            }
        }
    }

    public void autorizarReversion(
            long reversionId,
            long autorizadoPor
    ) throws SQLException {

        if (reversionId <= 0 || autorizadoPor <= 0) {
            throw new IllegalArgumentException(
                    "Los identificadores no son válidos."
            );
        }

        try (Connection cn = ConexionDB.conectar()) {
            cn.setAutoCommit(false);

            try {
                validarPermisoReversion(cn, autorizadoPor);

                long detalleId;
                long productoId;
                BigDecimal cantidadReversion;
                BigDecimal cantidadOriginal;
                BigDecimal cantidadYaRevertida;

                String sqlSolicitud = """
                    SELECT r.detalle_id,
                           r.cantidad,
                           r.estado,
                           dv.producto_id,
                           dv.cantidad AS cantidad_original,
                           COALESCE(dv.cantidad_revertida, 0)
                               AS cantidad_revertida
                    FROM reversiones r
                    INNER JOIN detalle_venta dv
                        ON dv.detalle_id = r.detalle_id
                    WHERE r.reversion_id = ?
                    FOR UPDATE OF r, dv
                    """;

                try (PreparedStatement ps =
                             cn.prepareStatement(sqlSolicitud)) {
                    ps.setLong(1, reversionId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "No existe la solicitud de reversión."
                            );
                        }

                        if (!"PENDIENTE".equals(rs.getString("estado"))) {
                            throw new SQLException(
                                    "La solicitud ya fue procesada."
                            );
                        }

                        detalleId = rs.getLong("detalle_id");
                        cantidadReversion =
                                rs.getBigDecimal("cantidad");
                        productoId = rs.getLong("producto_id");
                        cantidadOriginal =
                                rs.getBigDecimal("cantidad_original");
                        cantidadYaRevertida =
                                rs.getBigDecimal("cantidad_revertida");
                    }
                }

                if (cantidadYaRevertida.add(cantidadReversion)
                        .compareTo(cantidadOriginal) > 0) {
                    throw new SQLException(
                            "La autorización supera la cantidad "
                                    + "que queda disponible para reversión."
                    );
                }

                String sqlActualizarDetalle = """
                    UPDATE detalle_venta
                    SET cantidad_revertida =
                        COALESCE(cantidad_revertida, 0) + ?
                    WHERE detalle_id = ?
                      AND COALESCE(cantidad_revertida, 0) + ?
                          <= cantidad
                    """;

                try (PreparedStatement ps =
                             cn.prepareStatement(sqlActualizarDetalle)) {
                    ps.setBigDecimal(1, cantidadReversion);
                    ps.setLong(2, detalleId);
                    ps.setBigDecimal(3, cantidadReversion);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "No se pudo actualizar la cantidad revertida."
                        );
                    }
                }

                String sqlInventario = """
                    UPDATE productos
                    SET existencia = existencia + ?
                    WHERE producto_id = ?
                    """;

                try (PreparedStatement ps =
                             cn.prepareStatement(sqlInventario)) {
                    ps.setBigDecimal(1, cantidadReversion);
                    ps.setLong(2, productoId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "No se pudo restituir el inventario."
                        );
                    }
                }

                String sqlAutorizar = """
                    UPDATE reversiones
                    SET estado = 'AUTORIZADA',
                        autorizada_por = ?
                    WHERE reversion_id = ?
                      AND estado = 'PENDIENTE'
                    """;

                try (PreparedStatement ps =
                             cn.prepareStatement(sqlAutorizar)) {
                    ps.setLong(1, autorizadoPor);
                    ps.setLong(2, reversionId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "No se pudo autorizar la reversión."
                        );
                    }
                }

                cn.commit();

            } catch (Exception e) {
                cn.rollback();

                if (e instanceof SQLException sqlException) {
                    throw sqlException;
                }
                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }

                throw new SQLException(
                        "No se pudo autorizar la reversión.", e
                );

            } finally {
                cn.setAutoCommit(true);
            }
        }
    }

    public void rechazarReversion(
            long reversionId,
            long autorizadoPor
    ) throws SQLException {

        try (Connection cn = ConexionDB.conectar()) {
            cn.setAutoCommit(false);

            try {
                validarPermisoReversion(cn, autorizadoPor);

                String sql = """
                    UPDATE reversiones
                    SET estado = 'RECHAZADA',
                        autorizada_por = ?
                    WHERE reversion_id = ?
                      AND estado = 'PENDIENTE'
                    """;

                try (PreparedStatement ps = cn.prepareStatement(sql)) {
                    ps.setLong(1, autorizadoPor);
                    ps.setLong(2, reversionId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "La solicitud no existe o ya fue procesada."
                        );
                    }
                }

                cn.commit();

            } catch (Exception e) {
                cn.rollback();

                if (e instanceof SQLException sqlException) {
                    throw sqlException;
                }
                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }

                throw new SQLException(
                        "No se pudo rechazar la reversión.", e
                );

            } finally {
                cn.setAutoCommit(true);
            }
        }
    }

    public record DetalleDisponible(
            long detalleId,
            String numeroVenta,
            String producto,
            BigDecimal cantidadDisponible,
            BigDecimal precioUnitario
    ) {
        @Override
        public String toString() {
            return "Venta " + numeroVenta + " | " + producto
                    + " | Disponible: " + cantidadDisponible;
        }
    }

    public record SolicitudPendiente(
            long reversionId,
            String numeroVenta,
            String producto,
            BigDecimal cantidad,
            BigDecimal monto,
            String motivo,
            String solicitante,
            java.time.LocalDateTime fecha
    ) {}

    public java.util.List<DetalleDisponible> listarDetallesDisponibles()
            throws SQLException {

        java.util.List<DetalleDisponible> lista = new java.util.ArrayList<>();

        String sql = """
        SELECT dv.detalle_id,
               v.numero_venta,
               p.nombre AS producto,
               dv.cantidad
                   - COALESCE(dv.cantidad_revertida, 0)
                   AS cantidad_disponible,
               dv.precio_unitario
        FROM detalle_venta dv
        INNER JOIN ventas v ON v.venta_id = dv.venta_id
        INNER JOIN productos p ON p.producto_id = dv.producto_id
        WHERE v.estado = 'COMPLETADA'
          AND dv.cantidad > COALESCE(dv.cantidad_revertida, 0)
        ORDER BY v.fecha DESC, dv.detalle_id DESC
        """;

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new DetalleDisponible(
                        rs.getLong("detalle_id"),
                        rs.getString("numero_venta"),
                        rs.getString("producto"),
                        rs.getBigDecimal("cantidad_disponible"),
                        rs.getBigDecimal("precio_unitario")
                ));
            }
        }

        return lista;
    }

    public java.util.List<SolicitudPendiente> listarSolicitudesPendientes()
            throws SQLException {

        java.util.List<SolicitudPendiente> lista =
                new java.util.ArrayList<>();

        String sql = """
        SELECT r.reversion_id,
               v.numero_venta,
               p.nombre AS producto,
               r.cantidad,
               r.monto,
               r.motivo,
               u.username AS solicitante,
               r.fecha
        FROM reversiones r
        INNER JOIN detalle_venta dv
            ON dv.detalle_id = r.detalle_id
        INNER JOIN ventas v
            ON v.venta_id = dv.venta_id
        INNER JOIN productos p
            ON p.producto_id = dv.producto_id
        INNER JOIN usuarios u
            ON u.usuario_id = r.solicitada_por
        WHERE r.estado = 'PENDIENTE'
        ORDER BY r.fecha ASC, r.reversion_id ASC
        """;

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                java.sql.Timestamp timestamp = rs.getTimestamp("fecha");

                lista.add(new SolicitudPendiente(
                        rs.getLong("reversion_id"),
                        rs.getString("numero_venta"),
                        rs.getString("producto"),
                        rs.getBigDecimal("cantidad"),
                        rs.getBigDecimal("monto"),
                        rs.getString("motivo"),
                        rs.getString("solicitante"),
                        timestamp != null
                                ? timestamp.toLocalDateTime()
                                : null
                ));
            }
        }

        return lista;
    }

    private void validarPermisoReversion(
            Connection cn,
            long usuarioId
    ) throws SQLException {

        String sql = """
            SELECT EXISTS (
                SELECT 1
                FROM usuario_roles ur
                INNER JOIN roles r
                    ON r.rol_id = ur.rol_id
                INNER JOIN rol_permisos rp
                    ON rp.rol_id = r.rol_id
                INNER JOIN permisos p
                    ON p.permiso_id = rp.permiso_id
                INNER JOIN usuarios u
                    ON u.usuario_id = ur.usuario_id
                WHERE ur.usuario_id = ?
                  AND u.activo = TRUE
                  AND r.activo = TRUE
                  AND p.codigo = 'REVERSAR_PRODUCTO'
                  AND r.nombre IN ('ADMINISTRADOR', 'SUPERVISOR')
            )
            """;

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !rs.getBoolean(1)) {
                    throw new SQLException(
                            "El usuario no tiene permiso para "
                                    + "autorizar o rechazar reversiones."
                    );
                }
            }
        }
    }
}


package com.example.sistemagestion.controller;

import com.example.sistemagestion.controller.config.ConexionDB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventarioDAO {

    private final ProductoDAO productoDAO = new ProductoDAO();

    public List<ProductoDAO.Producto> listarProductos()
            throws SQLException {
        return productoDAO.listar().stream()
                .filter(ProductoDAO.Producto::isActivo)
                .toList();
    }

    public void registrarMovimiento(
            long productoId,
            long usuarioId,
            String tipo,
            BigDecimal cantidad,
            String motivo) throws SQLException {

        if (cantidad == null) {
            throw new SQLException("Debes indicar una cantidad.");
        }

        if (motivo == null || motivo.isBlank()) {
            throw new SQLException("Debes indicar el motivo.");
        }

        if (!"ENTRADA".equals(tipo) && !"AJUSTE".equals(tipo)) {
            throw new SQLException("Tipo de movimiento no válido.");
        }

        // Una entrada debe ser positiva.
        // Un ajuste representa la existencia final y permite cero.
        if ("ENTRADA".equals(tipo) && cantidad.signum() <= 0) {
            throw new SQLException(
                    "La cantidad de entrada debe ser mayor que cero.");
        }

        if ("AJUSTE".equals(tipo) && cantidad.signum() < 0) {
            throw new SQLException(
                    "La existencia final no puede ser negativa.");
        }

        String consulta = """
            SELECT existencia
            FROM productos
            WHERE producto_id = ? AND activo = TRUE
            FOR UPDATE
            """;

        String actualizar = """
            UPDATE productos
            SET existencia = ?
            WHERE producto_id = ? AND activo = TRUE
            """;

        String insertar = """
            INSERT INTO inventario_movimientos (
                producto_id, usuario_id, tipo, cantidad,
                existencia_anterior, existencia_nueva, motivo
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection con = ConexionDB.conectar()) {
            con.setAutoCommit(false);

            try {
                BigDecimal anterior;

                try (PreparedStatement ps = con.prepareStatement(consulta)) {
                    ps.setLong(1, productoId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Producto inexistente o inactivo.");
                        }

                        anterior = rs.getBigDecimal("existencia");

                        if (anterior == null) {
                            anterior = BigDecimal.ZERO;
                        }
                    }
                }

                BigDecimal nueva;
                BigDecimal cantidadMovimiento;

                if ("ENTRADA".equals(tipo)) {
                    // Sumar las unidades recibidas al inventario actual.
                    nueva = anterior.add(cantidad);
                    cantidadMovimiento = cantidad;
                } else {
                    // AJUSTE: cantidad representa la existencia final.
                    nueva = cantidad;
                    cantidadMovimiento = nueva.subtract(anterior);
                }

                if (nueva.signum() < 0) {
                    throw new SQLException(
                            "La existencia no puede ser negativa.");
                }

                try (PreparedStatement ps =
                             con.prepareStatement(actualizar)) {
                    ps.setBigDecimal(1, nueva);
                    ps.setLong(2, productoId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "No se pudo actualizar la existencia.");
                    }
                }

                try (PreparedStatement ps =
                             con.prepareStatement(insertar)) {
                    ps.setLong(1, productoId);
                    ps.setLong(2, usuarioId);
                    ps.setString(3, tipo);
                    ps.setBigDecimal(4, cantidadMovimiento);
                    ps.setBigDecimal(5, anterior);
                    ps.setBigDecimal(6, nueva);
                    ps.setString(7, motivo.trim());

                    ps.executeUpdate();
                }

                con.commit();

            } catch (SQLException | RuntimeException e) {
                try {
                    con.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }

                throw e;
            }
        }
    }

    public List<String[]> listarMovimientos() throws SQLException {
        List<String[]> movimientos = new ArrayList<>();

        String sql = """
            SELECT m.movimiento_id, m.fecha, p.nombre,
                   m.tipo, m.cantidad, m.existencia_anterior,
                   m.existencia_nueva, m.motivo, m.usuario_id
            FROM inventario_movimientos m
            JOIN productos p
                ON p.producto_id = m.producto_id
            ORDER BY m.fecha DESC, m.movimiento_id DESC
            """;

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                movimientos.add(new String[]{
                        String.valueOf(rs.getLong("movimiento_id")),
                        rs.getTimestamp("fecha").toString(),
                        rs.getString("nombre"),
                        rs.getString("tipo"),
                        rs.getBigDecimal("cantidad").toPlainString(),
                        rs.getBigDecimal("existencia_anterior").toPlainString(),
                        rs.getBigDecimal("existencia_nueva").toPlainString(),
                        rs.getString("motivo"),
                        String.valueOf(rs.getLong("usuario_id"))
                });
            }
        }

        return movimientos;
    }
}

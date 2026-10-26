
package com.example.sistemagestion.controller;

import com.example.sistemagestion.controller.config.ConexionDB;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class TurnoDAO {

    public Long buscarTurnoAbierto(long cajaId) throws SQLException {
        String sql = """
            SELECT turno_id
            FROM turnos
            WHERE caja_id = ?
              AND estado = 'ABIERTO'
              AND cierre IS NULL
            ORDER BY apertura DESC
            LIMIT 1
            """;

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, cajaId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("turno_id") : null;
            }
        }
    }

    public long abrirTurno(
            long cajaId,
            long cajeroId,
            BigDecimal fondoInicial,
            String observaciones
    ) throws SQLException {

        if (fondoInicial == null
                || fondoInicial.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El fondo inicial debe ser mayor que cero."
            );
        }

        String sql = """
            INSERT INTO turnos (
                caja_id,
                cajero_id,
                fondo_inicial,
                estado,
                observaciones
            )
            SELECT ?, ?, ?, 'ABIERTO', ?
            WHERE EXISTS (
                SELECT 1
                FROM cajas
                WHERE caja_id = ?
                  AND activa = TRUE
            )
            AND NOT EXISTS (
                SELECT 1
                FROM turnos
                WHERE caja_id = ?
                  AND estado = 'ABIERTO'
                  AND cierre IS NULL
            )
            RETURNING turno_id
            """;

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, cajaId);
            ps.setLong(2, cajeroId);
            ps.setBigDecimal(3, fondoInicial);

            if (observaciones == null || observaciones.isBlank()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, observaciones.trim());
            }

            ps.setLong(5, cajaId);
            ps.setLong(6, cajaId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("turno_id");
                }
            }

            throw new SQLException(
                    "No se pudo abrir el turno. Verifica que la caja esté "
                            + "activa y que no tenga otro turno abierto."
            );
        }
    }

    public void cerrarTurno(
            long turnoId,
            BigDecimal efectivoDeclarado,
            String observaciones
    ) throws SQLException {

        if (efectivoDeclarado == null
                || efectivoDeclarado.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "El efectivo declarado no puede ser negativo."
            );
        }

        String efectivoEsperado = """
            (
                t.fondo_inicial
                + COALESCE((
                    SELECT SUM(
                        CASE
                            WHEN mc.tipo = 'INGRESO_EFECTIVO'
                                THEN mc.monto
                            WHEN mc.tipo = 'RETIRO_BOVEDA'
                                THEN -mc.monto
                            ELSE 0
                        END
                    )
                    FROM movimientos_caja mc
                    WHERE mc.turno_id = t.turno_id
                ), 0)
                + COALESCE((
                    SELECT SUM(p.monto)
                    FROM pagos p
                    INNER JOIN ventas v
                        ON v.venta_id = p.venta_id
                    WHERE v.turno_id = t.turno_id
                      AND v.estado = 'COMPLETADA'
                      AND p.metodo = 'EFECTIVO'
                ), 0)
            )
            """;

        String sql = """
            UPDATE turnos t
            SET efectivo_esperado = %s,
                efectivo_declarado = ?,
                diferencia = ? - %s,
                cierre = CURRENT_TIMESTAMP,
                estado = 'CERRADO',
                observaciones = COALESCE(?, t.observaciones)
            WHERE t.turno_id = ?
              AND t.estado = 'ABIERTO'
              AND t.cierre IS NULL
            """.formatted(efectivoEsperado, efectivoEsperado);

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setBigDecimal(1, efectivoDeclarado);
            ps.setBigDecimal(2, efectivoDeclarado);

            if (observaciones == null || observaciones.isBlank()) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, observaciones.trim());
            }

            ps.setLong(4, turnoId);

            if (ps.executeUpdate() != 1) {
                throw new SQLException(
                        "No se cerró el turno. Puede que ya esté cerrado "
                                + "o que no exista."
                );
            }
        }
    }

    public record CajaOption(long id, String codigo, String nombre) {
        @Override
        public String toString() {
            return codigo + " - " + nombre;
        }
    }

    public List<CajaOption> listarCajasActivas() throws SQLException {
        String sql = """
            SELECT caja_id, codigo, nombre
            FROM cajas
            WHERE activa = TRUE
            ORDER BY codigo
            """;

        List<CajaOption> cajas = new ArrayList<>();

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cajas.add(new CajaOption(
                        rs.getLong("caja_id"),
                        rs.getString("codigo"),
                        rs.getString("nombre")
                ));
            }
        }

        return cajas;
    }

    public String obtenerResumenTurno(long turnoId) throws SQLException {
        String sql = """
            SELECT t.turno_id, c.codigo, t.apertura,
                   t.fondo_inicial, t.estado
            FROM turnos t
            INNER JOIN cajas c ON c.caja_id = t.caja_id
            WHERE t.turno_id = ?
              AND t.estado = 'ABIERTO'
              AND t.cierre IS NULL
            """;

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, turnoId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return "Turno: " + rs.getLong("turno_id")
                            + "\nCaja: " + rs.getString("codigo")
                            + "\nApertura: " + rs.getTimestamp("apertura")
                            + "\nFondo inicial: Q "
                            + rs.getBigDecimal("fondo_inicial")
                            + "\nEstado: " + rs.getString("estado");
                }
            }
        }

        return "No hay información de un turno abierto.";
    }
}

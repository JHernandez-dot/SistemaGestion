package com.example.sistemagestion.controller.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class UsuarioDAO {

    public static class UsuarioAutenticado {
        private final long usuarioId;
        private final String username;
        private final String nombreCompleto;
        private final boolean activo;
        private final String rol;

        public UsuarioAutenticado(
                long usuarioId,
                String username,
                String nombreCompleto,
                boolean activo,
                String rol) {
            this.usuarioId = usuarioId;
            this.username = username;
            this.nombreCompleto = nombreCompleto;
            this.activo = activo;
            this.rol = rol;
        }

        public long getUsuarioId() {
            return usuarioId;
        }

        public String getUsername() {
            return username;
        }

        public String getNombreCompleto() {
            return nombreCompleto;
        }

        public boolean isActivo() {
            return activo;
        }

        public String getRol() {
            return rol;
        }
    }

    public UsuarioAutenticado buscarPorUsername(String username)
            throws SQLException {


        String sql = """
    SELECT
        u.usuario_id,
        u.username,
        u.nombre_completo,
        u.activo,
        u.intentos_fallidos,
        u.bloqueado_hasta,
        r.nombre AS rol
    FROM usuarios u
    INNER JOIN usuario_roles ur
        ON ur.usuario_id = u.usuario_id
    INNER JOIN roles r
        ON r.rol_id = ur.rol_id
    WHERE u.username = ?
      AND u.activo = TRUE
      AND r.activo = TRUE
    ORDER BY r.nivel DESC
    LIMIT 1
    """;

        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                Timestamp bloqueo = rs.getTimestamp("bloqueado_hasta");
                LocalDateTime bloqueadoHasta = bloqueo == null
                        ? null
                        : bloqueo.toLocalDateTime();

                if (!rs.getBoolean("activo")) {
                    return null;
                }

                if (bloqueadoHasta != null
                        && bloqueadoHasta.isAfter(LocalDateTime.now())) {
                    return null;
                }

                return new UsuarioAutenticado(
                        rs.getLong("usuario_id"),
                        rs.getString("username"),
                        rs.getString("nombre_completo"),
                        rs.getBoolean("activo"),
                        rs.getString("rol")
                );
            }
        }
    }

    public String obtenerHashPassword(String username) throws SQLException {
        String sql = """
            SELECT password_hash
            FROM usuarios
            WHERE username = ?
              AND activo = TRUE
            """;

        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("password_hash") : null;
            }
        }
    }

    public void registrarIntentoFallido(String username)
            throws SQLException {

        String sql = """
            UPDATE usuarios
            SET intentos_fallidos =
                CASE
                    WHEN COALESCE(intentos_fallidos, 0) + 1 >= 5
                    THEN 0
                    ELSE COALESCE(intentos_fallidos, 0) + 1
                END,
                bloqueado_hasta =
                CASE
                    WHEN COALESCE(intentos_fallidos, 0) + 1 >= 5
                    THEN ?
                    ELSE bloqueado_hasta
                END
            WHERE username = ?
              AND activo = TRUE
              AND (
                  bloqueado_hasta IS NULL
                  OR bloqueado_hasta <= CURRENT_TIMESTAMP
              )
            """;

        Timestamp fechaBloqueo = Timestamp.valueOf(
                LocalDateTime.now().plusMinutes(15)
        );

        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setTimestamp(1, fechaBloqueo);
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }

    public void restablecerIntentosFallidos(String username)
            throws SQLException {

        String sql = """
            UPDATE usuarios
            SET intentos_fallidos = 0,
                bloqueado_hasta = NULL
            WHERE username = ?
            """;

        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.executeUpdate();
        }
    }

}

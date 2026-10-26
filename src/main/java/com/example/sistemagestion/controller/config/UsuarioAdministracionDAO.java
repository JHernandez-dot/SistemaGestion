
package com.example.sistemagestion.controller.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class UsuarioAdministracionDAO {

    public record Usuario(
            long usuarioId,
            String username,
            String nombreCompleto,
            boolean activo,
            String rol
    ) {
    }

    public record Rol(
            long rolId,
            String nombre,
            int nivel
    ) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    public List<Usuario> listarUsuarios() throws SQLException {
        String sql = """
            SELECT u.usuario_id, u.username, u.nombre_completo,
                   u.activo, r.nombre AS rol
            FROM usuarios u
            LEFT JOIN usuario_roles ur
                ON ur.usuario_id = u.usuario_id
            LEFT JOIN roles r
                ON r.rol_id = ur.rol_id
            ORDER BY u.usuario_id
            """;

        List<Usuario> usuarios = new ArrayList<>();

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                usuarios.add(new Usuario(
                        rs.getLong("usuario_id"),
                        rs.getString("username"),
                        rs.getString("nombre_completo"),
                        rs.getBoolean("activo"),
                        rs.getString("rol")
                ));
            }
        }

        return usuarios;
    }

    public List<Rol> listarRolesActivos() throws SQLException {
        String sql = """
            SELECT rol_id, nombre, nivel
            FROM roles
            WHERE activo = TRUE
            ORDER BY nivel
            """;

        List<Rol> roles = new ArrayList<>();

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                roles.add(new Rol(
                        rs.getLong("rol_id"),
                        rs.getString("nombre"),
                        rs.getInt("nivel")
                ));
            }
        }

        return roles;
    }

    public void crearUsuario(
            String username,
            String nombreCompleto,
            String password,
            long rolId
    ) throws SQLException {

        username = validarUsername(username);
        nombreCompleto = validarNombre(nombreCompleto);
        validarPassword(password);

        if (rolId <= 0) {
            throw new IllegalArgumentException("Selecciona un rol válido.");
        }

        String sqlUsuario = """
            INSERT INTO usuarios
                (username, nombre_completo, password_hash,
                 activo, intentos_fallidos, creado_en)
            VALUES (?, ?, ?, TRUE, 0, CURRENT_TIMESTAMP)
            """;

        String sqlRol = """
            INSERT INTO usuario_roles (usuario_id, rol_id, asignado_en)
            VALUES (?, ?, CURRENT_TIMESTAMP)
            """;

        try (Connection cn = ConexionDB.conectar()) {
            cn.setAutoCommit(false);

            try {
                long usuarioId;

                try (PreparedStatement ps = cn.prepareStatement(
                        sqlUsuario, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, username);
                    ps.setString(2, nombreCompleto);
                    ps.setString(3, PasswordUtil.hash(password));

                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                    "No se pudo obtener el ID del usuario."
                            );
                        }
                        usuarioId = keys.getLong(1);
                    }
                }

                try (PreparedStatement ps = cn.prepareStatement(sqlRol)) {
                    ps.setLong(1, usuarioId);
                    ps.setLong(2, rolId);
                    ps.executeUpdate();
                }

                cn.commit();

            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            } finally {
                cn.setAutoCommit(true);
            }
        }
    }

    public void actualizarUsuario(
            long usuarioId,
            String nombreCompleto
    ) throws SQLException {

        nombreCompleto = validarNombre(nombreCompleto);

        String sql = """
            UPDATE usuarios
            SET nombre_completo = ?
            WHERE usuario_id = ?
            """;

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, nombreCompleto);
            ps.setLong(2, usuarioId);

            if (ps.executeUpdate() == 0) {
                throw new SQLException("El usuario no existe.");
            }
        }
    }

    public void cambiarPassword(
            long usuarioId,
            String passwordNueva
    ) throws SQLException {

        validarPassword(passwordNueva);

        String sql = """
            UPDATE usuarios
            SET password_hash = ?,
                intentos_fallidos = 0,
                bloqueado_hasta = NULL
            WHERE usuario_id = ?
            """;

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, PasswordUtil.hash(passwordNueva));
            ps.setLong(2, usuarioId);

            if (ps.executeUpdate() == 0) {
                throw new SQLException("El usuario no existe.");
            }
        }
    }

    public void cambiarEstado(
            long usuarioId,
            boolean activo
    ) throws SQLException {

        String sql = """
            UPDATE usuarios
            SET activo = ?
            WHERE usuario_id = ?
            """;

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setBoolean(1, activo);
            ps.setLong(2, usuarioId);

            if (ps.executeUpdate() == 0) {
                throw new SQLException("El usuario no existe.");
            }
        }
    }

    public void cambiarRol(
            long usuarioId,
            long rolId
    ) throws SQLException {

        if (usuarioId <= 0 || rolId <= 0) {
            throw new IllegalArgumentException(
                    "El usuario y el rol deben ser válidos."
            );
        }

        String sqlEliminar = """
            DELETE FROM usuario_roles
            WHERE usuario_id = ?
            """;

        String sqlInsertar = """
            INSERT INTO usuario_roles (usuario_id, rol_id, asignado_en)
            SELECT ?, rol_id, CURRENT_TIMESTAMP
            FROM roles
            WHERE rol_id = ? AND activo = TRUE
            """;

        try (Connection cn = ConexionDB.conectar()) {
            cn.setAutoCommit(false);

            try {
                try (PreparedStatement ps =
                             cn.prepareStatement(sqlInsertar)) {
                    ps.setLong(1, usuarioId);
                    ps.setLong(2, rolId);

                    // Primero comprobamos que el usuario y el rol existan.
                    try (PreparedStatement verificar =
                                 cn.prepareStatement("""
                                     SELECT
                                         EXISTS (
                                             SELECT 1 FROM usuarios
                                             WHERE usuario_id = ?
                                         ) AS usuario_existe,
                                         EXISTS (
                                             SELECT 1 FROM roles
                                             WHERE rol_id = ? AND activo = TRUE
                                         ) AS rol_existe
                                     """)) {

                        verificar.setLong(1, usuarioId);
                        verificar.setLong(2, rolId);

                        try (ResultSet rs = verificar.executeQuery()) {
                            rs.next();

                            if (!rs.getBoolean("usuario_existe")) {
                                throw new SQLException(
                                        "El usuario no existe."
                                );
                            }

                            if (!rs.getBoolean("rol_existe")) {
                                throw new SQLException(
                                        "El rol no existe o está inactivo."
                                );
                            }
                        }
                    }
                }

                try (PreparedStatement ps =
                             cn.prepareStatement(sqlEliminar)) {
                    ps.setLong(1, usuarioId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps =
                             cn.prepareStatement(sqlInsertar)) {
                    ps.setLong(1, usuarioId);
                    ps.setLong(2, rolId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "No se pudo asignar el rol."
                        );
                    }
                }

                cn.commit();

            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            } finally {
                cn.setAutoCommit(true);
            }
        }
    }

    private String validarUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de usuario es obligatorio."
            );
        }

        username = username.trim();

        if (username.length() > 50 || username.contains(" ")) {
            throw new IllegalArgumentException(
                    "El usuario no debe contener espacios y debe tener "
                            + "máximo 50 caracteres."
            );
        }

        return username;
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre completo es obligatorio."
            );
        }

        nombre = nombre.trim();

        if (nombre.length() > 150) {
            throw new IllegalArgumentException(
                    "El nombre completo es demasiado largo."
            );
        }

        return nombre;
    }

    private void validarPassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos 8 caracteres."
            );
        }
    }
}


package com.example.sistemagestion.controller.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class CrearUsuarioPrueba {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Usuario de prueba: ");
        String username = scanner.nextLine().trim();

        System.out.print("Nombre completo: ");
        String nombre = scanner.nextLine().trim();

        System.out.print("Contraseña temporal (mínimo 10 caracteres): ");
        String password = scanner.nextLine();

        if (username.isBlank() || nombre.isBlank()
                || password.length() < 10) {
            System.out.println("Datos inválidos. Revisa los campos "
                    + "y utiliza una contraseña de al menos 10 caracteres.");
            return;
        }

        String comprobarUsuario =
                "SELECT 1 FROM usuarios WHERE username = ?";

        String buscarRol =
                "SELECT rol_id FROM roles "
                        + "WHERE nombre = 'CAJERO' AND activo = TRUE";

        String insertarUsuario =
                "INSERT INTO usuarios "
                        + "(username, nombre_completo, password_hash, activo, "
                        + "intentos_fallidos, bloqueado_hasta, creado_en) "
                        + "VALUES (?, ?, ?, TRUE, 0, NULL, CURRENT_TIMESTAMP) "
                        + "RETURNING usuario_id";

        String asignarRol =
                "INSERT INTO usuario_roles "
                        + "(usuario_id, rol_id, asignado_en) "
                        + "VALUES (?, ?, CURRENT_TIMESTAMP)";

        try (Connection conexion = ConexionDB.conectar()) {
            conexion.setAutoCommit(false);

            try {
                try (PreparedStatement ps =
                             conexion.prepareStatement(comprobarUsuario)) {
                    ps.setString(1, username);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            throw new SQLException(
                                    "Ese nombre de usuario ya existe."
                            );
                        }
                    }
                }

                long rolId;

                try (PreparedStatement ps =
                             conexion.prepareStatement(buscarRol);
                     ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        throw new SQLException(
                                "No se encontró un rol CAJERO activo."
                        );
                    }

                    rolId = rs.getLong("rol_id");
                }

                String hash = PasswordUtil.hash(password);
                long usuarioId;

                try (PreparedStatement ps =
                             conexion.prepareStatement(insertarUsuario)) {
                    ps.setString(1, username);
                    ps.setString(2, nombre);
                    ps.setString(3, hash);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "No se pudo crear el usuario."
                            );
                        }

                        usuarioId = rs.getLong("usuario_id");
                    }
                }

                try (PreparedStatement ps =
                             conexion.prepareStatement(asignarRol)) {
                    ps.setLong(1, usuarioId);
                    ps.setLong(2, rolId);
                    ps.executeUpdate();
                }

                conexion.commit();
                System.out.println(
                        "Usuario de prueba creado con rol CAJERO."
                );
                System.out.println("Usuario: " + username);

            } catch (Exception e) {
                conexion.rollback();
                System.err.println(
                        "No se pudo crear el usuario: " + e.getMessage()
                );
            } finally {
                conexion.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println(
                    "Error de conexión o base de datos: " + e.getMessage()
            );
        }
    }
}

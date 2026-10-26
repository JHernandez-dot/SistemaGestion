
package com.example.sistemagestion.controller.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {

    private static final String URL = "DB_URL";
    private static final String USUARIO = "DB_USER";
    private static final String PASSWORD = "DB_PASSWORD";

    private static String obtenerVariable(String nombre)
            throws SQLException {
        String valor = System.getenv(nombre);

        if (valor == null || valor.isBlank()) {
            throw new SQLException(
                    "Falta configurar la variable de entorno: " + nombre
            );
        }

        return valor;
    }

    public static Connection conectar() throws SQLException {
        String url = obtenerVariable(URL);
        String usuario = obtenerVariable(USUARIO);
        String password = obtenerVariable(PASSWORD);

        return DriverManager.getConnection(url, usuario, password);
    }

    public static void probarConexion() {
        try (Connection conexion = conectar()) {
            System.out.println("Conexión a PostgreSQL exitosa.");
            System.out.println("Base de datos: " + conexion.getCatalog());
        } catch (SQLException e) {
            System.err.println(
                    "Error al conectar con PostgreSQL: " + e.getMessage()
            );
        }
    }

    public static void main(String[] args) {
        probarConexion();
    }
}

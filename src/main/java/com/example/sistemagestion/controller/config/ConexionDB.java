package com.example.sistemagestion.controller.config;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {

    private static final String URL =
            "jdbc:postgresql://localhost:5433/sistema_gestion_pos";

    private static final String USUARIO = "postgres";


    private static final String PASSWORD = "123456";

    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, PASSWORD);
    }

    public static void probarConexion() {
        try (Connection conexion = conectar()) {
            System.out.println("Conexión a PostgreSQL exitosa.");
            System.out.println("Base de datos: "
                    + conexion.getCatalog());
        } catch (SQLException e) {
            System.err.println("Error al conectar con PostgreSQL: "
                    + e.getMessage());
        }
    }
    public static void main(String[] args) {
        probarConexion();
    }
}

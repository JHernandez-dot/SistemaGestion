
package com.example.sistemagestion.controller;

import com.example.sistemagestion.controller.config.ConexionDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDAO {

    public static class Categoria {
        private final long id;
        private final String nombre;
        private final String descripcion;
        private final boolean activo;

        public Categoria(long id, String nombre,
                         String descripcion, boolean activo) {
            this.id = id;
            this.nombre = nombre;
            this.descripcion = descripcion;
            this.activo = activo;
        }

        public long getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public boolean isActivo() {
            return activo;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    public List<Categoria> listar() throws SQLException {
        String sql = """
            SELECT categoria_id, nombre, descripcion, activo
            FROM categorias
            ORDER BY nombre
            """;

        List<Categoria> categorias = new ArrayList<>();

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                categorias.add(new Categoria(
                        rs.getLong("categoria_id"),
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        rs.getBoolean("activo")
                ));
            }
        }

        return categorias;
    }

    public void registrar(String nombre, String descripcion)
            throws SQLException {

        String sql = """
            INSERT INTO categorias (nombre, descripcion)
            VALUES (?, ?)
            """;

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, nombre.trim());

            if (descripcion == null || descripcion.isBlank()) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, descripcion.trim());
            }

            ps.executeUpdate();
        }
    }

    public void desactivar(long categoriaId) throws SQLException {
        String sql = """
            UPDATE categorias
            SET activo = FALSE
            WHERE categoria_id = ?
            """;

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, categoriaId);
            ps.executeUpdate();
        }
    }
}

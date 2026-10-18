package com.example.sistemagestion.controller;

import com.example.sistemagestion.controller.config.ConexionDB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    public static class Producto {
        private final long id;
        private final String sku;
        private final String codigoBarras;
        private final String nombre;
        private final String descripcion;
        private final BigDecimal precioVenta;
        private final BigDecimal costo;
        private final BigDecimal existencia;
        private final BigDecimal stockMinimo;
        private final String unidadMedida;
        private final Long categoriaId;
        private final String categoria;
        private final boolean activo;

        public Producto(
                long id, String sku, String codigoBarras,
                String nombre, String descripcion,
                BigDecimal precioVenta, BigDecimal costo,
                BigDecimal existencia, BigDecimal stockMinimo,
                String unidadMedida, Long categoriaId,
                String categoria, boolean activo) {
            this.id = id;
            this.sku = sku;
            this.codigoBarras = codigoBarras;
            this.nombre = nombre;
            this.descripcion = descripcion;
            this.precioVenta = precioVenta;
            this.costo = costo;
            this.existencia = existencia;
            this.stockMinimo = stockMinimo;
            this.unidadMedida = unidadMedida;
            this.categoriaId = categoriaId;
            this.categoria = categoria;
            this.activo = activo;
        }

        public long getId() { return id; }
        public String getSku() { return sku; }
        public String getCodigoBarras() { return codigoBarras; }
        public String getNombre() { return nombre; }
        public String getDescripcion() { return descripcion; }
        public BigDecimal getPrecioVenta() { return precioVenta; }
        public BigDecimal getCosto() { return costo; }
        public BigDecimal getExistencia() { return existencia; }
        public BigDecimal getStockMinimo() { return stockMinimo; }
        public String getUnidadMedida() { return unidadMedida; }
        public Long getCategoriaId() { return categoriaId; }
        public String getCategoria() { return categoria; }
        public boolean isActivo() { return activo; }
    }

    public List<Producto> listar() throws SQLException {
        List<Producto> productos = new ArrayList<>();

        String sql = """
            SELECT p.producto_id, p.sku, p.codigo_barras,
                   p.nombre, p.descripcion, p.precio_venta,
                   p.costo, p.existencia, p.stock_minimo,
                   p.unidad_medida, p.categoria_id,
                   c.nombre AS categoria, p.activo
            FROM productos p
            LEFT JOIN categorias c
                ON p.categoria_id = c.categoria_id
            ORDER BY p.nombre
            """;

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                long categoriaValor = rs.getLong("categoria_id");
                Long categoriaId = rs.wasNull() ? null : categoriaValor;

                productos.add(new Producto(
                        rs.getLong("producto_id"),
                        rs.getString("sku"),
                        rs.getString("codigo_barras"),
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        rs.getBigDecimal("precio_venta"),
                        rs.getBigDecimal("costo"),
                        rs.getBigDecimal("existencia"),
                        rs.getBigDecimal("stock_minimo"),
                        rs.getString("unidad_medida"),
                        categoriaId,
                        rs.getString("categoria"),
                        rs.getBoolean("activo")
                ));
            }
        }

        return productos;
    }

    public void registrar(
            String sku, String codigoBarras, String nombre,
            String descripcion, BigDecimal precioVenta,
            BigDecimal costo, BigDecimal existencia,
            BigDecimal stockMinimo, String unidadMedida,
            Long categoriaId) throws SQLException {

        String sql = """
            INSERT INTO productos (
                sku, codigo_barras, nombre, descripcion,
                precio_venta, costo, existencia, stock_minimo,
                unidad_medida, categoria_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, sku);
            ps.setString(2, codigoBarras == null || codigoBarras.isBlank()
                    ? null : codigoBarras.trim());
            ps.setString(3, nombre);
            ps.setString(4, descripcion);
            ps.setBigDecimal(5, precioVenta);
            ps.setBigDecimal(6, costo);
            ps.setBigDecimal(7, existencia);
            ps.setBigDecimal(8, stockMinimo);
            ps.setString(9, unidadMedida);

            if (categoriaId == null) {
                ps.setNull(10, Types.BIGINT);
            } else {
                ps.setLong(10, categoriaId);
            }

            ps.executeUpdate();
        }
    }

    public void desactivar(long id) throws SQLException {
        String sql = """
            UPDATE productos
            SET activo = FALSE
            WHERE producto_id = ? AND activo = TRUE
            """;

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}

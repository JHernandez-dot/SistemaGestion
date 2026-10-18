
package com.example.sistemagestion.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.sql.SQLException;

public class ProductoController {

    @FXML private TextField txtSku;
    @FXML private TextField txtCodigoBarras;
    @FXML private TextField txtNombre;
    @FXML private TextField txtDescripcion;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtCosto;
    @FXML private TextField txtExistencia;
    @FXML private TextField txtStockMinimo;
    @FXML private ComboBox<String> cmbUnidad;
    @FXML private ComboBox<CategoriaDAO.Categoria> cmbCategoria;

    @FXML private TableView<ProductoDAO.Producto> tablaProductos;
    @FXML private TableColumn<ProductoDAO.Producto, Long> colId;
    @FXML private TableColumn<ProductoDAO.Producto, String> colSku;
    @FXML private TableColumn<ProductoDAO.Producto, String> colNombre;
    @FXML private TableColumn<ProductoDAO.Producto, BigDecimal> colPrecio;
    @FXML private TableColumn<ProductoDAO.Producto, BigDecimal> colExistencia;
    @FXML private TableColumn<ProductoDAO.Producto, String> colCategoria;
    @FXML private TableColumn<ProductoDAO.Producto, Boolean> colActivo;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSku.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colExistencia.setCellValueFactory(new PropertyValueFactory<>("existencia"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));

        cmbUnidad.setItems(FXCollections.observableArrayList(
                "UNIDAD", "LIBRA", "KILOGRAMO", "LITRO", "PAQUETE", "CAJA"
        ));
        cmbUnidad.setValue("UNIDAD");

        cargarCategorias();
        cargarProductos();
    }

    private void cargarCategorias() {
        try {
            cmbCategoria.setItems(
                    FXCollections.observableArrayList(categoriaDAO.listar())
            );
        } catch (SQLException e) {
            mostrarError("No se pudieron cargar las categorías: " + e.getMessage());
        }
    }

    private void cargarProductos() {
        try {
            ObservableList<ProductoDAO.Producto> lista =
                    FXCollections.observableArrayList(productoDAO.listar());
            tablaProductos.setItems(lista);
        } catch (SQLException e) {
            mostrarError("No se pudieron cargar los productos: " + e.getMessage());
        }
    }

    @FXML
    private void registrarProducto() {
        try {
            String sku = txtSku.getText().trim();
            String nombre = txtNombre.getText().trim();
            String unidad = cmbUnidad.getValue();

            if (sku.isEmpty() || nombre.isEmpty()
                    || txtPrecio.getText().isBlank()
                    || txtCosto.getText().isBlank()
                    || txtExistencia.getText().isBlank()
                    || txtStockMinimo.getText().isBlank()
                    || unidad == null) {
                mostrarError("Completa los campos obligatorios.");
                return;
            }

            BigDecimal precio = new BigDecimal(txtPrecio.getText().trim());
            BigDecimal costo = new BigDecimal(txtCosto.getText().trim());
            BigDecimal existencia = new BigDecimal(txtExistencia.getText().trim());
            BigDecimal stockMinimo = new BigDecimal(txtStockMinimo.getText().trim());

            if (precio.signum() < 0 || costo.signum() < 0
                    || existencia.signum() < 0 || stockMinimo.signum() < 0) {
                mostrarError("Los valores numéricos no pueden ser negativos.");
                return;
            }

            CategoriaDAO.Categoria categoria = cmbCategoria.getValue();

            productoDAO.registrar(
                    sku,
                    txtCodigoBarras.getText().trim(),
                    nombre,
                    txtDescripcion.getText().trim(),
                    precio,
                    costo,
                    existencia,
                    stockMinimo,
                    unidad,
                    categoria == null ? null : categoria.getId()
            );

            mostrarInformacion("Producto registrado correctamente.");
            limpiarFormulario();
            cargarProductos();

        } catch (NumberFormatException e) {
            mostrarError("Verifica los valores numéricos. Usa punto para los decimales.");
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                mostrarError("El SKU o código de barras ya existe.");
            } else {
                mostrarError("No se pudo registrar el producto: " + e.getMessage());
            }
        }
    }

    @FXML
    private void desactivarProducto() {
        ProductoDAO.Producto producto = tablaProductos.getSelectionModel()
                .getSelectedItem();

        if (producto == null) {
            mostrarError("Selecciona un producto de la tabla.");
            return;
        }

        if (!producto.isActivo()) {
            mostrarError("El producto ya está desactivado.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar acción");
        confirmacion.setHeaderText("Desactivar producto");
        confirmacion.setContentText("¿Deseas desactivar " + producto.getNombre() + "?");

        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                productoDAO.desactivar(producto.getId());
                cargarProductos();
                mostrarInformacion("Producto desactivado correctamente.");
            } catch (SQLException e) {
                mostrarError("No se pudo desactivar: " + e.getMessage());
            }
        }
    }

    @FXML
    private void limpiarFormulario() {
        txtSku.clear();
        txtCodigoBarras.clear();
        txtNombre.clear();
        txtDescripcion.clear();
        txtPrecio.clear();
        txtCosto.clear();
        txtExistencia.clear();
        txtStockMinimo.clear();
        cmbUnidad.setValue("UNIDAD");
        cmbCategoria.setValue(null);
    }

    @FXML
    private void regresarMenu() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource(
                            "/com/example/sistemagestion/view/menu_principal.fxml"
                    )
            );

            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage)
                    tablaProductos.getScene().getWindow();

            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle("Menú principal");
            stage.centerOnScreen();
            stage.show();

        } catch (java.io.IOException | RuntimeException e) {
            mostrarError("No se pudo regresar al menú: " + e.getMessage());
        }
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Error");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void mostrarInformacion(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Información");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}

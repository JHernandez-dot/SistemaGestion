
package com.example.sistemagestion.controller;

import com.example.sistemagestion.controller.config.UsuarioDAO;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class InventarioController {

    @FXML private TextField txtBuscar;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtMotivo;
    @FXML private ComboBox<String> cmbTipo;

    @FXML private TableView<ProductoDAO.Producto> tablaProductos;
    @FXML private TableColumn<ProductoDAO.Producto, String> colCodigo;
    @FXML private TableColumn<ProductoDAO.Producto, String> colNombre;
    @FXML private TableColumn<ProductoDAO.Producto, String> colCategoria;
    @FXML private TableColumn<ProductoDAO.Producto, BigDecimal> colExistencia;
    @FXML private TableColumn<ProductoDAO.Producto, BigDecimal> colMinimo;
    @FXML private TableColumn<ProductoDAO.Producto, String> colUnidad;

    @FXML private TableView<String[]> tablaMovimientos;
    @FXML private TableColumn<String[], String> colFecha;
    @FXML private TableColumn<String[], String> colProductoMov;
    @FXML private TableColumn<String[], String> colTipoMov;
    @FXML private TableColumn<String[], String> colCantidadMov;
    @FXML private TableColumn<String[], String> colAnterior;
    @FXML private TableColumn<String[], String> colNueva;
    @FXML private TableColumn<String[], String> colMotivo;

    @FXML private Label lblEstado;
    @FXML private Label lblSeleccionado;

    private final InventarioDAO dao = new InventarioDAO();

    private final ObservableList<ProductoDAO.Producto> productos =
            FXCollections.observableArrayList();

    private UsuarioDAO.UsuarioAutenticado usuarioAutenticado;

    public void setUsuarioAutenticado(
            UsuarioDAO.UsuarioAutenticado usuario) {
        this.usuarioAutenticado = usuario;
    }

    @FXML
    private void initialize() {
        colCodigo.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getSku()));

        colNombre.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNombre()));

        colCategoria.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getCategoria() == null
                                ? ""
                                : c.getValue().getCategoria()));

        colExistencia.setCellValueFactory(
                new PropertyValueFactory<>("existencia"));

        colMinimo.setCellValueFactory(
                new PropertyValueFactory<>("stockMinimo"));

        colUnidad.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getUnidadMedida() == null
                                ? ""
                                : c.getValue().getUnidadMedida()));

        configurarColumnaMovimiento(colFecha, 1);
        configurarColumnaMovimiento(colProductoMov, 2);
        configurarColumnaMovimiento(colTipoMov, 3);
        configurarColumnaMovimiento(colCantidadMov, 4);
        configurarColumnaMovimiento(colAnterior, 5);
        configurarColumnaMovimiento(colNueva, 6);
        configurarColumnaMovimiento(colMotivo, 7);

        cmbTipo.setItems(FXCollections.observableArrayList(
                "ENTRADA", "AJUSTE"));

        cmbTipo.setValue("ENTRADA");

        cmbTipo.valueProperty().addListener(
                (obs, anterior, actual) -> {
                    if ("AJUSTE".equals(actual)) {
                        mostrar(
                                "AJUSTE: ingresa la existencia final deseada.");
                    } else if ("ENTRADA".equals(actual)) {
                        mostrar(
                                "ENTRADA: ingresa las unidades recibidas.");
                    }
                });

        tablaProductos.setItems(productos);

        tablaProductos.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, actual) -> {
                    if (actual != null) {
                        lblSeleccionado.setText(
                                "Seleccionado: "
                                        + actual.getNombre()
                                        + " | Existencia: "
                                        + actual.getExistencia()
                                        + " "
                                        + actual.getUnidadMedida());
                    } else {
                        lblSeleccionado.setText(
                                "Selecciona un producto");
                    }
                });

        try {
            cargarDatos();
            mostrar("Inventario cargado correctamente.");
        } catch (SQLException e) {
            mostrar("Error al cargar inventario: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void configurarColumnaMovimiento(
            TableColumn<String[], String> columna,
            int indice) {

        columna.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue()[indice]));
    }

    @FXML
    private void buscar() {
        String texto = txtBuscar.getText() == null
                ? ""
                : txtBuscar.getText().trim().toLowerCase();

        tablaProductos.setItems(productos.filtered(p ->
                (p.getNombre() != null
                        && p.getNombre().toLowerCase().contains(texto))
                        || (p.getSku() != null
                        && p.getSku().toLowerCase().contains(texto))
                        || (p.getCodigoBarras() != null
                        && p.getCodigoBarras().toLowerCase().contains(texto))
        ));

        mostrar("Búsqueda realizada.");
    }

    @FXML
    private void mostrarTodos() {
        txtBuscar.clear();
        tablaProductos.setItems(productos);
        mostrar("Mostrando todos los productos.");
    }

    @FXML
    private void registrarMovimiento() {
        System.out.println("=== INICIO REGISTRO DE INVENTARIO ===");

        try {
            if (usuarioAutenticado == null) {
                mostrar("Error: no se encontró la sesión del usuario.");
                System.err.println("La sesión del usuario es null.");
                return;
            }

            ProductoDAO.Producto producto =
                    tablaProductos.getSelectionModel().getSelectedItem();

            if (producto == null) {
                mostrar("Selecciona un producto de la tabla.");
                System.err.println("No hay producto seleccionado.");
                return;
            }

            String tipo = cmbTipo.getValue();
            String textoCantidad = txtCantidad.getText();
            String motivo = txtMotivo.getText();

            System.out.println("Producto ID: " + producto.getId());
            System.out.println("Tipo: [" + tipo + "]");
            System.out.println("Cantidad: [" + textoCantidad + "]");
            System.out.println("Motivo: [" + motivo + "]");

            if (tipo == null || tipo.isBlank()) {
                mostrar("Selecciona el tipo de movimiento.");
                return;
            }

            if (textoCantidad == null || textoCantidad.isBlank()) {
                mostrar("Ingresa una cantidad.");
                return;
            }

            BigDecimal cantidad;

            try {
                cantidad = new BigDecimal(textoCantidad.trim());
            } catch (NumberFormatException e) {
                mostrar("Cantidad inválida. Ejemplos: 5 o 2.5.");
                return;
            }

            if ("ENTRADA".equals(tipo) && cantidad.signum() <= 0) {
                mostrar("La cantidad de entrada debe ser mayor que cero.");
                return;
            }

            if ("AJUSTE".equals(tipo) && cantidad.signum() < 0) {
                mostrar("La existencia final no puede ser negativa.");
                return;
            }

            if (motivo == null || motivo.isBlank()) {
                mostrar("Escribe el motivo del movimiento.");
                return;
            }

            System.out.println("Enviando movimiento al DAO...");

            dao.registrarMovimiento(
                    producto.getId(),
                    usuarioAutenticado.getUsuarioId(),
                    tipo,
                    cantidad,
                    motivo.trim()
            );

            System.out.println("Movimiento guardado en PostgreSQL.");

            // Recargar productos e historial.
            cargarDatos();

            // Volver a seleccionar el producto actualizado.
            for (ProductoDAO.Producto p : productos) {
                if (p.getId() == producto.getId()) {
                    tablaProductos.getSelectionModel().select(p);
                    break;
                }
            }

            txtCantidad.clear();
            txtMotivo.clear();

            mostrar("Movimiento " + tipo
                    + " registrado correctamente.");

            System.out.println("=== REGISTRO COMPLETADO ===");

        } catch (SQLException e) {
            mostrar("Error de base de datos: " + e.getMessage());
            System.err.println("ERROR SQL EN INVENTARIO:");
            e.printStackTrace();

        } catch (Exception e) {
            mostrar("Error al registrar: " + e.getMessage());
            System.err.println("ERROR INESPERADO EN INVENTARIO:");
            e.printStackTrace();
        }
    }

    private void cargarDatos() throws SQLException {
        productos.setAll(dao.listarProductos());

        List<String[]> movimientos = dao.listarMovimientos();

        tablaMovimientos.setItems(
                FXCollections.observableArrayList(movimientos));

        tablaProductos.refresh();
        tablaMovimientos.refresh();
    }

    @FXML
    private void volverAlMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/sistemagestion/view/menu_principal.fxml"
                    )
            );

            Parent root = loader.load();

            MenuPrincipalController controller =
                    loader.getController();

            controller.setUsuarioAutenticado(usuarioAutenticado);

            Stage stage = (Stage) lblEstado.getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("Sistema de Gestión");
            stage.centerOnScreen();

        } catch (IOException | RuntimeException e) {
            mostrar("No se pudo regresar al menú: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mostrar(String mensaje) {
        if (lblEstado != null) {
            lblEstado.setText(mensaje);
        }
    }
}

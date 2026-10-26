
package com.example.sistemagestion.controller;

import com.example.sistemagestion.controller.config.UsuarioDAO;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class VentasController {

    @FXML private Label lblUsuario;
    @FXML private Label lblTurno;
    @FXML private Label lblTotal;

    @FXML private TextField txtBuscar;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtPagoEfectivo;
    @FXML private TextField txtPagoTarjeta;
    @FXML private TextField txtPagoTransferencia;
    @FXML private TextArea txtObservaciones;
    @FXML private ComboBox<String> cmbMetodoPago;

    @FXML private TableView<ProductoDAO.Producto> tablaProductos;
    @FXML private TableColumn<ProductoDAO.Producto, String> colSku;
    @FXML private TableColumn<ProductoDAO.Producto, String> colProducto;
    @FXML private TableColumn<ProductoDAO.Producto, BigDecimal> colPrecio;
    @FXML private TableColumn<ProductoDAO.Producto, BigDecimal> colExistencia;

    @FXML private TableView<ItemCarrito> tablaCarrito;
    @FXML private TableColumn<ItemCarrito, String> colCarritoProducto;
    @FXML private TableColumn<ItemCarrito, BigDecimal> colCarritoCantidad;
    @FXML private TableColumn<ItemCarrito, BigDecimal> colCarritoPrecio;
    @FXML private TableColumn<ItemCarrito, BigDecimal> colCarritoSubtotal;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final VentaDAO ventaDAO = new VentaDAO();

    private final ObservableList<ProductoDAO.Producto> productos =
            FXCollections.observableArrayList();

    private final ObservableList<ItemCarrito> carrito =
            FXCollections.observableArrayList();

    private UsuarioDAO.UsuarioAutenticado usuarioAutenticado;

    public static class ItemCarrito {

        private final ProductoDAO.Producto producto;
        private BigDecimal cantidad;

        public ItemCarrito(
                ProductoDAO.Producto producto,
                BigDecimal cantidad) {
            this.producto = producto;
            this.cantidad = cantidad;
        }

        public String getNombre() {
            return producto.getNombre();
        }

        public BigDecimal getCantidad() {
            return cantidad;
        }

        public BigDecimal getPrecio() {
            return producto.getPrecioVenta();
        }

        public BigDecimal getSubtotal() {
            return getPrecio()
                    .multiply(cantidad)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        public ProductoDAO.Producto getProducto() {
            return producto;
        }

        public void sumarCantidad(BigDecimal cantidadAdicional) {
            cantidad = cantidad.add(cantidadAdicional);
        }
    }

    public void setUsuarioAutenticado(
            UsuarioDAO.UsuarioAutenticado usuarioAutenticado) {

        this.usuarioAutenticado = usuarioAutenticado;

        if (lblUsuario != null && usuarioAutenticado != null) {
            String rol = usuarioAutenticado.getRol();

            String etiqueta = "CAJERO".equalsIgnoreCase(
                    rol == null ? "" : rol.trim())
                    ? "Cajero: "
                    : "Usuario: ";

            lblUsuario.setText(
                    etiqueta + usuarioAutenticado.getNombreCompleto()
            );
        }
    }

    @FXML
    public void initialize() {
        colSku.setCellValueFactory(
                new PropertyValueFactory<>("sku"));
        colProducto.setCellValueFactory(
                new PropertyValueFactory<>("nombre"));
        colPrecio.setCellValueFactory(
                new PropertyValueFactory<>("precioVenta"));
        colExistencia.setCellValueFactory(
                new PropertyValueFactory<>("existencia"));

        colCarritoProducto.setCellValueFactory(
                new PropertyValueFactory<>("nombre"));
        colCarritoCantidad.setCellValueFactory(
                new PropertyValueFactory<>("cantidad"));
        colCarritoPrecio.setCellValueFactory(
                new PropertyValueFactory<>("precio"));
        colCarritoSubtotal.setCellValueFactory(
                new PropertyValueFactory<>("subtotal"));

        tablaProductos.setItems(productos);
        tablaCarrito.setItems(carrito);

        cmbMetodoPago.setItems(FXCollections.observableArrayList(
                "EFECTIVO", "TARJETA", "TRANSFERENCIA", "MIXTO"
        ));
        cmbMetodoPago.setValue("EFECTIVO");

        cmbMetodoPago.valueProperty().addListener(
                (observable, anterior, nuevo) -> actualizarCamposPago()
        );

        actualizarCamposPago();

        lblTurno.setText(
                "El turno abierto se validará al confirmar la venta."
        );

        cargarProductos();
        actualizarTotal();
    }

    private void actualizarCamposPago() {
        if (cmbMetodoPago == null || txtPagoEfectivo == null) {
            return;
        }

        String metodo = cmbMetodoPago.getValue();

        if (metodo == null) {
            return;
        }

        boolean mixto = "MIXTO".equals(metodo);

        txtPagoEfectivo.setDisable(
                !mixto && !"EFECTIVO".equals(metodo));
        txtPagoTarjeta.setDisable(
                !mixto && !"TARJETA".equals(metodo));
        txtPagoTransferencia.setDisable(
                !mixto && !"TRANSFERENCIA".equals(metodo));

        if (!mixto) {
            txtPagoEfectivo.setText(
                    "EFECTIVO".equals(metodo)
                            ? calcularTotal().toPlainString() : "0.00");

            txtPagoTarjeta.setText(
                    "TARJETA".equals(metodo)
                            ? calcularTotal().toPlainString() : "0.00");

            txtPagoTransferencia.setText(
                    "TRANSFERENCIA".equals(metodo)
                            ? calcularTotal().toPlainString() : "0.00");
        } else {
            txtPagoEfectivo.setText("0.00");
            txtPagoTarjeta.setText("0.00");
            txtPagoTransferencia.setText("0.00");
        }
    }

    private void cargarProductos() {
        try {
            productos.setAll(
                    productoDAO.listar().stream()
                            .filter(ProductoDAO.Producto::isActivo)
                            .collect(Collectors.toList())
            );
        } catch (SQLException e) {
            mostrarError(
                    "No se pudieron cargar los productos: " + e.getMessage());
        }
    }

    @FXML
    private void buscarProductos() {
        String texto = txtBuscar.getText();

        if (texto == null || texto.isBlank()) {
            mostrarTodos();
            return;
        }

        String criterio = texto.trim().toLowerCase(Locale.ROOT);

        try {
            productos.setAll(
                    productoDAO.listar().stream()
                            .filter(ProductoDAO.Producto::isActivo)
                            .filter(p ->
                                    contiene(p.getNombre(), criterio)
                                            || contiene(p.getSku(), criterio)
                                            || contiene(
                                            p.getCodigoBarras(),
                                            criterio))
                            .collect(Collectors.toList())
            );
        } catch (SQLException e) {
            mostrarError("No se pudo buscar: " + e.getMessage());
        }
    }

    @FXML
    private void mostrarTodos() {
        txtBuscar.clear();
        cargarProductos();
    }

    private boolean contiene(String valor, String criterio) {
        return valor != null
                && valor.toLowerCase(Locale.ROOT).contains(criterio);
    }

    @FXML
    private void agregarAlCarrito() {
        ProductoDAO.Producto producto =
                tablaProductos.getSelectionModel().getSelectedItem();

        if (producto == null) {
            mostrarError("Selecciona un producto de la tabla.");
            return;
        }

        BigDecimal cantidad;

        try {
            cantidad = new BigDecimal(txtCantidad.getText().trim());

            if (cantidad.signum() <= 0) {
                mostrarError("La cantidad debe ser mayor que cero.");
                return;
            }
        } catch (Exception e) {
            mostrarError("Ingresa una cantidad válida.");
            return;
        }

        BigDecimal cantidadExistente = carrito.stream()
                .filter(i -> i.getProducto().getId() == producto.getId())
                .map(ItemCarrito::getCantidad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (cantidadExistente.add(cantidad)
                .compareTo(producto.getExistencia()) > 0) {
            mostrarError("Existencias insuficientes. Disponibles: "
                    + producto.getExistencia());
            return;
        }

        ItemCarrito existente = carrito.stream()
                .filter(i -> i.getProducto().getId() == producto.getId())
                .findFirst()
                .orElse(null);

        if (existente == null) {
            carrito.add(new ItemCarrito(producto, cantidad));
        } else {
            existente.sumarCantidad(cantidad);
            tablaCarrito.refresh();
        }

        txtCantidad.setText("1");
        actualizarTotal();
    }

    @FXML
    private void quitarDelCarrito() {
        ItemCarrito seleccionado =
                tablaCarrito.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarError("Selecciona un producto del carrito.");
            return;
        }

        carrito.remove(seleccionado);
        actualizarTotal();
    }

    @FXML
    private void vaciarCarrito() {
        carrito.clear();
        actualizarTotal();
    }

    private BigDecimal calcularTotal() {
        return carrito.stream()
                .map(ItemCarrito::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void actualizarTotal() {
        BigDecimal total = calcularTotal();
        lblTotal.setText("Q " + total.toPlainString());

        if (cmbMetodoPago != null
                && cmbMetodoPago.getValue() != null
                && !"MIXTO".equals(cmbMetodoPago.getValue())) {
            actualizarCamposPago();
        }
    }

    private BigDecimal leerMonto(TextField campo, String nombre) {
        try {
            BigDecimal monto = new BigDecimal(campo.getText().trim())
                    .setScale(2, RoundingMode.HALF_UP);

            if (monto.signum() < 0) {
                throw new IllegalArgumentException(
                        "El importe de " + nombre + " no puede ser negativo.");
            }

            return monto;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Ingresa un importe válido para " + nombre + ".");
        }
    }

    private List<VentaDAO.PagoVenta> obtenerPagos(BigDecimal total) {
        String metodo = cmbMetodoPago.getValue();

        if (metodo == null || metodo.isBlank()) {
            throw new IllegalArgumentException(
                    "Selecciona un método de pago.");
        }

        List<VentaDAO.PagoVenta> pagos = new ArrayList<>();

        if ("MIXTO".equals(metodo)) {
            agregarPagoSiPositivo(
                    pagos, "EFECTIVO",
                    leerMonto(txtPagoEfectivo, "efectivo"));

            agregarPagoSiPositivo(
                    pagos, "TARJETA",
                    leerMonto(txtPagoTarjeta, "tarjeta"));

            agregarPagoSiPositivo(
                    pagos, "TRANSFERENCIA",
                    leerMonto(txtPagoTransferencia, "transferencia"));
        } else {
            BigDecimal monto;

            switch (metodo) {
                case "EFECTIVO" ->
                        monto = leerMonto(txtPagoEfectivo, "efectivo");
                case "TARJETA" ->
                        monto = leerMonto(txtPagoTarjeta, "tarjeta");
                case "TRANSFERENCIA" ->
                        monto = leerMonto(
                                txtPagoTransferencia, "transferencia");
                default -> throw new IllegalArgumentException(
                        "Método de pago no reconocido.");
            }

            agregarPagoSiPositivo(pagos, metodo, monto);
        }

        BigDecimal suma = pagos.stream()
                .map(VentaDAO.PagoVenta::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        if (suma.compareTo(total) != 0) {
            throw new IllegalArgumentException(
                    "Los pagos suman Q " + suma
                            + ", pero el total es Q " + total + ".");
        }

        return pagos;
    }

    private void agregarPagoSiPositivo(
            List<VentaDAO.PagoVenta> pagos,
            String metodo,
            BigDecimal monto) {

        if (monto.compareTo(BigDecimal.ZERO) > 0) {
            pagos.add(new VentaDAO.PagoVenta(metodo, monto, null));
        }
    }

    @FXML
    private void confirmarVenta() {
        if (usuarioAutenticado == null
                || usuarioAutenticado.getUsuarioId() <= 0) {
            mostrarError("No se encontró el usuario autenticado.");
            return;
        }

        if (carrito.isEmpty()) {
            mostrarError("Agrega productos antes de confirmar la venta.");
            return;
        }

        BigDecimal total = calcularTotal();
        List<VentaDAO.PagoVenta> pagos;

        try {
            pagos = obtenerPagos(total);
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar venta");
        confirmacion.setHeaderText(
                "Total a cobrar: Q " + total.toPlainString());
        confirmacion.setContentText(
                "Método: " + cmbMetodoPago.getValue()
                        + "\n¿Deseas registrar la venta y descontar el inventario?");

        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL)
                != ButtonType.OK) {
            return;
        }

        List<VentaDAO.ItemVenta> items = new ArrayList<>();

        for (ItemCarrito item : carrito) {
            items.add(new VentaDAO.ItemVenta(
                    item.getProducto().getId(),
                    item.getCantidad()));
        }

        try {
            VentaDAO.ResultadoVenta resultado =
                    ventaDAO.registrarVenta(
                            usuarioAutenticado.getUsuarioId(),
                            items,
                            txtObservaciones.getText(),
                            pagos);

            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
            alerta.setTitle("Venta completada");
            alerta.setHeaderText(
                    "Venta número " + resultado.numeroVenta());
            alerta.setContentText(
                    "Total: Q " + resultado.total().toPlainString()
                            + "\nPagos registrados correctamente."
                            + "\nInventario actualizado correctamente.");
            alerta.showAndWait();

            carrito.clear();
            txtObservaciones.clear();
            actualizarTotal();
            cargarProductos();

        } catch (SQLException | IllegalArgumentException e) {
            mostrarError(
                    "No se pudo completar la venta: " + e.getMessage());
        }
    }

    @FXML
    private void volverAlMenu() {
        if (usuarioAutenticado == null) {
            mostrarError(
                    "No se encontró la sesión del usuario. Inicia sesión nuevamente.");
            return;
        }

        String rol = usuarioAutenticado.getRol();

        if (rol == null || rol.isBlank()) {
            mostrarError("No se pudo determinar el rol del usuario.");
            return;
        }

        rol = rol.trim().toUpperCase(Locale.ROOT);

        String ruta;
        String titulo;

        switch (rol) {
            case "ADMINISTRADOR" -> {
                ruta = "/com/example/sistemagestion/view/menu_principal.fxml";
                titulo = "Sistema de Gestión - Administrador";
            }
            case "CAJERO" -> {
                ruta = "/com/example/sistemagestion/view/MenuCajero.fxml";
                titulo = "Sistema de Gestión - Cajero";
            }
            case "SUPERVISOR" -> {
                mostrarError(
                        "El menú del supervisor todavía no está conectado.");
                return;
            }
            default -> {
                mostrarError(
                        "El rol del usuario no tiene un menú configurado.");
                return;
            }
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(ruta));

            Parent root = loader.load();

            if ("ADMINISTRADOR".equals(rol)) {
                MenuPrincipalController controller =
                        loader.getController();
                controller.setUsuarioAutenticado(usuarioAutenticado);
            } else {
                MenuCajeroController controller =
                        loader.getController();
                controller.setUsuarioAutenticado(usuarioAutenticado);
            }

            Stage stage = (Stage) lblUsuario.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(titulo);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            mostrarError(
                    "No se pudo volver al menú: " + e.getMessage());
        }
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Punto de venta");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}

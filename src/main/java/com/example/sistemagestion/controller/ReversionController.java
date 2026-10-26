
package com.example.sistemagestion.controller;

import com.example.sistemagestion.controller.config.UsuarioDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class ReversionController {

    @FXML private Label lblUsuario;
    @FXML private ComboBox<ReversionDAO.DetalleDisponible> cmbDetalle;
    @FXML private TextField txtCantidad;
    @FXML private TextArea txtMotivo;
    @FXML private TableView<ReversionDAO.SolicitudPendiente> tablaSolicitudes;
    @FXML private TableColumn<ReversionDAO.SolicitudPendiente, Number> colId;
    @FXML private TableColumn<ReversionDAO.SolicitudPendiente, String> colVenta;
    @FXML private TableColumn<ReversionDAO.SolicitudPendiente, String> colProducto;
    @FXML private TableColumn<ReversionDAO.SolicitudPendiente, BigDecimal> colCantidad;
    @FXML private TableColumn<ReversionDAO.SolicitudPendiente, BigDecimal> colMonto;
    @FXML private TableColumn<ReversionDAO.SolicitudPendiente, String> colMotivo;
    @FXML private TableColumn<ReversionDAO.SolicitudPendiente, String> colSolicitante;
    @FXML private TableColumn<ReversionDAO.SolicitudPendiente, String> colFecha;
    @FXML private Button btnSolicitar;
    @FXML private Button btnAutorizar;
    @FXML private Button btnRechazar;

    private final ReversionDAO dao = new ReversionDAO();
    private UsuarioDAO.UsuarioAutenticado usuarioAutenticado;

    @FXML
    private void initialize() {
        colId.setCellValueFactory(c ->
                new javafx.beans.property.SimpleLongProperty(
                        c.getValue().reversionId()));
        colVenta.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().numeroVenta()));
        colProducto.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().producto()));
        colCantidad.setCellValueFactory(c ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        c.getValue().cantidad()));
        colMonto.setCellValueFactory(c ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        c.getValue().monto()));
        colMotivo.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().motivo()));
        colSolicitante.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().solicitante()));
        colFecha.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().fecha() == null ? ""
                                : c.getValue().fecha().format(
                                DateTimeFormatter.ofPattern(
                                        "dd/MM/yyyy HH:mm"))));

        tablaSolicitudes.getSelectionModel()
                .setSelectionMode(SelectionMode.SINGLE);

        btnAutorizar.setDisable(true);
        btnRechazar.setDisable(true);

        tablaSolicitudes.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, seleccionado) -> {
                    boolean seleccionadoValido = seleccionado != null;
                    btnAutorizar.setDisable(!seleccionadoValido);
                    btnRechazar.setDisable(!seleccionadoValido);
                });
    }

    public void setUsuarioAutenticado(
            UsuarioDAO.UsuarioAutenticado usuario) {
        this.usuarioAutenticado = usuario;

        if (lblUsuario != null && usuario != null) {
            lblUsuario.setText("Usuario: " + usuario.getNombreCompleto());
        }

        boolean puedeAutorizar = usuario != null
                && ("ADMINISTRADOR".equalsIgnoreCase(usuario.getRol())
                || "SUPERVISOR".equalsIgnoreCase(usuario.getRol()));

        if (btnAutorizar != null) {
            btnAutorizar.setVisible(puedeAutorizar);
            btnAutorizar.setManaged(puedeAutorizar);
        }
        if (btnRechazar != null) {
            btnRechazar.setVisible(puedeAutorizar);
            btnRechazar.setManaged(puedeAutorizar);
        }

        if (btnSolicitar != null) {
            btnSolicitar.setVisible(usuario != null);
            btnSolicitar.setManaged(usuario != null);
        }

        cargarDetalles();
        cargarSolicitudes();
    }

    @FXML
    private void solicitarReversion() {
        if (usuarioAutenticado == null) {
            mostrarError("No hay un usuario autenticado.");
            return;
        }

        ReversionDAO.DetalleDisponible detalle =
                cmbDetalle.getSelectionModel().getSelectedItem();

        if (detalle == null) {
            mostrarError("Selecciona el detalle de venta.");
            return;
        }

        try {
            BigDecimal cantidad = new BigDecimal(
                    txtCantidad.getText().trim());

            if (cantidad.signum() <= 0) {
                mostrarError("La cantidad debe ser mayor que cero.");
                return;
            }

            if (cantidad.compareTo(detalle.cantidadDisponible()) > 0) {
                mostrarError("La cantidad supera la disponible para reversión.");
                return;
            }

            String motivo = txtMotivo.getText();

            long id = dao.solicitarReversion(
                    detalle.detalleId(),
                    cantidad,
                    motivo,
                    usuarioAutenticado.getUsuarioId());

            mostrarInformacion(
                    "Solicitud registrada",
                    "La solicitud #" + id
                            + " quedó pendiente de autorización.");

            txtCantidad.clear();
            txtMotivo.clear();
            cargarDetalles();
            cargarSolicitudes();

        } catch (NumberFormatException e) {
            mostrarError("Ingresa una cantidad numérica válida.");
        } catch (SQLException | IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void autorizarReversion() {
        ReversionDAO.SolicitudPendiente seleccionada =
                tablaSolicitudes.getSelectionModel().getSelectedItem();

        if (seleccionada == null || usuarioAutenticado == null) {
            mostrarError("Selecciona una solicitud pendiente.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Autorizar reversión");
        confirmacion.setHeaderText("¿Autorizar la solicitud #"
                + seleccionada.reversionId() + "?");
        confirmacion.setContentText(
                "Se restituirán " + seleccionada.cantidad()
                        + " unidades al inventario.");

        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL)
                != ButtonType.OK) {
            return;
        }

        try {
            dao.autorizarReversion(
                    seleccionada.reversionId(),
                    usuarioAutenticado.getUsuarioId());

            mostrarInformacion("Reversión autorizada",
                    "Se actualizó la solicitud y el inventario.");
            cargarDetalles();
            cargarSolicitudes();

        } catch (SQLException | IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void rechazarReversion() {
        ReversionDAO.SolicitudPendiente seleccionada =
                tablaSolicitudes.getSelectionModel().getSelectedItem();

        if (seleccionada == null || usuarioAutenticado == null) {
            mostrarError("Selecciona una solicitud pendiente.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Rechazar reversión");
        confirmacion.setHeaderText("¿Rechazar la solicitud #"
                + seleccionada.reversionId() + "?");

        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL)
                != ButtonType.OK) {
            return;
        }

        try {
            dao.rechazarReversion(
                    seleccionada.reversionId(),
                    usuarioAutenticado.getUsuarioId());

            mostrarInformacion("Solicitud rechazada",
                    "La solicitud fue rechazada.");
            cargarSolicitudes();

        } catch (SQLException | IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void actualizarListas() {
        cargarDetalles();
        cargarSolicitudes();
    }

    @FXML
    private void volver() {
        Stage stage = (Stage) lblUsuario.getScene().getWindow();
        stage.close();
    }

    private void cargarDetalles() {
        if (cmbDetalle == null) return;

        try {
            cmbDetalle.setItems(
                    FXCollections.observableArrayList(
                            dao.listarDetallesDisponibles()));
        } catch (SQLException e) {
            mostrarError("No se pudieron cargar los detalles de venta: "
                    + e.getMessage());
        }
    }

    private void cargarSolicitudes() {
        if (tablaSolicitudes == null) return;

        try {
            tablaSolicitudes.setItems(
                    FXCollections.observableArrayList(
                            dao.listarSolicitudesPendientes()));
        } catch (SQLException e) {
            mostrarError("No se pudieron cargar las solicitudes: "
                    + e.getMessage());
        }
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Reversiones");
        alert.setHeaderText("No se pudo completar la operación");
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarInformacion(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Reversiones");
        alert.setHeaderText(titulo);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }


    @FXML
    private void regresarMenuAdministrador() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/sistemagestion/view/menu_principal.fxml"
                    )
            );

            Parent root = loader.load();

            MenuPrincipalController controller = loader.getController();
            controller.setUsuarioAutenticado(usuarioAutenticado);

            Stage stage = (Stage) tablaSolicitudes.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Sistema de Gestión - Administrador");
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();

            javafx.scene.control.Alert alerta =
                    new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.ERROR
                    );

            alerta.setTitle("Error");
            alerta.setHeaderText("No se pudo regresar al menú");
            alerta.setContentText(e.getMessage());
            alerta.showAndWait();
        }
    }

}

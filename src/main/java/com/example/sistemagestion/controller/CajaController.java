
package com.example.sistemagestion.controller;

import com.example.sistemagestion.controller.config.UsuarioDAO;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

public class CajaController {

    @FXML
    private ComboBox<TurnoDAO.CajaOption> cmbCaja;

    @FXML
    private TextField txtFondoInicial;

    @FXML
    private TextField txtEfectivoDeclarado;

    @FXML
    private TextArea txtObservaciones;

    @FXML
    private Label lblEstado;

    private final TurnoDAO turnoDAO = new TurnoDAO();

    private Long cajeroId;
    private UsuarioDAO.UsuarioAutenticado usuarioAutenticado;

    public void setCajeroId(long cajeroId) {
        if (cajeroId <= 0) {
            throw new IllegalArgumentException(
                    "El ID del cajero debe ser válido."
            );
        }
        this.cajeroId = cajeroId;
    }

    public void setUsuarioAutenticado(
            UsuarioDAO.UsuarioAutenticado usuarioAutenticado) {

        this.usuarioAutenticado = usuarioAutenticado;

        if (usuarioAutenticado != null) {
            this.cajeroId = usuarioAutenticado.getUsuarioId();
        }
    }

    @FXML
    public void initialize() {
        cargarCajas();
    }

    @FXML
    public void cargarCajas() {
        try {
            cmbCaja.setItems(
                    FXCollections.observableArrayList(
                            turnoDAO.listarCajasActivas()
                    )
            );

            lblEstado.setText(
                    cmbCaja.getItems().isEmpty()
                            ? "No hay cajas activas."
                            : "Seleccione una caja para consultar su estado."
            );

        } catch (SQLException e) {
            mostrarError("No se pudieron cargar las cajas.", e);
        }
    }

    @FXML
    public void consultarTurno() {
        TurnoDAO.CajaOption caja = cmbCaja.getValue();

        if (caja == null) {
            mostrarMensaje("Seleccione una caja.");
            return;
        }

        try {
            Long turnoId = turnoDAO.buscarTurnoAbierto(caja.id());

            if (turnoId == null) {
                lblEstado.setText(
                        caja.codigo() + " no tiene un turno abierto."
                );
            } else {
                lblEstado.setText(
                        turnoDAO.obtenerResumenTurno(turnoId)
                );
            }

        } catch (SQLException e) {
            mostrarError("No se pudo consultar el turno.", e);
        }
    }

    @FXML
    public void abrirTurno() {
        TurnoDAO.CajaOption caja = cmbCaja.getValue();

        if (caja == null) {
            mostrarMensaje("Seleccione una caja registradora.");
            return;
        }

        if (cajeroId == null || cajeroId <= 0) {
            mostrarMensaje("No se identificó al cajero autenticado.");
            return;
        }

        BigDecimal fondo;

        try {
            String valor = txtFondoInicial.getText();

            if (valor == null || valor.isBlank()) {
                mostrarMensaje("Ingrese el fondo inicial.");
                return;
            }

            fondo = new BigDecimal(valor.trim().replace(",", "."));

            if (fondo.scale() > 2 || fondo.signum() <= 0) {
                mostrarMensaje(
                        "El fondo debe ser mayor que cero y tener "
                                + "máximo dos decimales."
                );
                return;
            }

            fondo = fondo.setScale(2);

        } catch (NumberFormatException e) {
            mostrarMensaje("Ingrese un fondo válido. Ejemplo: 500.00");
            return;
        }

        try {
            if (turnoDAO.buscarTurnoAbierto(caja.id()) != null) {
                mostrarMensaje("Esta caja ya tiene un turno abierto.");
                consultarTurno();
                return;
            }

            long turnoId = turnoDAO.abrirTurno(
                    caja.id(),
                    cajeroId,
                    fondo,
                    txtObservaciones.getText()
            );

            txtFondoInicial.clear();
            txtObservaciones.clear();

            mostrarMensaje(
                    "Turno abierto correctamente.\n"
                            + "Número: " + turnoId
                            + "\nCaja: " + caja.codigo()
                            + "\nFondo inicial: Q " + fondo
            );

            consultarTurno();

        } catch (SQLException e) {
            mostrarError("No se pudo abrir el turno.", e);
        }
    }

    @FXML
    public void cerrarTurno() {
        TurnoDAO.CajaOption caja = cmbCaja.getValue();

        if (caja == null) {
            mostrarMensaje("Seleccione la caja que desea cerrar.");
            return;
        }

        String valor = txtEfectivoDeclarado.getText();

        if (valor == null || valor.isBlank()) {
            mostrarMensaje("Ingrese el efectivo contado en caja.");
            txtEfectivoDeclarado.requestFocus();
            return;
        }

        BigDecimal efectivo;

        try {
            efectivo = new BigDecimal(valor.trim().replace(",", "."));

            if (efectivo.scale() > 2 || efectivo.signum() < 0) {
                mostrarMensaje(
                        "El efectivo debe ser cero o mayor y tener "
                                + "máximo dos decimales."
                );
                return;
            }

            efectivo = efectivo.setScale(2);

        } catch (NumberFormatException e) {
            mostrarMensaje(
                    "Ingrese un importe válido. Ejemplo: 1250.00"
            );
            txtEfectivoDeclarado.requestFocus();
            return;
        }

        try {
            Long turnoId = turnoDAO.buscarTurnoAbierto(caja.id());

            if (turnoId == null) {
                mostrarMensaje("Esta caja no tiene un turno abierto.");
                consultarTurno();
                return;
            }

            Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacion.setTitle("Confirmar cierre");
            confirmacion.setHeaderText("¿Desea cerrar el turno?");
            confirmacion.setContentText(
                    "Caja: " + caja.codigo()
                            + "\nTurno: " + turnoId
                            + "\nEfectivo contado: Q " + efectivo
            );

            if (confirmacion.showAndWait().orElse(null)
                    != javafx.scene.control.ButtonType.OK) {
                return;
            }

            turnoDAO.cerrarTurno(
                    turnoId,
                    efectivo,
                    txtObservaciones.getText()
            );

            txtEfectivoDeclarado.clear();
            txtObservaciones.clear();

            mostrarMensaje(
                    "Turno " + turnoId + " cerrado correctamente."
                            + "\nEfectivo contado: Q " + efectivo
                            + "\nLa diferencia quedó registrada en el sistema."
            );

            consultarTurno();

        } catch (SQLException e) {
            mostrarError("No se pudo cerrar el turno.", e);
        }
    }

    @FXML
    public void volverAlMenu() {
        if (usuarioAutenticado == null) {
            mostrarMensaje(
                    "No se encontró la sesión del cajero. "
                            + "Inicie sesión nuevamente."
            );
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/sistemagestion/view/MenuCajero.fxml"
                    )
            );

            Parent root = loader.load();

            MenuCajeroController menuController = loader.getController();
            menuController.setUsuarioAutenticado(usuarioAutenticado);

            Stage stage = (Stage) lblEstado.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Sistema de Gestión - Cajero");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException | RuntimeException e) {
            mostrarError("No se pudo volver al menú del cajero.", e);
        }
    }

    private void mostrarMensaje(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Gestión de caja");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String mensaje, Exception e) {
        String detalle = e.getMessage() == null
                ? "No se proporcionaron detalles."
                : e.getMessage();

        lblEstado.setText(mensaje);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de gestión de caja");
        alert.setHeaderText(mensaje);
        alert.setContentText(detalle);
        alert.showAndWait();

        e.printStackTrace();
    }
}

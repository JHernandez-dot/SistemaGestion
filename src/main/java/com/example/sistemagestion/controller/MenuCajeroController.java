
package com.example.sistemagestion.controller;

import com.example.sistemagestion.controller.config.UsuarioDAO;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class MenuCajeroController {

    @FXML
    private Label lblUsuario;

    private UsuarioDAO.UsuarioAutenticado usuarioAutenticado;

    public UsuarioDAO.UsuarioAutenticado getUsuarioAutenticado() {
        return usuarioAutenticado;
    }

    public void setUsuarioAutenticado(
            UsuarioDAO.UsuarioAutenticado usuarioAutenticado) {

        this.usuarioAutenticado = usuarioAutenticado;

        if (lblUsuario != null && usuarioAutenticado != null) {
            lblUsuario.setText(
                    "Usuario: " + usuarioAutenticado.getNombreCompleto()
            );
        }
    }

    @FXML
    private void abrirCaja() {
        if (usuarioAutenticado == null) {
            mostrarMensaje(
                    Alert.AlertType.ERROR,
                    "Sesión no válida",
                    "No se encontró el usuario autenticado."
            );
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/sistemagestion/view/CajaView.fxml"
                    )
            );

            Parent root = loader.load();

            CajaController controller = loader.getController();
            controller.setUsuarioAutenticado(usuarioAutenticado);

            mostrarEscena(root, "Gestión de caja");

        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            mostrarMensaje(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo abrir la gestión de caja: " + e.getMessage()
            );
        }
    }

    @FXML
    private void abrirVentas() {
        if (usuarioAutenticado == null) {
            mostrarMensaje(
                    Alert.AlertType.ERROR,
                    "Sesión no válida",
                    "No se encontró el usuario autenticado. "
                            + "Inicia sesión nuevamente."
            );
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/sistemagestion/view/VentasView.fxml"
                    )
            );

            Parent root = loader.load();

            VentasController controller = loader.getController();
            controller.setUsuarioAutenticado(usuarioAutenticado);

            mostrarEscena(root, "Punto de venta");

        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            mostrarMensaje(
                    Alert.AlertType.ERROR,
                    "Error al abrir ventas",
                    "No se pudo abrir el punto de venta: " + e.getMessage()
            );
        }
    }

    @FXML
    private void cerrarSesion() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/sistemagestion/view/login.fxml"
                    )
            );

            Parent root = loader.load();

            Stage stage = (Stage) lblUsuario.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Inicio de sesión");
            stage.setMaximized(false);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            mostrarMensaje(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo volver al inicio de sesión."
            );
        }
    }

    private void mostrarEscena(Parent root, String titulo) {
        Stage stage = (Stage) lblUsuario.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle(titulo);
        stage.centerOnScreen();
        stage.show();
    }

    private void mostrarMensaje(
            Alert.AlertType tipo,
            String titulo,
            String mensaje) {

        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}

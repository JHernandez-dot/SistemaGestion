
package com.example.sistemagestion.controller;

import com.example.sistemagestion.controller.config.UsuarioDAO;
import com.example.sistemagestion.controller.config.PasswordUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private TextField txtUsuario;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Label lblMensaje;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    private void iniciarSesion() {
        String usuario = txtUsuario.getText().trim();
        String password = txtPassword.getText();

        if (usuario.isEmpty() || password.isEmpty()) {
            lblMensaje.setText("Completa todos los campos.");
            return;
        }

        try {
            UsuarioDAO.UsuarioAutenticado usuarioAutenticado =
                    usuarioDAO.buscarPorUsername(usuario);

            if (usuarioAutenticado == null) {
                lblMensaje.setText(
                        "Usuario no válido, inactivo o bloqueado."
                );
                return;
            }

            String hash = usuarioDAO.obtenerHashPassword(usuario);

            if (hash == null || !PasswordUtil.verificar(password, hash)) {
                lblMensaje.setText("Usuario o contraseña incorrectos.");
                return;
            }

            if (!"ADMINISTRADOR".equalsIgnoreCase(
                    usuarioAutenticado.getRol())) {
                lblMensaje.setText(
                        "No tienes acceso al menú de administración."
                );
                return;
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/sistemagestion/view/menu_principal.fxml"
                    )
            );

            Parent root = loader.load();

            Stage stage = (Stage) txtUsuario
                    .getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("Sistema de Gestión - Supermercado");
            stage.setMaximized(true);
            stage.show();

        } catch (SQLException e) {
            lblMensaje.setText(
                    "No se pudo consultar la base de datos."
            );
            e.printStackTrace();

        } catch (IOException | RuntimeException e) {
            lblMensaje.setText(
                    "Error al abrir el menú principal."
            );
            e.printStackTrace();
        }
    }
}

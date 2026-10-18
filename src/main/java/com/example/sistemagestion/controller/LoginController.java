package com.example.sistemagestion.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField txtUsuario;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Label lblMensaje;

    @FXML
    private void iniciarSesion() {
        String usuario = txtUsuario.getText().trim();
        String password = txtPassword.getText();

        if (usuario.isEmpty() || password.isEmpty()) {
            lblMensaje.setText("Completa todos los campos.");
            return;
        }

        if (usuario.equals("admin") && password.equals("12345")) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource(
                                "/com/example/sistemagestion/view/menu_principal.fxml"
                        )
                );

                Parent root = loader.load();

                Stage stage = (Stage) txtUsuario
                        .getScene().getWindow();

                stage.setScene(new Scene(root));
                stage.setTitle(
                        "Sistema de Gestión - Supermercado"
                );
                stage.setMaximized(true);
                stage.show();

            } catch (IOException | RuntimeException e) {
                lblMensaje.setText(
                        "Error al abrir el menú principal."
                );
                e.printStackTrace();
            }
        } else {
            lblMensaje.setText(
                    "Usuario o contraseña incorrectos."
            );
        }
    }
}

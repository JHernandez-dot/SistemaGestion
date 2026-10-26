
package com.example.sistemagestion.controller;

import com.example.sistemagestion.controller.config.PasswordUtil;
import com.example.sistemagestion.controller.config.UsuarioDAO;

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
                usuarioDAO.registrarIntentoFallido(usuario);

                lblMensaje.setText(
                        "Usuario o contraseña incorrectos. "
                                + "Después de 5 intentos fallidos, "
                                + "la cuenta se bloqueará temporalmente."
                );
                return;
            }

            usuarioDAO.restablecerIntentosFallidos(usuario);

            String rol = usuarioAutenticado.getRol();

            if (rol == null || rol.isBlank()) {
                lblMensaje.setText(
                        "El usuario no tiene un rol asignado."
                );
                return;
            }

            String rutaFXML;
            String tituloVentana;
            boolean maximizar;

            switch (rol.trim().toUpperCase()) {
                case "ADMINISTRADOR":
                    rutaFXML =
                            "/com/example/sistemagestion/view/menu_principal.fxml";
                    tituloVentana = "Sistema de Gestión - Administrador";
                    maximizar = true;
                    break;

                case "CAJERO":
                    rutaFXML =
                            "/com/example/sistemagestion/view/MenuCajero.fxml";
                    tituloVentana = "Sistema de Gestión - Cajero";
                    maximizar = true;
                    break;

                case "SUPERVISOR":
                    lblMensaje.setText(
                            "El menú del supervisor todavía está pendiente."
                    );
                    return;

                default:
                    lblMensaje.setText(
                            "El rol asignado no tiene acceso al sistema."
                    );
                    return;
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(rutaFXML)
            );

            Parent root = loader.load();

            // Entregar la información del usuario al menú correspondiente.
            if ("ADMINISTRADOR".equalsIgnoreCase(rol.trim())) {
                MenuPrincipalController menuController =
                        loader.getController();

                menuController.setUsuarioAutenticado(usuarioAutenticado);

            } else if ("CAJERO".equalsIgnoreCase(rol.trim())) {
                MenuCajeroController menuController =
                        loader.getController();

                menuController.setUsuarioAutenticado(usuarioAutenticado);
            }

            Stage stage = (Stage) txtUsuario.getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle(tituloVentana);
            stage.setMaximized(maximizar);
            stage.show();

        } catch (SQLException e) {
            lblMensaje.setText(
                    "No se pudo consultar la base de datos."
            );
            e.printStackTrace();

        } catch (IOException | RuntimeException e) {
            lblMensaje.setText(
                    "No se pudo abrir el menú correspondiente. "
                            + "Verifica que el archivo FXML y su controlador existan."
            );
            e.printStackTrace();
        }
    }
}

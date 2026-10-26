
package com.example.sistemagestion.controller;

import com.example.sistemagestion.controller.config.UsuarioDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javafx.scene.control.Alert;
public class MenuPrincipalController {

    @FXML private Label lblFecha;
    @FXML private Label lblVentas;
    @FXML private Label lblProductos;
    @FXML private Label lblStockBajo;
    @FXML private Label lblEstado;

    private UsuarioDAO.UsuarioAutenticado usuarioAutenticado;

    public void setUsuarioAutenticado(
            UsuarioDAO.UsuarioAutenticado usuarioAutenticado) {
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @FXML
    private void initialize() {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern(
                "EEEE, dd 'de' MMMM 'de' yyyy HH:mm",
                new Locale("es", "GT")
        );

        lblFecha.setText(LocalDateTime.now().format(formato));

        // Valores provisionales hasta conectar las consultas a la BD.
        lblVentas.setText("Q 0.00");
        lblProductos.setText("0");
        lblStockBajo.setText("0");
    }

    @FXML
    private void mostrarInicio() {
        lblEstado.setText("Estás en el menú principal.");
    }

    @FXML
    private void mostrarVentas() {
        if (usuarioAutenticado == null) {
            mostrarMensaje("No se encontró la sesión del usuario. Inicia sesión nuevamente.");
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

            Stage stage = (Stage) lblEstado.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Punto de venta - Administrador");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException | RuntimeException e) {
            mostrarMensaje(
                    "No se pudo abrir el módulo de ventas: " + e.getMessage()
            );
            e.printStackTrace();
        }
    }


    @FXML
    private void mostrarProductos() {
        abrirVentana(
                "/com/example/sistemagestion/view/productos.fxml",
                "Gestión de Productos"
        );
    }

    @FXML
    private void mostrarCategorias() {
        abrirVentana(
                "/com/example/sistemagestion/view/categorias.fxml",
                "Gestión de Categorías"
        );
    }


    @FXML
    private void mostrarInventario() {
        if (usuarioAutenticado == null) {
            mostrarMensaje("No se encontró la sesión del usuario.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/sistemagestion/view/InventarioView.fxml"
                    )
            );

            Parent root = loader.load();

            InventarioController controller = loader.getController();
            controller.setUsuarioAutenticado(usuarioAutenticado);

            Stage stage = (Stage) lblEstado.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Control de Inventario");
            stage.centerOnScreen();

        } catch (IOException | RuntimeException e) {
            mostrarMensaje("No se pudo abrir inventario: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @FXML
    private void mostrarUsuarios() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/sistemagestion/view/UsuariosView.fxml"
                    )
            );

            Parent root = loader.load();

            UsuariosController controller = loader.getController();
            controller.setUsuarioAutenticado(usuarioAutenticado);

            Stage stage = (Stage) lblEstado.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Administración de usuarios");
            stage.centerOnScreen();

        } catch (IOException | RuntimeException e) {
            mostrarMensaje(
                    "No se pudo abrir la administración de usuarios: "
                            + e.getMessage()
            );
            e.printStackTrace();
        }
    }

    private void mostrarMensaje(String mensaje) {
        lblEstado.setText(mensaje);
    }

    private void abrirVentana(String ruta, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(ruta)
            );

            Parent root = loader.load();

            Stage stage = (Stage) lblEstado.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 650));
            stage.setTitle(titulo);
            stage.centerOnScreen();

        } catch (IOException | RuntimeException e) {
            mostrarMensaje(
                    "No se pudo abrir la pantalla: " + titulo
            );
            e.printStackTrace();
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

            Stage stage = (Stage) lblEstado.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Sistema de Gestión - Inicio de sesión");
            stage.centerOnScreen();

        } catch (IOException | RuntimeException e) {
            mostrarMensaje(
                    "No se pudo regresar al inicio de sesión. "
                            + "Revisa la ruta de login.fxml."
            );
            e.printStackTrace();
        }
    }

    @FXML
    private void mostrarReversiones() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/sistemagestion/view/ReversionView.fxml"
                    )
            );

            Parent root = loader.load();

            ReversionController controller = loader.getController();
            controller.setUsuarioAutenticado(usuarioAutenticado);

            Stage stage = (Stage) lblEstado.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Gestión de Reversiones");
            stage.centerOnScreen();

        } catch (IOException | RuntimeException e) {
            mostrarMensaje(
                    "No se pudo abrir el módulo de reversiones: "
                            + e.getMessage()
            );
            e.printStackTrace();
        }
    }
}

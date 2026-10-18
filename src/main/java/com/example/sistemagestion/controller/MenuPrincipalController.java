package com.example.sistemagestion.controller;

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

public class MenuPrincipalController {

    @FXML private Label lblFecha;
    @FXML private Label lblVentas;
    @FXML private Label lblProductos;
    @FXML private Label lblClientes;
    @FXML private Label lblStockBajo;
    @FXML private Label lblEstado;

    @FXML
    private void initialize() {
        DateTimeFormatter formato =
                DateTimeFormatter.ofPattern(
                        "EEEE, dd 'de' MMMM 'de' yyyy HH:mm",
                        new Locale("es", "GT")
                );

        lblFecha.setText(
                LocalDateTime.now().format(formato)
        );

        // Valores iniciales de demostración.
        // Después se obtendrán de la base de datos.
        lblVentas.setText("Q 0.00");
        lblProductos.setText("0");
        lblClientes.setText("0");
        lblStockBajo.setText("0");
    }

    @FXML
    private void mostrarInicio() {
        lblEstado.setText("Estás en el menú principal.");
    }

    @FXML
    private void mostrarVentas() {
        mostrarMensaje("Módulo de ventas: pendiente de implementar.");
    }

    @FXML
    private void mostrarProductos() {
        abrirVentana(
                "/com/example/sistemagestion/view/productos.fxml",
                "Gestión de Productos"
        );

        Stage stageActual = (Stage) lblEstado.getScene().getWindow();
        stageActual.close();
    }
    @FXML
    private void mostrarCategorias() {
        abrirVentana(
                "/com/example/sistemagestion/view/categorias.fxml",
                "Gestión de Categorías"
        );

        // Cerrar la ventana del menú principal
        Stage stageActual = (Stage) lblEstado.getScene().getWindow();
        stageActual.close();
    }
    @FXML
    private void mostrarInventario() {
        mostrarMensaje("Módulo de inventario: pendiente de implementar.");
    }

    @FXML
    private void mostrarClientes() {
        mostrarMensaje("Módulo de clientes: pendiente de implementar.");
    }

    @FXML
    private void mostrarProveedores() {
        mostrarMensaje("Módulo de proveedores: pendiente de implementar.");
    }

    @FXML
    private void mostrarReportes() {
        mostrarMensaje("Módulo de reportes: pendiente de implementar.");
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

            Stage stage = new Stage();
            stage.setTitle(titulo);
            stage.setScene(new Scene(root, 1000, 650));
            stage.centerOnScreen();
            stage.show();

        } catch (IOException | RuntimeException e) {
            lblEstado.setText("No se pudo abrir la pantalla: " + titulo);
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
            stage.show();

        } catch (IOException | RuntimeException e) {
            lblEstado.setText(
                    "No se pudo regresar al inicio de sesión. Revisa la ruta de login.fxml."
            );
            e.printStackTrace();
        }
    }

}

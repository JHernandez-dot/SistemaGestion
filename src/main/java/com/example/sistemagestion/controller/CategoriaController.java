
package com.example.sistemagestion.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.SQLException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class CategoriaController {

    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;

    @FXML private TableView<CategoriaDAO.Categoria> tablaCategorias;
    @FXML private TableColumn<CategoriaDAO.Categoria, Long> colId;
    @FXML private TableColumn<CategoriaDAO.Categoria, String> colNombre;
    @FXML private TableColumn<CategoriaDAO.Categoria, String> colDescripcion;
    @FXML private TableColumn<CategoriaDAO.Categoria, Boolean> colActivo;

    @FXML private Button btnDesactivar;

    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    @FXML
    private void initialize() {
        colId.setCellValueFactory(
                new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre"));
        colDescripcion.setCellValueFactory(
                new PropertyValueFactory<>("descripcion"));
        colActivo.setCellValueFactory(
                new PropertyValueFactory<>("activo"));

        cargarCategorias();
    }

    @FXML
    private void registrarCategoria() {
        String nombre = txtNombre.getText().trim();
        String descripcion = txtDescripcion.getText().trim();

        if (nombre.isEmpty()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Validación",
                    "Ingresa el nombre de la categoría."
            );
            return;
        }

        try {
            categoriaDAO.registrar(nombre, descripcion);

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Éxito",
                    "Categoría registrada correctamente."
            );

            txtNombre.clear();
            txtDescripcion.clear();
            cargarCategorias();

        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                mostrarAlerta(
                        Alert.AlertType.WARNING,
                        "Categoría duplicada",
                        "Ya existe una categoría con ese nombre."
                );
            } else {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Error de base de datos",
                        "No se pudo registrar la categoría."
                );
                e.printStackTrace();
            }
        }
    }
    @FXML
    private void regresarMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/sistemagestion/view/menu_principal.fxml"
                    )
            );

            Parent root = loader.load();

            Stage stage = (Stage) tablaCategorias.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Sistema de Gestión - Menú Principal");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException | RuntimeException e) {
            e.printStackTrace();

            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Error de navegación");
            alerta.setHeaderText("No se pudo regresar al menú principal");
            alerta.setContentText("Verifica la ruta de menu_principal.fxml.");
            alerta.showAndWait();
        }
    }
    @FXML
    private void desactivarCategoria() {
        CategoriaDAO.Categoria seleccionada =
                tablaCategorias.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Selección requerida",
                    "Selecciona una categoría de la tabla."
            );
            return;
        }

        if (!seleccionada.isActivo()) {
            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Categoría inactiva",
                    "La categoría seleccionada ya está desactivada."
            );
            return;
        }

        try {
            categoriaDAO.desactivar(seleccionada.getId());
            cargarCategorias();

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Éxito",
                    "Categoría desactivada correctamente."
            );

        } catch (SQLException e) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo desactivar la categoría."
            );
            e.printStackTrace();
        }
    }

    private void cargarCategorias() {
        try {
            tablaCategorias.setItems(
                    FXCollections.observableArrayList(
                            categoriaDAO.listar()
                    )
            );
        } catch (SQLException e) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error de conexión",
                    "No se pudieron cargar las categorías."
            );
            e.printStackTrace();
        }
    }

    private void mostrarAlerta(
            Alert.AlertType tipo, String titulo, String mensaje) {

        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}

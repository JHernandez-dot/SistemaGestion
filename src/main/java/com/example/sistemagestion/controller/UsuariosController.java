
package com.example.sistemagestion.controller;

import com.example.sistemagestion.controller.config.UsuarioAdministracionDAO;
import com.example.sistemagestion.controller.config.UsuarioDAO;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuariosController {

    @FXML
    private TableView<UsuarioAdministracionDAO.Usuario> tablaUsuarios;

    @FXML
    private TableColumn<UsuarioAdministracionDAO.Usuario, Number> colId;

    @FXML
    private TableColumn<UsuarioAdministracionDAO.Usuario, String> colUsername;

    @FXML
    private TableColumn<UsuarioAdministracionDAO.Usuario, String> colNombre;

    @FXML
    private TableColumn<UsuarioAdministracionDAO.Usuario, String> colRol;

    @FXML
    private TableColumn<UsuarioAdministracionDAO.Usuario, String> colEstado;

    @FXML
    private TextField txtBuscar;

    private final UsuarioAdministracionDAO dao =
            new UsuarioAdministracionDAO();

    private UsuarioDAO.UsuarioAutenticado usuarioAutenticado;

    private final ObservableList<UsuarioAdministracionDAO.Usuario>
            usuariosCargados = FXCollections.observableArrayList();

    // Recibe al usuario autenticado desde MenuPrincipalController.
    public void setUsuarioAutenticado(
            UsuarioDAO.UsuarioAutenticado usuario) {

        this.usuarioAutenticado = usuario;

        if (!esAdministrador()) {
            mostrarAviso(
                    "Acceso denegado. Solo el administrador puede " +
                            "gestionar usuarios."
            );
            usuariosCargados.clear();

            if (tablaUsuarios != null) {
                tablaUsuarios.getItems().clear();
            }
            return;
        }

        cargarUsuarios();
    }

    private boolean esAdministrador() {
        return usuarioAutenticado != null
                && usuarioAutenticado.getRol() != null
                && "ADMINISTRADOR".equalsIgnoreCase(
                usuarioAutenticado.getRol());
    }

    private void verificarAdministrador() {
        if (!esAdministrador()) {
            throw new SecurityException(
                    "Solo el administrador puede gestionar usuarios.");
        }
    }

    @FXML
    private void initialize() {

        colId.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().usuarioId()));

        colUsername.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().username()));

        colNombre.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().nombreCompleto()));

        colRol.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().rol() == null
                                ? "Sin rol"
                                : data.getValue().rol()));

        colEstado.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().activo()
                                ? "Activo"
                                : "Inactivo"));

        tablaUsuarios.setItems(usuariosCargados);

        // Filtra los datos cargados en memoria, sin consultar
        // la base de datos cada vez que se escribe.
        txtBuscar.textProperty().addListener(
                (observable, anterior, nuevo) ->
                        filtrarUsuarios(nuevo));

        // La carga se realiza después de recibir la sesión.
    }

    private void cargarUsuarios() {
        try {
            verificarAdministrador();

            List<UsuarioAdministracionDAO.Usuario> lista =
                    dao.listarUsuarios();

            usuariosCargados.setAll(lista);
            filtrarUsuarios(txtBuscar.getText());

        } catch (SQLException | SecurityException e) {
            mostrarError("No se pudieron cargar los usuarios.", e);
        }
    }

    private void filtrarUsuarios(String texto) {
        if (tablaUsuarios == null) {
            return;
        }

        String filtro = texto == null
                ? ""
                : texto.trim().toLowerCase();

        List<UsuarioAdministracionDAO.Usuario> filtrados =
                usuariosCargados.stream()
                        .filter(usuario ->
                                contiene(usuario.username(), filtro)
                                        || contiene(
                                        usuario.nombreCompleto(), filtro)
                                        || contiene(usuario.rol(), filtro))
                        .toList();

        tablaUsuarios.setItems(
                FXCollections.observableArrayList(filtrados));
    }

    private boolean contiene(String valor, String filtro) {
        return valor != null
                && valor.toLowerCase().contains(filtro);
    }

    @FXML
    private void actualizarLista() {
        cargarUsuarios();
    }

    @FXML
    private void crearUsuario() {
        try {
            verificarAdministrador();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Registrar usuario");
            dialog.setHeaderText("Datos del nuevo usuario");

            TextField username = new TextField();
            username.setPromptText("Nombre de usuario");

            TextField nombre = new TextField();
            nombre.setPromptText("Nombre completo");

            PasswordField password = new PasswordField();
            password.setPromptText("Contraseña inicial (mínimo 8 caracteres)");

            ComboBox<UsuarioAdministracionDAO.Rol> rol =
                    new ComboBox<>(
                            FXCollections.observableArrayList(
                                    dao.listarRolesActivos()));

            rol.setPromptText("Seleccionar rol");
            rol.setMaxWidth(Double.MAX_VALUE);

            GridPane formulario = new GridPane();
            formulario.setHgap(10);
            formulario.setVgap(10);
            formulario.addRow(0, new Label("Usuario:"), username);
            formulario.addRow(1, new Label("Nombre completo:"), nombre);
            formulario.addRow(2, new Label("Contraseña:"), password);
            formulario.addRow(3, new Label("Rol:"), rol);

            dialog.getDialogPane().setContent(formulario);
            dialog.getDialogPane().getButtonTypes().addAll(
                    ButtonType.OK, ButtonType.CANCEL);

            Optional<ButtonType> resultado = dialog.showAndWait();

            if (resultado.isEmpty()
                    || resultado.get() != ButtonType.OK) {
                return;
            }

            if (username.getText().isBlank()
                    || nombre.getText().isBlank()
                    || password.getText().isBlank()) {
                mostrarAviso("Completa todos los campos.");
                return;
            }

            if (password.getText().length() < 8) {
                mostrarAviso(
                        "La contraseña debe tener al menos 8 caracteres.");
                return;
            }

            if (rol.getValue() == null) {
                mostrarAviso("Selecciona un rol.");
                return;
            }

            dao.crearUsuario(
                    username.getText().trim(),
                    nombre.getText().trim(),
                    password.getText(),
                    rol.getValue().rolId());

            mostrarInformacion("Usuario registrado correctamente.");
            cargarUsuarios();

        } catch (SQLException | IllegalArgumentException
                 | SecurityException e) {
            mostrarError("No se pudo registrar el usuario.", e);
        }
    }

    @FXML
    private void editarUsuario() {
        try {
            verificarAdministrador();

            UsuarioAdministracionDAO.Usuario seleccionado =
                    tablaUsuarios.getSelectionModel().getSelectedItem();

            if (seleccionado == null) {
                mostrarAviso("Selecciona un usuario de la tabla.");
                return;
            }

            TextInputDialog dialog =
                    new TextInputDialog(seleccionado.nombreCompleto());

            dialog.setTitle("Editar usuario");
            dialog.setHeaderText(
                    "Usuario: " + seleccionado.username());
            dialog.setContentText("Nombre completo:");

            Optional<String> resultado = dialog.showAndWait();

            if (resultado.isEmpty()) {
                return;
            }

            String nuevoNombre = resultado.get().trim();

            if (nuevoNombre.isBlank()) {
                mostrarAviso("El nombre completo es obligatorio.");
                return;
            }

            dao.actualizarUsuario(
                    seleccionado.usuarioId(),
                    nuevoNombre);

            mostrarInformacion("Usuario actualizado correctamente.");
            cargarUsuarios();

        } catch (SQLException | IllegalArgumentException
                 | SecurityException e) {
            mostrarError("No se pudo actualizar el usuario.", e);
        }
    }

    @FXML
    private void cambiarPassword() {
        try {
            verificarAdministrador();

            UsuarioAdministracionDAO.Usuario seleccionado =
                    tablaUsuarios.getSelectionModel().getSelectedItem();

            if (seleccionado == null) {
                mostrarAviso("Selecciona un usuario.");
                return;
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Cambiar contraseña");
            dialog.setHeaderText(
                    "Nueva contraseña para " + seleccionado.username());

            PasswordField password = new PasswordField();
            password.setPromptText("Mínimo 8 caracteres");

            VBox contenido = new VBox(
                    10,
                    new Label("Nueva contraseña:"),
                    password);

            contenido.setPadding(new javafx.geometry.Insets(10));

            dialog.getDialogPane().setContent(contenido);
            dialog.getDialogPane().getButtonTypes().addAll(
                    ButtonType.OK, ButtonType.CANCEL);

            Optional<ButtonType> resultado = dialog.showAndWait();

            if (resultado.isEmpty()
                    || resultado.get() != ButtonType.OK) {
                return;
            }

            if (password.getText().isBlank()
                    || password.getText().length() < 8) {
                mostrarAviso(
                        "La contraseña debe tener al menos 8 caracteres.");
                return;
            }

            dao.cambiarPassword(
                    seleccionado.usuarioId(),
                    password.getText());

            mostrarInformacion("Contraseña actualizada correctamente.");

        } catch (SQLException | IllegalArgumentException
                 | SecurityException e) {
            mostrarError("No se pudo cambiar la contraseña.", e);
        }
    }

    @FXML
    private void cambiarRol() {
        try {
            verificarAdministrador();

            UsuarioAdministracionDAO.Usuario seleccionado =
                    tablaUsuarios.getSelectionModel().getSelectedItem();

            if (seleccionado == null) {
                mostrarAviso("Selecciona un usuario.");
                return;
            }

            if (seleccionado.usuarioId()
                    == usuarioAutenticado.getUsuarioId()) {
                mostrarAviso("No puedes cambiar tu propio rol.");
                return;
            }

            ComboBox<UsuarioAdministracionDAO.Rol> combo =
                    new ComboBox<>(
                            FXCollections.observableArrayList(
                                    dao.listarRolesActivos()));

            combo.setPromptText("Seleccionar rol");
            combo.setMaxWidth(Double.MAX_VALUE);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Cambiar rol");
            dialog.setHeaderText(
                    "Nuevo rol para " + seleccionado.username());
            dialog.getDialogPane().setContent(combo);
            dialog.getDialogPane().getButtonTypes().addAll(
                    ButtonType.OK, ButtonType.CANCEL);

            Optional<ButtonType> resultado = dialog.showAndWait();

            if (resultado.isEmpty()
                    || resultado.get() != ButtonType.OK) {
                return;
            }

            if (combo.getValue() == null) {
                mostrarAviso("Selecciona un rol.");
                return;
            }

            dao.cambiarRol(
                    seleccionado.usuarioId(),
                    combo.getValue().rolId());

            mostrarInformacion("Rol actualizado correctamente.");
            cargarUsuarios();

        } catch (SQLException | IllegalArgumentException
                 | SecurityException e) {
            mostrarError("No se pudo cambiar el rol.", e);
        }
    }

    @FXML
    private void cambiarEstado() {
        try {
            verificarAdministrador();

            UsuarioAdministracionDAO.Usuario seleccionado =
                    tablaUsuarios.getSelectionModel().getSelectedItem();

            if (seleccionado == null) {
                mostrarAviso("Selecciona un usuario.");
                return;
            }

            if (seleccionado.usuarioId()
                    == usuarioAutenticado.getUsuarioId()) {
                mostrarAviso(
                        "No puedes desactivar tu propia cuenta.");
                return;
            }

            boolean nuevoEstado = !seleccionado.activo();

            Alert confirmacion =
                    new Alert(Alert.AlertType.CONFIRMATION);

            confirmacion.setTitle("Confirmar operación");
            confirmacion.setHeaderText(
                    (nuevoEstado ? "Activar " : "Desactivar ")
                            + seleccionado.username() + "?");
            confirmacion.setContentText(
                    "¿Deseas continuar con esta operación?");

            Optional<ButtonType> resultado =
                    confirmacion.showAndWait();

            if (resultado.isEmpty()
                    || resultado.get() != ButtonType.OK) {
                return;
            }

            dao.cambiarEstado(
                    seleccionado.usuarioId(),
                    nuevoEstado);

            mostrarInformacion("Estado actualizado correctamente.");
            cargarUsuarios();

        } catch (SQLException | SecurityException e) {
            mostrarError("No se pudo cambiar el estado.", e);
        }
    }

    @FXML
    private void regresarMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/sistemagestion/view/menu_principal.fxml"));

            Parent root = loader.load();

            MenuPrincipalController controller =
                    loader.getController();

            controller.setUsuarioAutenticado(usuarioAutenticado);

            Stage stage =
                    (Stage) tablaUsuarios.getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("Sistema de Gestión - Menú principal");
            stage.centerOnScreen();

        } catch (IOException | RuntimeException e) {
            mostrarError(
                    "No se pudo regresar al menú principal.", e);
        }
    }

    private void mostrarInformacion(String mensaje) {
        new Alert(Alert.AlertType.INFORMATION, mensaje)
                .showAndWait();
    }

    private void mostrarAviso(String mensaje) {
        new Alert(Alert.AlertType.WARNING, mensaje)
                .showAndWait();
    }

    private void mostrarError(String mensaje, Exception e) {
        new Alert(
                Alert.AlertType.ERROR,
                mensaje + "\n" + e.getMessage())
                .showAndWait();
    }
}

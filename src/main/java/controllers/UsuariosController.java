package controllers;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import models.Usuario;
import util.AuditoriaUtil;
import util.DatabaseUtil;
import util.SecurityUtil;
import util.SessionManager;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

public class UsuariosController implements Initializable {

    @FXML private TableView<Usuario> tablaUsuarios;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, String> colEstado;
    @FXML private TableColumn<Usuario, String> colUltimoIngreso;
    @FXML private TextField txtNombre;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cmbRol;
    @FXML private CheckBox chkActivo;
    @FXML private Button btnNuevo;
    @FXML private Button btnGuardar;
    @FXML private Button btnEliminar;
    @FXML private Button btnResetPass;

    private Usuario usuarioSeleccionado;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!SessionManager.isAdmin()) {
            Platform.runLater(() -> {
                Stage stage = (Stage) tablaUsuarios.getScene().getWindow();
                stage.close();
            });
            return;
        }

        cmbRol.setItems(FXCollections.observableArrayList("ADMIN", "OPERADOR"));

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colEstado.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().isActivo() ? "Activo" : "Inactivo"));
        colUltimoIngreso.setCellValueFactory(data -> {
            LocalDateTime fecha = data.getValue().getUltimoIngreso();
            return new ReadOnlyStringWrapper(fecha != null ? fecha.toString().replace('T', ' ') : "");
        });

        tablaUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                usuarioSeleccionado = newSel;
                txtNombre.setText(newSel.getNombre());
                cmbRol.setValue(newSel.getRol());
                chkActivo.setSelected(newSel.isActivo());
            }
        });

        cargarUsuarios();
    }

    private void cargarUsuarios() {
        List<Usuario> usuarios = DatabaseUtil.obtenerUsuarios();
        ObservableList<Usuario> lista = FXCollections.observableArrayList(usuarios);
        tablaUsuarios.setItems(lista);
    }

    @FXML
    private void nuevoUsuario() {
        usuarioSeleccionado = null;
        txtNombre.clear();
        txtPassword.clear();
        cmbRol.getSelectionModel().clearSelection();
        chkActivo.setSelected(true);
        tablaUsuarios.getSelectionModel().clearSelection();
    }

    @FXML
    private void guardarUsuario() {
        String nombre = txtNombre.getText();
        String password = txtPassword.getText();
        String rol = cmbRol.getValue();
        boolean activo = chkActivo.isSelected();

        if (nombre == null || nombre.isBlank() || rol == null) {
            new Alert(Alert.AlertType.ERROR, "Nombre y rol son obligatorios").showAndWait();
            return;
        }

        try {
            if (usuarioSeleccionado == null) {
                Usuario u = new Usuario();
                u.setNombre(nombre);
                u.setPasswordHash(SecurityUtil.hashPassword(password != null ? password : ""));
                u.setRol(rol);
                u.setActivo(activo);
                int id = DatabaseUtil.insertarUsuario(u);
                AuditoriaUtil.registrar(SessionManager.getUsuarioActual().getNombre(),
                        "ALTA_USUARIO", "USUARIO", id, nombre);
            } else {
                usuarioSeleccionado.setNombre(nombre);
                if (password != null && !password.isBlank()) {
                    usuarioSeleccionado.setPasswordHash(SecurityUtil.hashPassword(password));
                }
                usuarioSeleccionado.setRol(rol);
                usuarioSeleccionado.setActivo(activo);
                DatabaseUtil.actualizarUsuario(usuarioSeleccionado);
                AuditoriaUtil.registrar(SessionManager.getUsuarioActual().getNombre(),
                        "MOD_USUARIO", "USUARIO", usuarioSeleccionado.getId(), nombre);
            }
            cargarUsuarios();
            nuevoUsuario();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error al guardar usuario").showAndWait();
        }
    }

    @FXML
    private void cambiarEstadoUsuario() {
        if (usuarioSeleccionado == null) {
            new Alert(Alert.AlertType.WARNING, "Seleccione un usuario").showAndWait();
            return;
        }
        boolean nuevoEstado = !usuarioSeleccionado.isActivo();
        try {
            DatabaseUtil.cambiarEstadoUsuario(usuarioSeleccionado.getId(), nuevoEstado);
            AuditoriaUtil.registrar(SessionManager.getUsuarioActual().getNombre(),
                    "BAJA_USUARIO", "USUARIO", usuarioSeleccionado.getId(),
                    nuevoEstado ? "Activado" : "Desactivado");
            cargarUsuarios();
            nuevoUsuario();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void resetearPassword() {
        if (usuarioSeleccionado == null) {
            new Alert(Alert.AlertType.WARNING, "Seleccione un usuario").showAndWait();
            return;
        }
        try {
            usuarioSeleccionado.setPasswordHash(SecurityUtil.hashPassword("123456"));
            DatabaseUtil.actualizarUsuario(usuarioSeleccionado);
            AuditoriaUtil.registrar(SessionManager.getUsuarioActual().getNombre(),
                    "RESET_PASS", "USUARIO", usuarioSeleccionado.getId(),
                    "Contraseña restablecida");
            cargarUsuarios();
            nuevoUsuario();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
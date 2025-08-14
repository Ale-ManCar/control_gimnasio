package controllers;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import models.Usuario;
import util.DatabaseUtil;
import util.SecurityUtil;
import util.SessionManager;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class UsuariosController implements Initializable {

    @FXML private TableView<Usuario> tablaUsuarios;
    @FXML private TableColumn<Usuario, Integer> colId;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, String> colActivo;
    @FXML private TextField txtNombre;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cbRol;
    @FXML private CheckBox chkActivo;

    private Usuario usuarioSeleccionado;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbRol.setItems(FXCollections.observableArrayList("ADMIN", "OPERADOR"));

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colActivo.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().isActivo() ? "Activo" : "Inactivo"));

        tablaUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                usuarioSeleccionado = newSel;
                txtNombre.setText(newSel.getNombre());
                cbRol.setValue(newSel.getRol());
                chkActivo.setSelected(newSel.isActivo());
            }
        });

        cargarUsuarios();
    }

    private boolean verificarAdmin() {
        if (!SessionManager.isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Acceso denegado");
            alert.showAndWait();
            return false;
        }
        return true;
    }

    private void cargarUsuarios() {
        if (!verificarAdmin()) return;
        ObservableList<Usuario> lista = FXCollections.observableArrayList();
        String sql = "SELECT id, nombre, rol, activo FROM usuarios";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Usuario u = new Usuario();
                u.setId(rs.getInt("id"));
                u.setNombre(rs.getString("nombre"));
                u.setRol(rs.getString("rol"));
                u.setActivo(rs.getBoolean("activo"));
                lista.add(u);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        tablaUsuarios.setItems(lista);
    }

    @FXML
    private void nuevoUsuario() {
        if (!verificarAdmin()) return;
        usuarioSeleccionado = null;
        txtNombre.clear();
        txtPassword.clear();
        cbRol.getSelectionModel().clearSelection();
        chkActivo.setSelected(true);
        tablaUsuarios.getSelectionModel().clearSelection();
    }

    @FXML
    private void guardarUsuario() {
        if (!verificarAdmin()) return;

        String nombre = txtNombre.getText();
        String password = txtPassword.getText();
        String rol = cbRol.getValue();
        boolean activo = chkActivo.isSelected();

        if (nombre == null || nombre.isBlank() || rol == null) {
            new Alert(Alert.AlertType.ERROR, "Nombre y rol son obligatorios").showAndWait();
            return;
        }

        try {
            if (usuarioSeleccionado == null) {
                String hash = SecurityUtil.hashPassword(password != null ? password : "");
                String sql = "INSERT INTO usuarios(nombre, password, rol, activo) VALUES(?,?,?,?)";
                DatabaseUtil.executeUpdate(sql, nombre, hash, rol, activo ? 1 : 0);
            } else {
                if (password != null && !password.isBlank()) {
                    String hash = SecurityUtil.hashPassword(password);
                    String sql = "UPDATE usuarios SET nombre=?, password=?, rol=?, activo=? WHERE id=?";
                    DatabaseUtil.executeUpdate(sql, nombre, hash, rol, activo ? 1 : 0, usuarioSeleccionado.getId());
                } else {
                    String sql = "UPDATE usuarios SET nombre=?, rol=?, activo=? WHERE id=?";
                    DatabaseUtil.executeUpdate(sql, nombre, rol, activo ? 1 : 0, usuarioSeleccionado.getId());
                }
            }
            cargarUsuarios();
            nuevoUsuario();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error al guardar usuario").showAndWait();
        }
    }
}
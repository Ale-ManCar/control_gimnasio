package controllers;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Role;
import models.User;
import util.SessionManager;
import util.UserService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class UsuariosController implements Initializable {

    @FXML private TableView<User> tablaUsuarios;
    @FXML private TableColumn<User, String> colUsuario;
    @FXML private TableColumn<User, String> colUltimoAcceso;
    @FXML private TableColumn<User, Integer> colAcciones;
    @FXML private Button btnNuevo;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Label lblMensaje;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (!SessionManager.tienePermiso(Role.ADMIN)) {
            if (lblMensaje != null) {
                lblMensaje.setText("Acceso restringido a administradores");
            }
            deshabilitarControles();
            return;
        }
        configurarTabla();
        cargarUsuarios();
    }

    private void deshabilitarControles() {
        if (tablaUsuarios != null) {
            tablaUsuarios.setDisable(true);
            tablaUsuarios.setPlaceholder(new Label("Acceso restringido"));
        }
        if (btnNuevo != null) btnNuevo.setDisable(true);
        if (btnEditar != null) btnEditar.setDisable(true);
        if (btnEliminar != null) btnEliminar.setDisable(true);
    }

    private void configurarTabla() {
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("username"));
        colAcciones.setCellValueFactory(new PropertyValueFactory<>("accionesRealizadas"));
        colUltimoAcceso.setCellValueFactory(cellData -> {
            if (cellData.getValue().getLastLogin() == null) {
                return new javafx.beans.property.SimpleStringProperty("-");
            }
            return new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLastLogin().format(formatter));
        });
    }

    @FXML
    private void handleNuevo() {
        abrirFormulario(null);
    }

    @FXML
    private void handleEditar() {
        User seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Seleccione un usuario para editar");
            return;
        }
        abrirFormulario(seleccionado);
    }

    @FXML
    private void handleEliminar() {
        User seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Seleccione un usuario para eliminar");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText(null);
        confirm.setContentText("¿Eliminar el usuario " + seleccionado.getUsername() + "?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                try {
                    UserService.eliminarUsuario(seleccionado.getId());
                    cargarUsuarios();
                } catch (SQLException e) {
                    mostrarAlerta("No se pudo eliminar el usuario: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleCerrar() {
        Stage stage = (Stage) tablaUsuarios.getScene().getWindow();
        stage.close();
    }

    private void abrirFormulario(User usuario) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/registro_recepcionista.fxml"));
            Parent root = loader.load();
            RegistroRecepcionistaController controller = loader.getController();
            Stage dialog = new Stage();
            dialog.initOwner(tablaUsuarios.getScene().getWindow());
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setTitle(usuario == null ? "Nuevo recepcionista" : "Editar recepcionista");
            controller.setStage(dialog);
            controller.setOnSave(() -> Platform.runLater(this::cargarUsuarios));
            if (usuario != null) {
                controller.editarUsuario(usuario);
            }
            dialog.showAndWait();
        } catch (IOException e) {
            mostrarAlerta("No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    private void cargarUsuarios() {
        try {
            ObservableList<User> usuarios = UserService.listarUsuariosPorRol(Role.RECEPCIONISTA);
            tablaUsuarios.setItems(usuarios);
            lblMensaje.setText(usuarios.isEmpty() ? "No hay recepcionistas registrados" : "");
        } catch (SQLException e) {
            lblMensaje.setText("No se pudieron cargar los usuarios");
        }
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
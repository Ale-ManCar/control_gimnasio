package controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Role;
import models.User;
import util.DatabaseUtil;
import util.SessionManager;

public class LoginController {
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private ChoiceBox<Role> cbRol;
    @FXML private Label lblMensaje;

    private Stage stage;
    private User usuarioSeleccionado;

    @FXML
    public void initialize() {
        cbRol.getItems().setAll(Role.values());
        cbRol.setValue(Role.RECEPCIONISTA);
        Platform.runLater(this::actualizarEstadoCampos);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        if (this.stage != null) {
            this.stage.setTitle("Iniciar sesión");
            this.stage.setResizable(false);
        }
        Platform.runLater(this::verificarUsuarios);
    }

    public void configurarParaUsuario(User user) {
        this.usuarioSeleccionado = user;
        if (user != null) {
            txtUsuario.setText(user.getUsername());
            txtUsuario.setDisable(true);
            cbRol.setValue(user.getRole());
            cbRol.setDisable(true);
            lblMensaje.setText("");
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String usuario = txtUsuario.getText();
        String password = txtPassword.getText();
        Role rol = usuarioSeleccionado != null ? usuarioSeleccionado.getRole() : cbRol.getValue();
        if (rol == null) {
            lblMensaje.setText("Seleccione un rol válido");
            return;
        }
        if (SessionManager.login(usuario, password, rol)) {
            Stage ventana = stage != null ? stage : (Stage) ((Node) event.getSource()).getScene().getWindow();
            abrirDashboard(rol, ventana);
        } else {
            lblMensaje.setText("Credenciales inválidas");
        }
    }

    @FXML
    private void handleCambiarPerfil() {
        abrirSelectorPerfiles();
    }

    private void abrirDashboard(Role rol, Stage currentStage) {
        try {
            String fxml = rol == Role.ADMIN ? "/fxml/admin_dashboard.fxml" : "/fxml/recepcionista_dashboard.fxml";
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage nuevaVentana = new Stage();
            nuevaVentana.setScene(new Scene(root, 900, 650));
            nuevaVentana.setTitle(rol == Role.ADMIN ? "Panel Admin" : "Panel Recepción");
            nuevaVentana.setResizable(false);
            nuevaVentana.show();
            currentStage.close();
        } catch (Exception e) {
            e.printStackTrace();
            lblMensaje.setText("Error al abrir panel");
        }
    }

    private void verificarUsuarios() {
        try {
            if (DatabaseUtil.getTotalUsuarios() == 0) {
                abrirCrearAdmin();
            }
        } catch (Exception e) {
            lblMensaje.setText("No se pudo verificar usuarios iniciales");
        }
    }

    private void abrirCrearAdmin() {
        if (stage == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/crear_admin.fxml"));
            Parent root = loader.load();
            CrearAdminController controller = loader.getController();
            controller.setStage(stage);
            stage.setScene(new Scene(root, 420, 320));
            stage.setTitle("Crear administrador");
        } catch (Exception e) {
            lblMensaje.setText("No se pudo abrir el formulario de administrador");
        }
    }

    private void abrirSelectorPerfiles() {
        if (stage == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/selector_perfiles.fxml"));
            Parent root = loader.load();
            SelectorPerfilesController controller = loader.getController();
            controller.setStage(stage);
            stage.setScene(new Scene(root, 700, 420));
            stage.setTitle("Seleccionar perfil");
        } catch (Exception e) {
            lblMensaje.setText("No se pudo volver al selector de perfiles");
        }
    }

    private void actualizarEstadoCampos() {
        boolean usuarioPreseleccionado = usuarioSeleccionado != null;
        txtUsuario.setDisable(usuarioPreseleccionado);
        cbRol.setDisable(usuarioPreseleccionado);
    }
}
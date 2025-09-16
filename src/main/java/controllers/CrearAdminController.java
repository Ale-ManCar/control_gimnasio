package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Role;
import models.User;
import util.UserService;

import java.io.IOException;
import java.sql.SQLException;

public class CrearAdminController {
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmar;
    @FXML private Label lblMensaje;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
        if (this.stage != null) {
            this.stage.setTitle("Crear administrador");
            this.stage.setResizable(false);
        }
    }

    @FXML
    private void handleGuardar() {
        String usuario = txtUsuario.getText() != null ? txtUsuario.getText().trim() : "";
        String password = txtPassword.getText();
        String confirmar = txtConfirmar.getText();

        if (usuario.isEmpty() || password == null || password.isEmpty() || confirmar == null || confirmar.isEmpty()) {
            mostrarMensaje("Debe completar todos los campos", true);
            return;
        }

        if (!password.equals(confirmar)) {
            mostrarMensaje("Las contraseñas no coinciden", true);
            return;
        }

        try {
            User admin = new User(0, usuario, password, Role.ADMIN);
            UserService.crearUsuario(admin);
            mostrarMensaje("Administrador creado correctamente", false);
            abrirSelectorPerfiles();
        } catch (SQLException e) {
            mostrarMensaje("No se pudo crear el administrador: " + e.getMessage(), true);
        }
    }

    private void abrirSelectorPerfiles() {
        if (stage == null) {
            mostrarAlerta("No se pudo determinar la ventana principal");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/selector_perfiles.fxml"));
            Parent root = loader.load();
            SelectorPerfilesController controller = loader.getController();
            controller.setStage(stage);
            stage.setScene(new Scene(root, 700, 420));
            stage.setTitle("Seleccionar perfil");
        } catch (IOException e) {
            mostrarAlerta("No se pudo abrir el selector de perfiles: " + e.getMessage());
        }
    }

    private void mostrarMensaje(String mensaje, boolean error) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle(error ? "-fx-text-fill: #ff6b6b;" : "-fx-text-fill: #2ecc71;");
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
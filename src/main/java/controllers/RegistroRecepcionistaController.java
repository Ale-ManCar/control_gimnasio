package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import models.Role;
import models.User;
import util.UserService;

import java.sql.SQLException;
import java.util.function.UnaryOperator;

public class RegistroRecepcionistaController {
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmar;
    @FXML private Label lblTitulo;
    @FXML private Label lblMensaje;

    private Stage stage;
    private User usuarioEditando;
    private Runnable onSave;

    @FXML
    private void initialize() {
        configurarTextFieldMayusculas(txtUsuario);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        if (this.stage != null) {
            this.stage.setResizable(false);
        }
    }

    public void setOnSave(Runnable onSave) {
        this.onSave = onSave;
    }

    public void editarUsuario(User user) {
        this.usuarioEditando = user;
        if (lblTitulo != null) {
            lblTitulo.setText("Editar recepcionista");
        }
        txtUsuario.setText(user.getUsername() != null ? user.getUsername().toUpperCase() : "");
        if (lblMensaje != null) {
            lblMensaje.setText("");
        }
        if (stage != null) {
            stage.setTitle("Editar recepcionista");
        }
    }

    @FXML
    private void handleGuardar() {
        String usuario = txtUsuario.getText() != null ? txtUsuario.getText().trim() : "";
        String password = txtPassword.getText();
        String confirmar = txtConfirmar.getText();

        if (usuario.isEmpty()) {
            mostrarMensaje("El usuario es obligatorio", true);
            return;
        }

        boolean cambiarPassword = usuarioEditando == null || (password != null && !password.isEmpty());
        if (cambiarPassword) {
            if (password == null || password.isEmpty() || confirmar == null || confirmar.isEmpty()) {
                mostrarMensaje("Debe ingresar y confirmar la contraseña", true);
                return;
            }
            if (!password.equals(confirmar)) {
                mostrarMensaje("Las contraseñas no coinciden", true);
                return;
            }
        }

        try {
            if (usuarioEditando == null) {
                User recepcionista = new User(0, usuario, password, Role.RECEPCIONISTA);
                UserService.crearUsuario(recepcionista);
                mostrarMensaje("Recepcionista creado", false);
            } else {
                String nuevaPassword = cambiarPassword ? password : usuarioEditando.getPassword();
                User actualizado = new User(usuarioEditando.getId(), usuario, nuevaPassword, Role.RECEPCIONISTA,
                        usuarioEditando.getLastLogin(), usuarioEditando.getAccionesRealizadas());
                UserService.editarUsuario(actualizado);
                mostrarMensaje("Recepcionista actualizado", false);
            }
            if (onSave != null) {
                onSave.run();
            }
            if (stage != null) {
                stage.close();
            }
        } catch (SQLException e) {
            mostrarMensaje("No se pudo guardar el usuario: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleCancelar() {
        if (stage != null) {
            stage.close();
        }
    }

    private void mostrarMensaje(String mensaje, boolean error) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle(error ? "-fx-text-fill: #ff6b6b;" : "-fx-text-fill: #2ecc71;");
        if (!error) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        }
    }

    private void configurarTextFieldMayusculas(TextField textField) {
        if (textField == null) {
            return;
        }
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String nuevoTexto = change.getText();
            if (nuevoTexto != null) {
                change.setText(nuevoTexto.toUpperCase());
            }
            return change;
        };
        TextFormatter<String> textFormatter = new TextFormatter<>(filter);
        textField.setTextFormatter(textFormatter);
    }
}

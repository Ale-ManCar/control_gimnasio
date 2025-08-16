package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import util.AuditoriaUtil;
import util.DatabaseUtil;
import util.SecurityUtil;

public class CrearAdminController {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirm;

    @FXML
    private void handleCrear(ActionEvent event) {
        String usuario = txtUsuario.getText().trim();
        String pass = txtPassword.getText();
        String confirm = txtConfirm.getText();

        if (usuario.isEmpty() || pass.isEmpty()) {
            new Alert(Alert.AlertType.ERROR, "Usuario y contraseña son obligatorios").showAndWait();
            return;
        }
        if (!pass.equals(confirm)) {
            new Alert(Alert.AlertType.ERROR, "Las contraseñas no coinciden").showAndWait();
            return;
        }

        try {
            String hash = SecurityUtil.hashPassword(pass);
            DatabaseUtil.insertUsuario(usuario, hash, "ADMIN", true);
            AuditoriaUtil.registrar("SYSTEM", "ALTA_USUARIO", "ADMIN_INICIAL", null, usuario);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Administrador creado. Inicie sesión.");
            alert.showAndWait();
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) txtUsuario.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error al crear administrador").showAndWait();
        }
    }
}
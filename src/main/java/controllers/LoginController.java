package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Usuario;
import util.DatabaseUtil;
import util.SecurityUtil;
import util.SessionManager;
import util.AuditoriaUtil;

public class LoginController {
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;

    @FXML
    private void handleLogin(ActionEvent event) {
        String nombre = txtUsuario.getText();
        String pass = txtPassword.getText();
        Usuario usuario = DatabaseUtil.obtenerUsuarioPorNombre(nombre);
        if (usuario == null || !usuario.isActivo() ||
                !usuario.getPasswordHash().equals(SecurityUtil.hashPassword(pass))) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("Credenciales inválidas o usuario inactivo");
            alert.showAndWait();
            return;
        }
        SessionManager.setUsuarioActual(usuario);
        DatabaseUtil.actualizarUltimoIngreso(usuario.getId());
        AuditoriaUtil.registrar(usuario.getNombre(), "LOGIN", "USUARIO", usuario.getId(), "Inicio de sesión");
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) btnLogin.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
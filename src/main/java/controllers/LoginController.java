package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;

import models.Role;
import models.User;
import util.DatabaseUtil;

public class LoginController {
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private ChoiceBox<Role> cbRol;
    @FXML private Label lblMensaje;

    @FXML
    public void initialize() {
        cbRol.getItems().setAll(Role.values());
        cbRol.setValue(Role.RECEPCIONISTA);
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String usuario = txtUsuario.getText();
        String password = txtPassword.getText();
        Role rol = cbRol.getValue();
        User user = DatabaseUtil.obtenerUsuario(usuario, password);
        if (user != null && user.getRole() == rol) {
            abrirDashboard(rol, (Stage)((Node)event.getSource()).getScene().getWindow());
        } else {
            lblMensaje.setText("Credenciales inválidas");
        }
    }

    private void abrirDashboard(Role rol, Stage currentStage) {
        try {
            String fxml = rol == Role.ADMIN ? "/fxml/admin_dashboard.fxml" : "/fxml/dashboard.fxml";
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 900, 650));
            stage.setTitle(rol == Role.ADMIN ? "Panel Admin" : "Panel Recepción");
            stage.setResizable(false);
            stage.show();
            currentStage.close();
        } catch (Exception e) {
            e.printStackTrace();
            lblMensaje.setText("Error al abrir panel");
        }
    }
}
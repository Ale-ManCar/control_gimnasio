package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ReactivacionController {

    @FXML private Label lblRequestCode;
    @FXML private TextField txtActivationCode;
    @FXML private Button btnSubmit;
    @FXML private Button btnCancel;

    private boolean success = false;

    public void setRequestCode(String code) {
        lblRequestCode.setText(code);
    }

    public boolean wasSuccess() {
        return success;
    }

    @FXML
    private void handleSubmit() {
        String code = txtActivationCode.getText().trim();
        if (code.isEmpty()) {
            showError("Ingrese un código de activación válido");
            return;
        }

        if (util.LicenseManager.applyReactivationCode(code)) {
            success = true;
            closeWindow();
        } else {
            showError("Código de activación inválido");
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de Activación");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeWindow() {
        Stage stage = (Stage) btnSubmit.getScene().getWindow();
        stage.close();
    }
}
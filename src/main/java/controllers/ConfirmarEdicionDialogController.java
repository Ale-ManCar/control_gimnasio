package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;

public class ConfirmarEdicionDialogController {

    @FXML public Label lblNombreCliente;
    @FXML public Button btnConfirmar;
    @FXML public Button btnCancelar;

    public void setNombreCliente(String nombreCompleto) {
        lblNombreCliente.setText(nombreCompleto);
    }

    @FXML
    public void initialize() {
        // Configurar efectos hover para los botones
        btnCancelar.addEventHandler(MouseEvent.MOUSE_ENTERED, e ->
                btnCancelar.setStyle("-fx-background-color: #f5f5f5; " +
                        "-fx-text-fill: #6C757D; " +
                        "-fx-font-weight: bold;" +
                        "-fx-border-color: #6C757D; " +
                        "-fx-border-radius: 20; " +
                        "-fx-padding: 8 20;")
        );

        btnCancelar.addEventHandler(MouseEvent.MOUSE_EXITED, e ->
                btnCancelar.setStyle("-fx-background-color: transparent; " +
                        "-fx-text-fill: #6C757D; " +
                        "-fx-font-weight: bold;" +
                        "-fx-border-color: #6C757D; " +
                        "-fx-border-radius: 20; " +
                        "-fx-padding: 8 20;")
        );

        btnConfirmar.addEventHandler(MouseEvent.MOUSE_ENTERED, e ->
                btnConfirmar.setStyle("-fx-background-color: #3A5BD9; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 20; " +
                        "-fx-padding: 8 20;")
        );

        btnConfirmar.addEventHandler(MouseEvent.MOUSE_EXITED, e ->
                btnConfirmar.setStyle("-fx-background-color: #4A6CF7; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 20; " +
                        "-fx-padding: 8 20;")
        );
    }
}
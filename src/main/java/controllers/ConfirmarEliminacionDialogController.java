package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;

public class ConfirmarEliminacionDialogController {

    @FXML public Label lblNombreCliente;
    @FXML public Button btnCancelar;
    @FXML public Button btnEliminar;

    public void setNombreCliente(String nombreCompleto) {
        lblNombreCliente.setText(nombreCompleto);
    }

    @FXML
    public void initialize() {
        // Efecto hover para botón Cancelar
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

        // Efecto hover para botón Eliminar
        btnEliminar.addEventHandler(MouseEvent.MOUSE_ENTERED, e ->
                btnEliminar.setStyle("-fx-background-color: #c82333; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 20; " +
                        "-fx-padding: 8 20;")
        );

        btnEliminar.addEventHandler(MouseEvent.MOUSE_EXITED, e ->
                btnEliminar.setStyle("-fx-background-color: #DC3545; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 20; " +
                        "-fx-padding: 8 20;")
        );
    }
}
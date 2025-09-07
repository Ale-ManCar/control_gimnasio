package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import util.AlertScheduler;

/**
 * Permite configurar las tareas programadas del sistema.
 */
public class ConfigSchedulerController {

    @FXML
    private TextField txtCronBackup;

    @FXML
    private TextField txtCronAvisos;

    @FXML
    private TextField txtCronOrdenes;

    @FXML
    private Label lblMensaje;

    @FXML
    private void guardar() {
        try {
            AlertScheduler.programarBackup(txtCronBackup.getText());
            AlertScheduler.programarAvisosVencimiento(txtCronAvisos.getText());
            AlertScheduler.programarOrdenesCompra(txtCronOrdenes.getText());
            lblMensaje.setText("Tareas programadas");
        } catch (Exception e) {
            lblMensaje.setText("Error al programar: " + e.getMessage());
        }
    }
}
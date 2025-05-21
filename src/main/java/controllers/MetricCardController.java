package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MetricCardController {
    @FXML private Label lblTitulo;
    @FXML private Label lblValor;

    public void setDatos(String titulo, int valor) {
        lblTitulo.setText(titulo);
        lblValor.setText(String.valueOf(valor));
    }
}
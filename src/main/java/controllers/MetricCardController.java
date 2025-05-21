package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MetricCardController {

    @FXML private Label lblTitulo;

    @FXML private Label lblValor;

    public void setTitulo(String titulo) {
        lblTitulo.setText(titulo);
    }

    public void setValor(String valor) {
        lblValor.setText(valor);
    }
}
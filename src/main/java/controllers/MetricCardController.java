package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.paint.Color;

public class MetricCardController {

    @FXML private Label lblTitulo;
    @FXML private Label lblValor;
    @FXML private FontIcon icono;

    public void setTitulo(String titulo) {
        lblTitulo.setText(titulo);
    }

    public void setValor(String valor) {
        lblValor.setText(valor);
    }

    public void setIcon(String iconLiteral, String color) {
        if (iconLiteral != null) {
            icono.setIconLiteral(iconLiteral);
        }
        if (color != null) {
            icono.setIconColor(Color.web(color));
        }
    }

    public void setIcon(String iconLiteral) {
        setIcon(iconLiteral, null);
    }
}
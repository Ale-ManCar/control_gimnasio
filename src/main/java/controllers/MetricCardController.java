package controllers;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import org.kordamp.ikonli.javafx.FontIcon;

public class MetricCardController {

    @FXML private AnchorPane root;
    @FXML private FontIcon iconoMetric;
    @FXML private Label lblTitulo;
    @FXML private Label lblValor;

    public void setTitulo(String titulo) {
        lblTitulo.setText(titulo);
    }

    public void setValor(String valor) {
        lblValor.setText(valor);
    }

    public void setIconLiteral(String iconLiteral) {
        if (iconLiteral != null && !iconLiteral.isBlank()) {
            iconoMetric.setIconLiteral(iconLiteral);
        }
    }

    public void setAccent(String cssClass) {
        root.getStyleClass().removeIf(style -> style.startsWith("metric-card--"));
        if (cssClass != null && !cssClass.isBlank()) {
            root.getStyleClass().add(cssClass);
        }
    }

    public void setOnClick(EventHandler<MouseEvent> handler) {
        root.setOnMouseClicked(handler);
        root.setFocusTraversable(handler != null);
    }
}
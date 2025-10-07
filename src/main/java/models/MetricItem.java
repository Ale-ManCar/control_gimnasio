package models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MetricItem {
    private final StringProperty descripcion;
    private final IntegerProperty valor;

    public MetricItem(String descripcion, int valor) {
        this.descripcion = new SimpleStringProperty(descripcion);
        this.valor = new SimpleIntegerProperty(valor);
    }

    public String getDescripcion() { return descripcion.get(); }
    public int getValor() { return valor.get(); }

    public StringProperty descripcionProperty() { return descripcion; }
    public IntegerProperty valorProperty() { return valor; }
}
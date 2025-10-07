package models;

public class IngresoData {
    private final String etiqueta;
    private final double total;

    public IngresoData(String etiqueta, double total) {
        this.etiqueta = etiqueta;
        this.total = total;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public double getTotal() {
        return total;
    }
}
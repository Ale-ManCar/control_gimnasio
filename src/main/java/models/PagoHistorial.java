package models;

import java.time.LocalDate;

public class PagoHistorial {
    private LocalDate fechaPago;
    private String tipoMembresia;
    private double monto;

    public PagoHistorial(LocalDate fechaPago, String tipoMembresia, double monto) {
        this.fechaPago = fechaPago;
        this.tipoMembresia = tipoMembresia;
        this.monto = monto;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public String getTipoMembresia() {
        return tipoMembresia;
    }

    public double getMonto() {
        return monto;
    }
}

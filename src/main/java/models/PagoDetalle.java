package models;

import java.time.LocalDate;

public class PagoDetalle {
    private LocalDate fecha;
    private String cliente;
    private String membresia;
    private double monto;

    public PagoDetalle(LocalDate fecha, String cliente, String membresia, double monto) {
        this.fecha = fecha;
        this.cliente = cliente;
        this.membresia = membresia;
        this.monto = monto;
    }

    // Getters
    public LocalDate getFecha() { return fecha; }
    public String getCliente() { return cliente; }
    public String getMembresia() { return membresia; }
    public double getMonto() { return monto; }
}
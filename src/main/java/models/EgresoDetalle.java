package models;

import java.time.LocalDate;

public class EgresoDetalle {
    private LocalDate fecha;
    private String descripcion;
    private String categoria;
    private double monto;

    public EgresoDetalle(LocalDate fecha, String descripcion, String categoria, double monto) {
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.monto = monto;
    }

    // Getters
    public LocalDate getFecha() { return fecha; }
    public String getDescripcion() { return descripcion; }
    public String getCategoria() { return categoria; }
    public double getMonto() { return monto; }
}
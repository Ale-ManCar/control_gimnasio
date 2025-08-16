package models;

import java.time.LocalDate;

public class Egreso {
    private int id;
    private String descripcion;
    private double monto;
    private LocalDate fecha;
    private String categoria;
    private String numeroFactura;
    private int proveedorId;
    private String rutaAdjunto;

    public Egreso() {}

    public Egreso(String descripcion, double monto, LocalDate fecha, String categoria) {
        this.descripcion = descripcion;
        this.monto = monto;
        this.fecha = fecha;
        this.categoria = categoria;
    }

    // Getters y setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }

    public int getProveedorId() { return proveedorId; }
    public void setProveedorId(int proveedorId) { this.proveedorId = proveedorId; }

    public String getRutaAdjunto() { return rutaAdjunto; }
    public void setRutaAdjunto(String rutaAdjunto) { this.rutaAdjunto = rutaAdjunto; }
}
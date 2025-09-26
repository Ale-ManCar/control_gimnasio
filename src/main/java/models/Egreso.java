package models;

import java.time.LocalDate;

public class Egreso {
    private int id;
    private String descripcion;
    private double monto;
    private LocalDate fecha;
    private String categoria;
    private String proveedor;
    private String pdfPath;

    public Egreso() {}

    public Egreso(String descripcion, double monto, LocalDate fecha, String categoria) {
        this.descripcion = descripcion;
        this.monto = monto;
        this.fecha = fecha;
        this.categoria = categoria;
    }

    public Egreso(String descripcion, double monto, LocalDate fecha, String categoria, String proveedor, String pdfPath) {
        this(descripcion, monto, fecha, categoria);
        this.proveedor = proveedor;
        this.pdfPath = pdfPath;
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
    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }
    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }
}
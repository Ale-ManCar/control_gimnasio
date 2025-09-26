package models;

import java.time.LocalDate;

public class EgresoDetalle {
    private final int id;
    private final LocalDate fecha;
    private final String descripcion;
    private final String categoria;
    private final double monto;
    private final String proveedor;
    private final String pdfPath;

    public EgresoDetalle(int id, LocalDate fecha, String descripcion, String categoria,
                         double monto, String proveedor, String pdfPath) {
        this.id = id;
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.monto = monto;
        this.proveedor = proveedor;
        this.pdfPath = pdfPath;
    }

    public int getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public String getDescripcion() { return descripcion; }
    public String getCategoria() { return categoria; }
    public double getMonto() { return monto; }
    public String getProveedor() { return proveedor; }
    public String getPdfPath() { return pdfPath; }
}
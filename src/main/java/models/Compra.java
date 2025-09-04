package models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Compra {
    private int id;
    private int proveedorId;
    private LocalDate fecha;
    private double total;
    private String rutaPdf;
    private List<CompraDetalle> detalles = new ArrayList<>();

    public Compra() {
    }

    public Compra(int proveedorId, LocalDate fecha, double total, String rutaPdf) {
        this.proveedorId = proveedorId;
        this.fecha = fecha;
        this.total = total;
        this.rutaPdf = rutaPdf;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(int proveedorId) {
        this.proveedorId = proveedorId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getRutaPdf() {
        return rutaPdf;
    }

    public void setRutaPdf(String rutaPdf) {
        this.rutaPdf = rutaPdf;
    }

    public List<CompraDetalle> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<CompraDetalle> detalles) {
        this.detalles = detalles;
    }
}
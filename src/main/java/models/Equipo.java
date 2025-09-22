package models;

import java.time.LocalDate;
import java.util.Objects;

public class Equipo {
    private int id;
    private String nombre = "";
    private String tipo = "";
    private String estado = "";
    private int cantidad;
    private LocalDate fechaCompra;
    private LocalDate fechaUltimoMantenimiento;
    private Integer frecuenciaMantenimiento;
    private String ubicacion;
    private String descripcion;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre != null ? nombre.trim() : "";
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo != null ? tipo.trim() : "";
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado != null ? estado.trim() : "";
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa");
        }
        this.cantidad = cantidad;
    }

    public LocalDate getFechaCompra() {
        return fechaCompra;
    }

    public void setFechaCompra(LocalDate fechaCompra) {
        this.fechaCompra = fechaCompra;
    }

    public LocalDate getFechaUltimoMantenimiento() {
        return fechaUltimoMantenimiento;
    }

    public void setFechaUltimoMantenimiento(LocalDate fechaUltimoMantenimiento) {
        this.fechaUltimoMantenimiento = fechaUltimoMantenimiento;
    }

    public Integer getFrecuenciaMantenimiento() {
        return frecuenciaMantenimiento;
    }

    public void setFrecuenciaMantenimiento(Integer frecuenciaMantenimiento) {
        if (frecuenciaMantenimiento != null && frecuenciaMantenimiento < 0) {
            throw new IllegalArgumentException("La frecuencia de mantenimiento debe ser positiva");
        }
        this.frecuenciaMantenimiento = frecuenciaMantenimiento;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion != null && !ubicacion.isBlank() ? ubicacion.trim() : null;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion != null && !descripcion.isBlank() ? descripcion.trim() : null;
    }

    public LocalDate getProximoMantenimiento() {
        if (fechaUltimoMantenimiento == null || frecuenciaMantenimiento == null || frecuenciaMantenimiento <= 0) {
            return null;
        }
        return fechaUltimoMantenimiento.plusDays(frecuenciaMantenimiento);
    }

    public boolean needsMaintenance(LocalDate now) {
        Objects.requireNonNull(now, "La fecha de referencia no puede ser nula");
        LocalDate proximo = getProximoMantenimiento();
        return proximo != null && !proximo.isAfter(now);
    }

    public boolean maintenanceDueSoon(LocalDate now, int dias) {
        Objects.requireNonNull(now, "La fecha de referencia no puede ser nula");
        if (dias < 0) {
            throw new IllegalArgumentException("El rango de días debe ser positivo");
        }
        LocalDate proximo = getProximoMantenimiento();
        if (proximo == null) {
            return false;
        }
        return !proximo.isBefore(now) && !proximo.isAfter(now.plusDays(dias));
    }

    public boolean isEstadoCritico() {
        if (estado == null) {
            return false;
        }
        String normalized = estado.trim().toUpperCase();
        return normalized.contains("CRITICO") || normalized.contains("FUERA") || normalized.contains("MAL");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Equipo equipo)) return false;
        return id == equipo.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return nombre + " (" + tipo + ")";
    }
}
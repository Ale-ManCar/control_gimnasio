package models;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.OptionalInt;

public class Equipo {
    private int id;
    private String nombre = "";
    private String tipo = "";
    private String estado = "";
    private int cantidad;
    private String marca = "";
    private String modelo = "";
    private String peso;
    private String fechaAdquisicion;
    private LocalDate fechaUltimoMantenimiento;
    private String frecuenciaMantenimiento;
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

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca != null ? marca.trim() : "";
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo != null ? modelo.trim() : "";
    }

    public String getPeso() {
        return peso;
    }

    public void setPeso(String peso) {
        if (peso == null || peso.isBlank()) {
            this.peso = null;
            return;
        }
        String trimmed = peso.trim();
        try {
            int valor = Integer.parseInt(trimmed);
            if (valor < 0) {
                throw new IllegalArgumentException("El peso debe ser mayor o igual a cero");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El peso debe ser un número entero válido");
        }
        this.peso = trimmed;
    }

    public void setPeso(Integer peso) {
        if (peso == null) {
            this.peso = null;
            return;
        }
        if (peso < 0) {
            throw new IllegalArgumentException("El peso debe ser mayor o igual a cero");
        }
        this.peso = String.valueOf(peso);
    }

    public Integer getPesoAsInteger() {
        if (peso == null || peso.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(peso.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getFechaAdquisicion() {
        return fechaAdquisicion;
    }

    public void setFechaAdquisicion(String fechaAdquisicion) {
        if (fechaAdquisicion == null || fechaAdquisicion.isBlank()) {
            this.fechaAdquisicion = null;
            return;
        }
        String trimmed = fechaAdquisicion.trim();
        try {
            LocalDate.parse(trimmed);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha de adquisición debe tener el formato yyyy-MM-dd");
        }
        this.fechaAdquisicion = trimmed;
    }

    public void setFechaAdquisicion(LocalDate fechaAdquisicion) {
        this.fechaAdquisicion = fechaAdquisicion != null ? fechaAdquisicion.toString() : null;
    }

    public LocalDate getFechaAdquisicionDate() {
        if (fechaAdquisicion == null || fechaAdquisicion.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(fechaAdquisicion);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public LocalDate getFechaUltimoMantenimiento() {
        return fechaUltimoMantenimiento;
    }

    public void setFechaUltimoMantenimiento(LocalDate fechaUltimoMantenimiento) {
        this.fechaUltimoMantenimiento = fechaUltimoMantenimiento;
    }

    public String getFrecuenciaMantenimiento() {
        return frecuenciaMantenimiento;
    }

    public void setFrecuenciaMantenimiento(String frecuenciaMantenimiento) {
        if (frecuenciaMantenimiento == null || frecuenciaMantenimiento.isBlank()) {
            this.frecuenciaMantenimiento = null;
            return;
        }
        String trimmed = frecuenciaMantenimiento.trim();
        try {
            int valor = Integer.parseInt(trimmed);
            if (valor < 0) {
                throw new IllegalArgumentException("La frecuencia de mantenimiento debe ser positiva");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La frecuencia de mantenimiento debe ser un número entero");
        }
        this.frecuenciaMantenimiento = trimmed;
    }

    public void setFrecuenciaMantenimiento(Integer frecuenciaMantenimiento) {
        if (frecuenciaMantenimiento == null) {
            this.frecuenciaMantenimiento = null;
            return;
        }
        if (frecuenciaMantenimiento < 0) {
            throw new IllegalArgumentException("La frecuencia de mantenimiento debe ser positiva");
        }
        this.frecuenciaMantenimiento = String.valueOf(frecuenciaMantenimiento);
    }

    public OptionalInt getFrecuenciaMantenimientoDias() {
        if (frecuenciaMantenimiento == null || frecuenciaMantenimiento.isBlank()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(frecuenciaMantenimiento.trim()));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
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
        OptionalInt frecuenciaDias = getFrecuenciaMantenimientoDias();
        if (fechaUltimoMantenimiento == null || frecuenciaDias.isEmpty() || frecuenciaDias.getAsInt() <= 0) {
            return null;
        }
        return fechaUltimoMantenimiento.plusDays(frecuenciaDias.getAsInt());
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
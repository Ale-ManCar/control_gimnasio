package models;

import java.time.LocalDate;

public class Pago {
    private int id;
    private int clienteId;
    private LocalDate fechaPago;
    private LocalDate fechaVencimiento;
    private String tipoMembresia;
    private double monto;
    private String estado;
    private String clienteNombre;

    public Pago() {
    }

    public Pago(int id, int clienteId, LocalDate fechaPago, LocalDate fechaVencimiento,
                String tipoMembresia, double monto, String estado) {
        this.id = id;
        this.clienteId = clienteId;
        this.fechaPago = fechaPago;
        this.fechaVencimiento = fechaVencimiento;
        this.tipoMembresia = tipoMembresia;
        this.monto = monto;
        this.estado = estado;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getClienteId() {
        return clienteId;
    }

    public void setClienteId(int clienteId) {
        this.clienteId = clienteId;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public String getTipoMembresia() {
        return tipoMembresia;
    }

    public void setTipoMembresia(String tipoMembresia) {
        this.tipoMembresia = tipoMembresia;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }
}
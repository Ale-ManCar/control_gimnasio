package models;

import java.time.LocalDate;

public class PagoDetalle {
    private LocalDate fecha;
    private String cliente;
    private int clienteId;
    private String membresia;
    private double monto;

    public PagoDetalle(LocalDate fecha, String cliente, int clienteId, String membresia, double monto) {
        this.fecha = fecha;
        this.cliente = cliente;
        this.clienteId = clienteId;
        this.membresia = membresia;
        this.monto = monto;
    }

    // Getters
    public LocalDate getFecha() { return fecha; }
    public String getCliente() { return cliente; }
    public int getClienteId() { return clienteId; }
    public String getMembresia() { return membresia; }
    public double getMonto() { return monto; }
}
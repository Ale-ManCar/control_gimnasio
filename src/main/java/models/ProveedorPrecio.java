package models;

/**
 * Representa el precio promedio que ofrece un proveedor para un equipo.
 */
public class ProveedorPrecio {
    private final String proveedor;
    private final double precio;

    public ProveedorPrecio(String proveedor, double precio) {
        this.proveedor = proveedor;
        this.precio = precio;
    }

    public String getProveedor() {
        return proveedor;
    }

    public double getPrecio() {
        return precio;
    }
}
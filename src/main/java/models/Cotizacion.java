package models;

public class Cotizacion {
    private int id;
    private int proveedorId;
    private int productoId;
    private String presentacion;
    private double precio;
    private String vigencia;
    private String condiciones;

    public Cotizacion() {}

    public Cotizacion(int proveedorId, int productoId, String presentacion,
                      double precio, String vigencia, String condiciones) {
        this.proveedorId = proveedorId;
        this.productoId = productoId;
        this.presentacion = presentacion;
        this.precio = precio;
        this.vigencia = vigencia;
        this.condiciones = condiciones;
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

    public int getProductoId() {
        return productoId;
    }

    public void setProductoId(int productoId) {
        this.productoId = productoId;
    }

    public String getPresentacion() {
        return presentacion;
    }

    public void setPresentacion(String presentacion) {
        this.presentacion = presentacion;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public String getVigencia() {
        return vigencia;
    }

    public void setVigencia(String vigencia) {
        this.vigencia = vigencia;
    }

    public String getCondiciones() {
        return condiciones;
    }

    public void setCondiciones(String condiciones) {
        this.condiciones = condiciones;
    }
}
package models;

public class Equipo {
    private int id;
    private String nombre;
    private int stock;
    private double precio;
    private Integer proveedorId;

    public Equipo() {
    }

    public Equipo(String nombre, int stock, double precio, Integer proveedorId) {
        this.nombre = nombre;
        this.stock = stock;
        this.precio = precio;
        this.proveedorId = proveedorId;
    }

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
        this.nombre = nombre;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public Integer getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(Integer proveedorId) {
        this.proveedorId = proveedorId;
    }
}
package models;

public class Equipo {
    private int id;
    private String nombre;
    private String marca;
    private double peso;
    private int stock;
    private double precio;
    private Integer proveedorId;

    public Equipo() {
    }

    public Equipo(String nombre, int stock, double precio, Integer proveedorId) {
        this(nombre, null, 0, stock, precio, proveedorId);
    }

    public Equipo(String nombre, String marca, double peso, int stock, double precio, Integer proveedorId) {
        this.nombre = nombre;
        this.marca = marca;
        this.peso = peso;
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

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public double getPeso() {
        return peso;
    }

    public void setPeso(double peso) {
        this.peso = peso;
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
package models;

public class Equipo {
    private int id;
    private String nombre;
    private String marca;
    private double peso;
    private int stock;
    private double costoCompra;
    private double precioVenta;
    private Integer proveedorId;
    private int umbral;

    public Equipo() {
    }

    public Equipo(String nombre, int stock, double costoCompra, double precioVenta, Integer proveedorId) {
        this(nombre, null, 0, stock, costoCompra, precioVenta, proveedorId);
    }

    public Equipo(String nombre, String marca, double peso, int stock, double costoCompra, double precioVenta, Integer proveedorId) {
        this.nombre = nombre;
        this.marca = marca;
        this.peso = peso;
        this.stock = stock;
        this.costoCompra = costoCompra;
        this.precioVenta = precioVenta;
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

    public double getCostoCompra() {
        return costoCompra;
    }

    public void setCostoCompra(double costoCompra) {
        this.costoCompra = costoCompra;
    }

    public double getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(double precioVenta) {
        this.precioVenta = precioVenta;
    }

    public Integer getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(Integer proveedorId) {
        this.proveedorId = proveedorId;
    }

    public int getUmbral() {
        return umbral;
    }

    public void setUmbral(int umbral) {
        this.umbral = umbral;
    }

    // Métodos de compatibilidad con código existente
    public double getPrecio() {
        return getPrecioVenta();
    }

    public void setPrecio(double precio) {
        setPrecioVenta(precio);
    }
}
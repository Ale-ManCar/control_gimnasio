package models;

public class Producto {
    private int id;
    private String nombre;
    private int stock;
    private double precioVenta;
    private String tipo; // PACA, KG, LB
    private double precioCompra; // Precio de compra de la paca completa
    private int unidadesPorPaca; // Solo para tipo PACA

    public Producto() {}

    public Producto(String nombre, int stock, double precioVenta, String tipo, double precioCompra, int unidadesPorPaca) {
        this.nombre = nombre;
        this.stock = stock;
        this.precioVenta = precioVenta;
        this.tipo = tipo;
        this.precioCompra = precioCompra;
        this.unidadesPorPaca = unidadesPorPaca;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public double getPrecio() { return precioVenta; }
    public void setPrecio(double precioVenta) { this.precioVenta = precioVenta; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public double getPrecioCompra() { return precioCompra; }
    public void setPrecioCompra(double precioCompra) { this.precioCompra = precioCompra; }
    public int getUnidadesPorPaca() { return unidadesPorPaca; }
    public void setUnidadesPorPaca(int unidadesPorPaca) { this.unidadesPorPaca = unidadesPorPaca; }

    // Método para calcular ganancia por unidad
    public double getGananciaPorUnidad() {
        if (tipo.equals("PACA")) {
            double costoPorUnidad = precioCompra / unidadesPorPaca;
            return precioVenta - costoPorUnidad;
        } else {
            return precioVenta - precioCompra;
        }
    }
}
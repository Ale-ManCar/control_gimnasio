package models;

public class VentaItem {

    private Producto producto;
    private int unidades;

    public VentaItem(Producto producto, int unidades) {
        this.producto = producto;
        this.unidades = unidades;
    }

    public Producto getProducto() {
        return producto;
    }

    public int getUnidades() {
        return unidades;
    }

    public void setUnidades(int unidades) {
        this.unidades = unidades;
    }

    public String getNombreProducto() {
        return producto.getNombre();
    }

    public double getPrecioUnitario() {
        return producto.getPrecio();
    }

    public double getTotal() {
        return producto.getPrecio() * unidades;
    }
}
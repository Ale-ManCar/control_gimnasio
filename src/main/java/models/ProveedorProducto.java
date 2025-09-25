package models;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ProveedorProducto {
    private int id;
    private int proveedorId;
    private Integer equipoId;
    private Integer productoId;
    private String tipo = "";
    private Proveedor proveedor;

    private final StringProperty nombreProducto = new SimpleStringProperty("");
    private final DoubleProperty precio = new SimpleDoubleProperty(0.0);
    private final BooleanProperty seleccionado = new SimpleBooleanProperty(false);

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

    public Integer getEquipoId() {
        return equipoId;
    }

    public void setEquipoId(Integer equipoId) {
        this.equipoId = equipoId;
    }

    public Integer getProductoId() {
        return productoId;
    }

    public void setProductoId(Integer productoId) {
        this.productoId = productoId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo != null ? tipo.toUpperCase() : "";
    }

    public String getNombreProducto() {
        return nombreProducto.get();
    }

    public void setNombreProducto(String nombre) {
        this.nombreProducto.set(nombre != null ? nombre : "");
    }

    public StringProperty nombreProductoProperty() {
        return nombreProducto;
    }

    public double getPrecio() {
        return precio.get();
    }

    public void setPrecio(double valor) {
        if (Double.isNaN(valor) || Double.isInfinite(valor) || valor < 0) {
            valor = 0;
        }
        this.precio.set(valor);
    }

    public DoubleProperty precioProperty() {
        return precio;
    }

    public boolean isSeleccionado() {
        return seleccionado.get();
    }

    public void setSeleccionado(boolean value) {
        this.seleccionado.set(value);
    }

    public BooleanProperty seleccionadoProperty() {
        return seleccionado;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    @Override
    public String toString() {
        return String.format("ProveedorProducto{id=%d, proveedorId=%d, tipo='%s', nombre='%s', precio=%.2f}",
                id, proveedorId, tipo, getNombreProducto(), getPrecio());
    }
}
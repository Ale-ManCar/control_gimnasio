package models;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

public class Proveedor {
    private int id;
    private String nombre;
    private String contacto;
    private String telefono;
    private final ObservableList<ProveedorProducto> productos = FXCollections.observableArrayList();

    public Proveedor() {
    }

    public Proveedor(String nombre, String contacto, String telefono) {
        this.nombre = nombre;
        this.contacto = contacto;
        this.telefono = telefono;
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
        this.nombre = nombre != null ? nombre.trim() : null;
    }

    public String getContacto() {
        return contacto;
    }

    public void setContacto(String contacto) {
        this.contacto = contacto != null ? contacto.trim() : null;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono != null ? telefono.trim() : null;
    }

    public ObservableList<ProveedorProducto> getProductos() {
        return productos;
    }

    public void setProductos(Collection<ProveedorProducto> nuevosProductos) {
        productos.setAll(nuevosProductos);
    }

    public void agregarProducto(ProveedorProducto producto) {
        if (producto != null) {
            productos.add(producto);
        }
    }

    public boolean suministraEquipos() {
        return productos.stream().anyMatch(p -> "EQUIPO".equalsIgnoreCase(p.getTipo()));
    }

    public boolean suministraInsumos() {
        return productos.stream().anyMatch(p -> "INSUMO".equalsIgnoreCase(p.getTipo()));
    }

    public List<ProveedorProducto> getEquiposSuministrados() {
        return productos.stream()
                .filter(p -> "EQUIPO".equalsIgnoreCase(p.getTipo()))
                .collect(Collectors.toList());
    }

    public List<ProveedorProducto> getInsumosSuministrados() {
        return productos.stream()
                .filter(p -> "INSUMO".equalsIgnoreCase(p.getTipo()))
                .collect(Collectors.toList());
    }

    public OptionalDouble obtenerPrecioPara(String tipo, int itemId) {
        return productos.stream()
                .filter(p -> tipo.equalsIgnoreCase(p.getTipo()))
                .filter(p -> ("EQUIPO".equalsIgnoreCase(tipo) && p.getEquipoId() != null && p.getEquipoId() == itemId)
                        || ("INSUMO".equalsIgnoreCase(tipo) && p.getProductoId() != null && p.getProductoId() == itemId))
                .mapToDouble(ProveedorProducto::getPrecio)
                .findFirst();
    }

    public String getResumenCategorias() {
        boolean equipos = suministraEquipos();
        boolean insumos = suministraInsumos();
        if (equipos && insumos) {
            return "Equipos e Insumos";
        }
        if (equipos) {
            return "Equipos";
        }
        if (insumos) {
            return "Insumos";
        }
        return "Sin productos";
    }

    public String getDetallePorTipo(String tipo) {
        List<String> nombres = productos.stream()
                .filter(p -> tipo.equalsIgnoreCase(p.getTipo()))
                .map(p -> {
                    String nombreProducto = p.getNombreProducto();
                    if (nombreProducto == null || nombreProducto.isBlank()) {
                        return "Producto sin nombre";
                    }
                    return nombreProducto.substring(0, 1).toUpperCase(Locale.ROOT) + nombreProducto.substring(1);
                })
                .toList();
        return nombres.isEmpty() ? "Sin productos registrados" : String.join(", ", nombres);
    }
}
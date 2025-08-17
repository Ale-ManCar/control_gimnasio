package controllers;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableCell;
import models.Cotizacion;
import models.Producto;
import models.Proveedor;
import util.DatabaseUtil;
import util.AuditoriaUtil;
import util.SessionManager;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Comparator;

public class ComparadorPreciosController implements Initializable {

    @FXML private ComboBox<Producto> cbProducto;
    @FXML private ComboBox<String> cbFiltro;
    @FXML private TableView<Cotizacion> tablaCotizaciones;
    @FXML private TableColumn<Cotizacion, String> colProveedor;
    @FXML private TableColumn<Cotizacion, String> colPresentacion;
    @FXML private TableColumn<Cotizacion, Double> colPrecio;
    @FXML private TableColumn<Cotizacion, String> colVigencia;
    @FXML private TableColumn<Cotizacion, String> colPrecioUnitario;

    private Map<Integer, String> mapaProveedores = new HashMap<>();
    private Cotizacion mejorCotizacion;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            cbProducto.setItems(DatabaseUtil.getProductos());
            ObservableList<Proveedor> proveedores = DatabaseUtil.getProveedores(false);
            proveedores.forEach(p -> mapaProveedores.put(p.getId(), p.getNombre()));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        cbFiltro.setItems(FXCollections.observableArrayList("Precio", "Fecha de entrega"));
        cbFiltro.getSelectionModel().selectFirst();

        cbProducto.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                cargarCotizaciones(n.getId());
            }
        });
        cbFiltro.valueProperty().addListener((obs, o, n) -> resaltarMejorOferta());

        colProveedor.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(mapaProveedores.getOrDefault(data.getValue().getProveedorId(), "")));
        colPresentacion.setCellValueFactory(new PropertyValueFactory<>("presentacion"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colPrecio.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("$%.2f", price));
            }
        });
        colVigencia.setCellValueFactory(new PropertyValueFactory<>("vigencia"));
        colPrecioUnitario.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(String.format("$%.2f", calcularPrecioUnitario(data.getValue()))));

        tablaCotizaciones.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Cotizacion item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && item.equals(mejorCotizacion)) {
                    setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void cargarCotizaciones(int productoId) {
        try {
            ObservableList<Cotizacion> cotizaciones = DatabaseUtil.getCotizacionesPorProducto(productoId);
            tablaCotizaciones.setItems(cotizaciones);
            resaltarMejorOferta();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private double calcularPrecioUnitario(Cotizacion c) {
        int cantidad = extraerCantidad(c.getPresentacion());
        return cantidad > 0 ? c.getPrecio() / cantidad : c.getPrecio();
    }

    private int extraerCantidad(String presentacion) {
        if (presentacion == null) return 0;
        String digits = presentacion.replaceAll("\\D+", "");
        if (digits.isEmpty()) return 0;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private LocalDate parseVigencia(Cotizacion c) {
        try {
            return LocalDate.parse(c.getVigencia());
        } catch (DateTimeParseException e) {
            return LocalDate.MAX;
        }
    }

    private void resaltarMejorOferta() {
        ObservableList<Cotizacion> lista = tablaCotizaciones.getItems();
        if (lista == null || lista.isEmpty()) {
            mejorCotizacion = null;
            tablaCotizaciones.refresh();
            return;
        }
        if ("Fecha de entrega".equals(cbFiltro.getValue())) {
            mejorCotizacion = lista.stream().min(Comparator.comparing(this::parseVigencia)).orElse(null);
        } else {
            mejorCotizacion = lista.stream().min(Comparator.comparingDouble(this::calcularPrecioUnitario)).orElse(null);
        }
        tablaCotizaciones.refresh();
    }

    @FXML
    private void confirmarCompra() {
        Cotizacion seleccionada = tablaCotizaciones.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            new Alert(Alert.AlertType.WARNING, "Seleccione una cotización").showAndWait();
            return;
        }
        String proveedor = mapaProveedores.get(seleccionada.getProveedorId());
        AuditoriaUtil.registrar(
                SessionManager.getUsuarioActual().getNombre(),
                "EDICION_COTIZACION",
                "COTIZACION",
                seleccionada.getId(),
                "Proveedor: " + proveedor
        );
        new Alert(Alert.AlertType.INFORMATION, "Compra confirmada con " + proveedor).showAndWait();
    }
}
package controllers;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import models.CatalogoItem;
import models.Proveedor;
import models.ProveedorProducto;
import util.DatabaseUtil;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ComparacionPreciosController {

    @FXML private ComboBox<String> cmbCategoria;
    @FXML private ComboBox<CatalogoItem> cmbProducto;
    @FXML private ComboBox<String> cmbOrden;

    @FXML private TableView<ProveedorProducto> tablaComparacion;
    @FXML private TableColumn<ProveedorProducto, String> colProveedor;
    @FXML private TableColumn<ProveedorProducto, String> colContacto;
    @FXML private TableColumn<ProveedorProducto, String> colTelefono;
    @FXML private TableColumn<ProveedorProducto, Double> colPrecio;

    @FXML private BarChart<String, Number> graficaPrecios;
    @FXML private CategoryAxis ejeProveedores;
    @FXML private NumberAxis ejePrecios;

    @FXML private Button btnAgregarPedido;
    @FXML private Button btnVerDetalles;
    @FXML private Label lblProductoSeleccionado;

    private final ObservableList<ProveedorProducto> comparaciones = FXCollections.observableArrayList();
    private SortedList<ProveedorProducto> comparacionesOrdenadas;
    private double mejorPrecio = Double.NaN;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarControles();
    }

    private void configurarTabla() {
        colProveedor.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                Optional.ofNullable(cell.getValue().getProveedor()).map(Proveedor::getNombre).orElse("Sin nombre")));
        colContacto.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                Optional.ofNullable(cell.getValue().getProveedor()).map(Proveedor::getContacto).orElse("-")));
        colTelefono.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                Optional.ofNullable(cell.getValue().getProveedor()).map(Proveedor::getTelefono).orElse("-")));
        colPrecio.setCellValueFactory(cell -> cell.getValue().precioProperty().asObject());
        colPrecio.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format(Locale.US, "$ %.2f", item));
                }
            }
        });

        comparacionesOrdenadas = new SortedList<>(comparaciones);
        comparacionesOrdenadas.comparatorProperty().bind(tablaComparacion.comparatorProperty());
        tablaComparacion.setItems(comparacionesOrdenadas);

        tablaComparacion.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ProveedorProducto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (!Double.isNaN(mejorPrecio) && Double.compare(item.getPrecio(), mejorPrecio) == 0) {
                    setStyle("-fx-background-color: rgba(76,175,80,0.2);");
                } else {
                    setStyle("");
                }
            }
        });

        btnAgregarPedido.disableProperty().bind(Bindings.isNull(tablaComparacion.getSelectionModel().selectedItemProperty()));
        btnVerDetalles.disableProperty().bind(btnAgregarPedido.disableProperty());
    }

    private void configurarControles() {
        cmbCategoria.setItems(FXCollections.observableArrayList("Equipos", "Insumos"));
        cmbCategoria.valueProperty().addListener((obs, old, nuevo) -> cargarProductos(nuevo));

        cmbProducto.valueProperty().addListener((obs, old, nuevo) -> {
            if (nuevo != null) {
                lblProductoSeleccionado.setText("Producto: " + nuevo.getNombre());
                cargarComparacion();
            } else {
                lblProductoSeleccionado.setText("Selecciona un producto para comparar");
                comparaciones.clear();
                actualizarGrafica();
            }
        });

        cmbOrden.setItems(FXCollections.observableArrayList("Precio ascendente", "Precio descendente"));
        cmbOrden.valueProperty().addListener((obs, old, nuevo) -> aplicarOrden(nuevo));
        cmbOrden.getSelectionModel().selectFirst();

        cmbCategoria.getSelectionModel().selectFirst();
    }

    private void cargarProductos(String categoria) {
        comparaciones.clear();
        actualizarGrafica();
        if (categoria == null) {
            cmbProducto.setItems(FXCollections.emptyObservableList());
            return;
        }
        try {
            if (categoria.equalsIgnoreCase("Equipos")) {
                List<CatalogoItem> items = DatabaseUtil.listarEquipos().stream()
                        .map(eq -> new CatalogoItem(eq.getId(), eq.getNombre(), "EQUIPO"))
                        .sorted(Comparator.comparing(CatalogoItem::getNombre))
                        .toList();
                cmbProducto.setItems(FXCollections.observableArrayList(items));
            } else {
                List<CatalogoItem> items = DatabaseUtil.getProductos().stream()
                        .map(prod -> new CatalogoItem(prod.getId(), prod.getNombre(), "INSUMO"))
                        .sorted(Comparator.comparing(CatalogoItem::getNombre))
                        .toList();
                cmbProducto.setItems(FXCollections.observableArrayList(items));
            }
        } catch (SQLException e) {
            mostrarError("No se pudieron cargar los productos disponibles.");
            cmbProducto.setItems(FXCollections.emptyObservableList());
        }
    }

    private void cargarComparacion() {
        CatalogoItem itemSeleccionado = cmbProducto.getSelectionModel().getSelectedItem();
        if (itemSeleccionado == null) {
            comparaciones.clear();
            actualizarGrafica();
            return;
        }
        String tipo = itemSeleccionado.getCategoria();
        try {
            comparaciones.setAll(DatabaseUtil.obtenerComparativaProducto(tipo, itemSeleccionado.getId()));
            calcularMejorPrecio();
            aplicarOrden(cmbOrden.getValue());
            actualizarGrafica();
        } catch (SQLException e) {
            mostrarError("No se pudo cargar la información de comparación.");
        }
    }

    private void calcularMejorPrecio() {
        mejorPrecio = comparaciones.stream()
                .mapToDouble(ProveedorProducto::getPrecio)
                .min()
                .orElse(Double.NaN);
        tablaComparacion.refresh();
    }

    private void aplicarOrden(String orden) {
        if (orden == null) {
            return;
        }
        if (orden.contains("asc")) {
            colPrecio.setSortType(TableColumn.SortType.ASCENDING);
        } else {
            colPrecio.setSortType(TableColumn.SortType.DESCENDING);
        }
        tablaComparacion.getSortOrder().setAll(colPrecio);
    }

    private void actualizarGrafica() {
        graficaPrecios.getData().clear();
        if (comparaciones.isEmpty()) {
            return;
        }
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (ProveedorProducto producto : comparacionesOrdenadas) {
            String nombreProveedor = Optional.ofNullable(producto.getProveedor())
                    .map(Proveedor::getNombre)
                    .orElse("Proveedor");
            serie.getData().add(new XYChart.Data<>(nombreProveedor, producto.getPrecio()));
        }
        graficaPrecios.getData().add(serie);
    }

    @FXML
    private void verDetallesProveedor() {
        ProveedorProducto seleccionado = tablaComparacion.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            return;
        }
        Proveedor proveedor = seleccionado.getProveedor();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detalle del proveedor");
        alert.setHeaderText(Optional.ofNullable(proveedor.getNombre()).orElse("Proveedor"));
        StringBuilder contenido = new StringBuilder();
        contenido.append("Contacto: ").append(Optional.ofNullable(proveedor.getContacto()).orElse("-"));
        contenido.append("\nTeléfono: ").append(Optional.ofNullable(proveedor.getTelefono()).orElse("-"));
        contenido.append("\nPrecio ofrecido: ").append(String.format(Locale.US, "$ %.2f", seleccionado.getPrecio()));
        alert.setContentText(contenido.toString());
        alert.showAndWait();
    }

    @FXML
    private void agregarAPedido() {
        ProveedorProducto seleccionado = tablaComparacion.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Agregar a pedido");
        alert.setHeaderText("Proveedor seleccionado");
        String mensaje = "Has seleccionado a " +
                Optional.ofNullable(seleccionado.getProveedor()).map(Proveedor::getNombre).orElse("un proveedor") +
                " para el pedido. Continúa el proceso en el módulo de compras.";
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
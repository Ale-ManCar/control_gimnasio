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
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import models.CatalogoItem;
import models.Equipo;
import models.Proveedor;
import models.ProveedorProducto;
import util.DatabaseUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ComparacionPreciosController {

    @FXML private ComboBox<String> cmbCategoria;
    @FXML private ComboBox<CatalogoItem> cmbProducto;
    @FXML private ComboBox<Equipo> cmbVariante;
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
    @FXML private Label lblVariante;

    private final ObservableList<ProveedorProducto> comparaciones = FXCollections.observableArrayList();
    private SortedList<ProveedorProducto> comparacionesOrdenadas;
    private double mejorPrecio = Double.NaN;
    private final Map<Integer, List<Equipo>> variantesPorProducto = new HashMap<>();
    private boolean actualizandoVariante = false;

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

        configurarSelectorVariantes();

        cmbProducto.valueProperty().addListener((obs, old, nuevo) -> manejarCambioProducto(nuevo));

        cmbOrden.setItems(FXCollections.observableArrayList("Precio ascendente", "Precio descendente"));
        cmbOrden.valueProperty().addListener((obs, old, nuevo) -> aplicarOrden(nuevo));
        cmbOrden.getSelectionModel().selectFirst();

        mostrarSelectorVariante(false);
        cmbCategoria.getSelectionModel().selectFirst();
    }

    private void cargarProductos(String categoria) {
        comparaciones.clear();
        actualizarGrafica();
        cmbProducto.getSelectionModel().clearSelection();
        lblProductoSeleccionado.setText("Selecciona un producto para comparar");
        limpiarVariantes();
        if (categoria == null) {
            cmbProducto.setItems(FXCollections.emptyObservableList());
            return;
        }
        try {
            if (categoria.equalsIgnoreCase("Equipos")) {
                variantesPorProducto.clear();
                AtomicInteger generadorId = new AtomicInteger(-1);
                List<Equipo> equipos = DatabaseUtil.listarEquipos();
                Map<String, List<Equipo>> agrupados = equipos.stream()
                        .collect(Collectors.groupingBy(eq -> normalizarNombre(eq.getNombre()),
                                Collectors.toCollection(ArrayList::new)));

                List<CatalogoItem> items = new ArrayList<>();
                for (List<Equipo> variantes : agrupados.values()) {
                    List<Equipo> ordenadas = variantes.stream()
                            .sorted(crearComparadorVariantes())
                            .collect(Collectors.toCollection(ArrayList::new));
                    if (ordenadas.isEmpty()) {
                        continue;
                    }
                    int idGrupo = generadorId.getAndDecrement();
                    variantesPorProducto.put(idGrupo, ordenadas);
                    String nombre = ordenadas.get(0).getNombre();
                    items.add(new CatalogoItem(idGrupo, nombre, "EQUIPO"));
                }

                items.sort(Comparator.comparing(CatalogoItem::getNombre));
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
        comparaciones.clear();
        if (itemSeleccionado == null) {
            actualizarGrafica();
            return;
        }
        if (esProductoEquipo(itemSeleccionado)) {
            Equipo variante = cmbVariante != null ? cmbVariante.getSelectionModel().getSelectedItem() : null;
            if (variante == null) {
                actualizarGrafica();
                return;
            }
            cargarComparacionEquipo(variante);
        } else {
            cargarComparacionInsumo(itemSeleccionado.getId());
        }
    }

    private void configurarSelectorVariantes() {
        if (cmbVariante == null) {
            return;
        }
        cmbVariante.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Equipo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatearDescripcionVariante(item));
                }
            }
        });
        cmbVariante.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Equipo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatearDescripcionVariante(item));
                }
            }
        });

        cmbVariante.valueProperty().addListener((obs, old, nuevo) -> {
            if (actualizandoVariante || nuevo == null) {
                return;
            }
            CatalogoItem producto = cmbProducto.getSelectionModel().getSelectedItem();
            if (!esProductoEquipo(producto)) {
                return;
            }
            actualizarEtiquetaProducto(producto, nuevo);
            cargarComparacion();
        });
    }

    private void manejarCambioProducto(CatalogoItem nuevo) {
        comparaciones.clear();
        actualizarGrafica();
        if (nuevo == null) {
            lblProductoSeleccionado.setText("Selecciona un producto para comparar");
            limpiarVariantes();
            return;
        }
        if (esProductoEquipo(nuevo)) {
            prepararVariantesPara(nuevo);
        } else {
            limpiarVariantes();
            lblProductoSeleccionado.setText("Producto: " + nuevo.getNombre());
            cargarComparacion();
        }
    }

    private void prepararVariantesPara(CatalogoItem producto) {
        List<Equipo> variantes = variantesPorProducto.getOrDefault(producto.getId(), List.of());
        actualizandoVariante = true;
        cmbVariante.setItems(FXCollections.observableArrayList(variantes));
        if (!variantes.isEmpty()) {
            cmbVariante.getSelectionModel().selectFirst();
        } else {
            cmbVariante.getSelectionModel().clearSelection();
        }
        actualizandoVariante = false;

        boolean hayPeso = variantes.stream().anyMatch(this::tienePeso);
        lblVariante.setText(hayPeso ? "Peso" : "Variante");
        mostrarSelectorVariante(variantes.size() > 1);

        Equipo seleccionado = cmbVariante.getSelectionModel().getSelectedItem();
        actualizarEtiquetaProducto(producto, seleccionado);
        cargarComparacion();
    }

    private void limpiarVariantes() {
        if (cmbVariante == null) {
            return;
        }
        actualizandoVariante = true;
        cmbVariante.getItems().clear();
        cmbVariante.getSelectionModel().clearSelection();
        actualizandoVariante = false;
        mostrarSelectorVariante(false);
    }

    private void mostrarSelectorVariante(boolean visible) {
        if (cmbVariante != null) {
            cmbVariante.setVisible(visible);
            cmbVariante.setManaged(visible);
        }
        if (lblVariante != null) {
            lblVariante.setVisible(visible);
            lblVariante.setManaged(visible);
        }
    }

    private void actualizarEtiquetaProducto(CatalogoItem producto, Equipo variante) {
        if (producto == null) {
            lblProductoSeleccionado.setText("Selecciona un producto para comparar");
            return;
        }
        StringBuilder texto = new StringBuilder("Producto: ").append(producto.getNombre());
        String detalle = descripcionVarianteParaEtiqueta(variante);
        if (!detalle.isBlank()) {
            texto.append(' ').append(detalle);
        }
        lblProductoSeleccionado.setText(texto.toString());
    }

    private String descripcionVarianteParaEtiqueta(Equipo variante) {
        if (variante == null) {
            return "";
        }
        List<String> detalles = obtenerDetallesVariante(variante);
        if (detalles.isEmpty()) {
            return "";
        }
        return "(" + String.join(" · ", detalles) + ")";
    }

    private String formatearDescripcionVariante(Equipo variante) {
        if (variante == null) {
            return "";
        }
        List<String> detalles = obtenerDetallesVariante(variante);
        if (detalles.isEmpty()) {
            return "Variante " + variante.getId();
        }
        return String.join(" · ", detalles);
    }

    private List<String> obtenerDetallesVariante(Equipo variante) {
        List<String> detalles = new ArrayList<>();
        if (tienePeso(variante)) {
            detalles.add(variante.getPeso().trim() + " kg");
        }
        if (tieneTexto(variante.getMarca())) {
            detalles.add(variante.getMarca().trim());
        }
        if (tieneTexto(variante.getModelo())) {
            detalles.add(variante.getModelo().trim());
        }
        return detalles;
    }

    private boolean tienePeso(Equipo equipo) {
        return equipo != null && tieneTexto(equipo.getPeso());
    }

    private boolean tieneTexto(String texto) {
        return texto != null && !texto.isBlank();
    }

    private boolean esProductoEquipo(CatalogoItem item) {
        return item != null && "EQUIPO".equalsIgnoreCase(item.getCategoria());
    }

    private void cargarComparacionEquipo(Equipo variante) {
        try {
            comparaciones.setAll(DatabaseUtil.obtenerComparativaProducto("EQUIPO", variante.getId()));
            calcularMejorPrecio();
            aplicarOrden(cmbOrden.getValue());
            actualizarGrafica();
        } catch (SQLException e) {
            mostrarError("No se pudo cargar la información de comparación.");
        }
    }

    private void cargarComparacionInsumo(int productoId) {
        try {
            comparaciones.setAll(DatabaseUtil.obtenerComparativaProducto("INSUMO", productoId));
            calcularMejorPrecio();
            aplicarOrden(cmbOrden.getValue());
            actualizarGrafica();
        } catch (SQLException e) {
            mostrarError("No se pudo cargar la información de comparación.");
        }
    }

    private Comparator<Equipo> crearComparadorVariantes() {
        return Comparator
                .comparing((Equipo eq) -> Optional.ofNullable(eq.getPesoAsInteger()).orElse(Integer.MAX_VALUE))
                .thenComparing(eq -> valorOrdenable(eq.getMarca()))
                .thenComparing(eq -> valorOrdenable(eq.getModelo()))
                .thenComparingInt(Equipo::getId);
    }

    private String valorOrdenable(String texto) {
        return texto != null ? texto.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String normalizarNombre(String nombre) {
        return nombre != null ? nombre.trim().toUpperCase(Locale.ROOT) : "";
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
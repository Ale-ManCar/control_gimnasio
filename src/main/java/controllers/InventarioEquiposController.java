package controllers;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import models.Equipo;
import models.EquipoHistorial;
import models.Proveedor;
import util.DatabaseUtil;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class InventarioEquiposController {

    private static final Logger LOGGER = Logger.getLogger(InventarioEquiposController.class.getName());
    private static final DateTimeFormatter HISTORIAL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat CURRENCY_FORMAT;
    private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d*");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("\\d*(?:[\\.,]\\d{0,2})?");
    private static final int LOW_STOCK_THRESHOLD = 3;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setDecimalSeparator('.');
        symbols.setGroupingSeparator(',');
        CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
        CURRENCY_FORMAT.setDecimalFormatSymbols(symbols);
    }

    @FXML private TableView<Equipo> tablaEquipos;
    @FXML private TableColumn<Equipo, String> colNombre;
    @FXML private TableColumn<Equipo, String> colMarca;
    @FXML private TableColumn<Equipo, Double> colPeso;
    @FXML private TableColumn<Equipo, Integer> colStock;
    @FXML private TableColumn<Equipo, Double> colCosto;
    @FXML private TableColumn<Equipo, Double> colPrecioVenta;
    @FXML private TableColumn<Equipo, String> colProveedor;

    @FXML private TextField txtBusqueda;
    @FXML private ComboBox<Proveedor> cbFiltroProveedor;
    @FXML private ComboBox<String> cbFiltroMarca;
    @FXML private Label lblTotalUnidades;
    @FXML private Label lblTotalValor;
    @FXML private Label lblStatus;

    @FXML private TableView<EquipoHistorial> tablaHistorial;
    @FXML private TableColumn<EquipoHistorial, String> colHistorialFecha;
    @FXML private TableColumn<EquipoHistorial, String> colHistorialDescripcion;
    @FXML private TableColumn<EquipoHistorial, String> colHistorialProveedor;
    @FXML private TableColumn<EquipoHistorial, Integer> colHistorialCantidad;
    @FXML private TableColumn<EquipoHistorial, Double> colHistorialCosto;
    @FXML private TableColumn<EquipoHistorial, Double> colHistorialPrecio;
    @FXML private TableColumn<EquipoHistorial, Double> colHistorialTotal;
    @FXML private TableColumn<EquipoHistorial, String> colHistorialMoneda;
    @FXML private TableColumn<EquipoHistorial, String> colHistorialArchivo;
    @FXML private Button btnAbrirComprobante;

    private final ObservableList<Equipo> equiposBase = FXCollections.observableArrayList();
    private FilteredList<Equipo> equiposFiltrados;
    private final ObservableList<EquipoHistorial> historialData = FXCollections.observableArrayList();

    private final Map<Integer, Proveedor> proveedoresMap = new ConcurrentHashMap<>();
    private ObservableList<Proveedor> proveedoresCache;
    private final Map<String, ObservableList<String>> marcasCache = new ConcurrentHashMap<>();
    private final Map<String, ObservableList<Double>> pesosCache = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("inventario-equipos-worker-" + t.getId());
        return t;
    });

    private Integer ultimoEquipoSeleccionadoId;

    @FXML
    public void initialize() {
        configurarTablaEquipos();
        configurarTablaHistorial();
        configurarFiltros();
        cargarProveedores(false);
        cargarEquipos(true);
    }
    private void configurarTablaEquipos() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colMarca.setCellValueFactory(new PropertyValueFactory<>("marca"));
        colPeso.setCellValueFactory(new PropertyValueFactory<>("peso"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colCosto.setCellValueFactory(new PropertyValueFactory<>("costoCompra"));
        colPrecioVenta.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colProveedor.setCellValueFactory(data -> {
            Integer id = data.getValue().getProveedorId();
            Proveedor proveedor = id != null ? proveedoresMap.get(id) : null;
            return new ReadOnlyStringWrapper(proveedor != null ? proveedor.getNombre() : "");
        });

        colPeso.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format(Locale.getDefault(), "%.2f", item));
            }
        });
        colCosto.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : CURRENCY_FORMAT.format(item));
            }
        });
        colPrecioVenta.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : CURRENCY_FORMAT.format(item));
            }
        });

        tablaEquipos.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Equipo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    boolean bajo = (item.getUmbral() > 0 && item.getStock() <= item.getUmbral())
                            || item.getStock() <= LOW_STOCK_THRESHOLD;
                    setStyle(bajo ? "-fx-background-color: rgba(231,76,60,0.2);" : "");
                }
            }
        });

        tablaEquipos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                ultimoEquipoSeleccionadoId = newVal.getId();
                cargarHistorial(newVal);
            } else {
                historialData.clear();
            }
        });
    }

    private void configurarTablaHistorial() {
        tablaHistorial.setItems(historialData);
        colHistorialFecha.setCellValueFactory(data -> {
            if (data.getValue().getFecha() == null) {
                return new ReadOnlyStringWrapper("");
            }
            return new ReadOnlyStringWrapper(HISTORIAL_FORMATTER.format(data.getValue().getFecha()));
        });
        colHistorialDescripcion.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                Optional.ofNullable(data.getValue().getDescripcion()).orElse("")));
        colHistorialProveedor.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                Optional.ofNullable(data.getValue().getProveedorNombre()).orElse("")));
        colHistorialCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colHistorialCosto.setCellValueFactory(new PropertyValueFactory<>("costoCompra"));
        colHistorialPrecio.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colHistorialTotal.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(
                data.getValue().getCantidad() * data.getValue().getCostoCompra()));
        colHistorialMoneda.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                Optional.ofNullable(data.getValue().getMoneda()).orElse("")));
        colHistorialArchivo.setCellValueFactory(data -> {
            String ruta = data.getValue().getRutaPdf();
            if (ruta == null || ruta.isBlank()) {
                return new ReadOnlyStringWrapper("");
            }
            Path path = Paths.get(ruta);
            return new ReadOnlyStringWrapper(path.getFileName().toString());
        });

        colHistorialCosto.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : CURRENCY_FORMAT.format(item));
            }
        });
        colHistorialPrecio.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : CURRENCY_FORMAT.format(item));
            }
        });
        colHistorialTotal.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : CURRENCY_FORMAT.format(item));
            }
        });

        btnAbrirComprobante.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            EquipoHistorial seleccionado = tablaHistorial.getSelectionModel().getSelectedItem();
            return seleccionado == null || seleccionado.getRutaPdf() == null || seleccionado.getRutaPdf().isBlank();
        }, tablaHistorial.getSelectionModel().selectedItemProperty()));
    }

    private void configurarFiltros() {
        equiposFiltrados = new FilteredList<>(equiposBase, equipo -> true);
        SortedList<Equipo> sortedList = new SortedList<>(equiposFiltrados);
        sortedList.comparatorProperty().bind(tablaEquipos.comparatorProperty());
        tablaEquipos.setItems(sortedList);

        equiposFiltrados.addListener((ListChangeListener<Equipo>) change -> actualizarTotales());

        txtBusqueda.textProperty().addListener((obs, old, val) -> actualizarFiltro());
        cbFiltroProveedor.valueProperty().addListener((obs, old, val) -> actualizarFiltro());
        cbFiltroMarca.valueProperty().addListener((obs, old, val) -> actualizarFiltro());

        cbFiltroProveedor.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Proveedor item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.getNombre());
            }
        });
        cbFiltroProveedor.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Proveedor item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Todos" : item.getNombre());
            }
        });
        cbFiltroProveedor.setConverter(new StringConverter<>() {
            @Override
            public String toString(Proveedor object) {
                return object == null ? "Todos" : object.getNombre();
            }

            @Override
            public Proveedor fromString(String string) {
                return null;
            }
        });
    }

    private void actualizarFiltro() {
        String texto = Optional.ofNullable(txtBusqueda.getText()).orElse("").trim().toLowerCase(Locale.ROOT);
        Proveedor proveedorFiltro = cbFiltroProveedor.getValue();
        String marcaFiltro = cbFiltroMarca.getValue();
        equiposFiltrados.setPredicate(equipo -> {
            if (equipo == null) {
                return false;
            }
            boolean coincideBusqueda = texto.isBlank() ||
                    equipo.getNombre().toLowerCase(Locale.ROOT).contains(texto) ||
                    Optional.ofNullable(equipo.getMarca()).map(m -> m.toLowerCase(Locale.ROOT).contains(texto)).orElse(false);
            boolean coincideProveedor = proveedorFiltro == null || Objects.equals(equipo.getProveedorId(), proveedorFiltro.getId());
            boolean coincideMarca = marcaFiltro == null || marcaFiltro.isBlank() ||
                    (equipo.getMarca() != null && equipo.getMarca().equalsIgnoreCase(marcaFiltro));
            return coincideBusqueda && coincideProveedor && coincideMarca;
        });
        actualizarTotales();
    }

    private void actualizarTotales() {
        int totalUnidades = 0;
        double totalValor = 0;
        for (Equipo equipo : equiposFiltrados) {
            totalUnidades += equipo.getStock();
            totalValor += equipo.getStock() * equipo.getPrecioVenta();
        }
        String unidadesTexto = String.valueOf(totalUnidades);
        String valorTexto = CURRENCY_FORMAT.format(totalValor);
        if (Platform.isFxApplicationThread()) {
            lblTotalUnidades.setText(unidadesTexto);
            lblTotalValor.setText(valorTexto);
        } else {
            Platform.runLater(() -> {
                lblTotalUnidades.setText(unidadesTexto);
                lblTotalValor.setText(valorTexto);
            });
        }
    }
    private void cargarEquipos(boolean forceRefresh) {
        if (!forceRefresh && !equiposBase.isEmpty()) {
            actualizarFiltro();
            return;
        }
        ejecutarConsultaAsync(DatabaseUtil::getEquipos, equipos -> {
            equiposBase.setAll(equipos);
            actualizarFiltro();
            actualizarCatalogosFiltros();
            if (ultimoEquipoSeleccionadoId != null) {
                equiposBase.stream()
                        .filter(e -> e.getId() == ultimoEquipoSeleccionadoId)
                        .findFirst()
                        .ifPresent(e -> tablaEquipos.getSelectionModel().select(e));
            }
            mostrarEstado("Equipos cargados correctamente", false);
        }, "No se pudieron cargar los equipos");
    }

    private void actualizarCatalogosFiltros() {
        Set<String> marcas = new HashSet<>();
        for (Equipo equipo : equiposBase) {
            if (equipo.getMarca() != null && !equipo.getMarca().isBlank()) {
                marcas.add(equipo.getMarca());
            }
            if (equipo.getProveedorId() != null && !proveedoresMap.containsKey(equipo.getProveedorId()) && proveedoresCache != null) {
                proveedoresCache.stream()
                        .filter(p -> Objects.equals(p.getId(), equipo.getProveedorId()))
                        .findFirst()
                        .ifPresent(p -> proveedoresMap.put(p.getId(), p));
            }
        }
        List<String> marcasOrdenadas = new ArrayList<>(marcas);
        marcasOrdenadas.sort(String::compareToIgnoreCase);
        cbFiltroMarca.getItems().setAll(marcasOrdenadas);
    }

    private void cargarProveedores(boolean forceRefresh) {
        if (!forceRefresh && proveedoresCache != null) {
            Platform.runLater(() -> actualizarComboProveedores(proveedoresCache));
            return;
        }
        ejecutarConsultaAsync(DatabaseUtil::getProveedores, proveedores -> {
            proveedoresCache = FXCollections.observableArrayList(proveedores);
            proveedoresMap.clear();
            for (Proveedor proveedor : proveedoresCache) {
                proveedoresMap.put(proveedor.getId(), proveedor);
            }
            actualizarComboProveedores(proveedoresCache);
        }, "No se pudieron cargar los proveedores");
    }

    private void actualizarComboProveedores(ObservableList<Proveedor> proveedores) {
        ObservableList<Proveedor> items = FXCollections.observableArrayList();
        items.add(null);
        items.addAll(proveedores);
        cbFiltroProveedor.setItems(items);
    }

    private void cargarHistorial(Equipo equipo) {
        if (equipo == null) {
            historialData.clear();
            return;
        }
        ejecutarConsultaAsync(() -> DatabaseUtil.obtenerHistorialEquipo(equipo.getId()), historial ->
                historialData.setAll(historial), "No se pudo cargar el historial del equipo");
    }
    @FXML
    private void handleRegistrarEquipo() {
        Dialog<Equipo> dialog = new Dialog<>();
        dialog.setTitle("Registrar Equipo");
        ButtonType guardarBtn = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarBtn, ButtonType.CANCEL);

        TextField txtNombre = new TextField();
        ComboBox<String> cbMarca = new ComboBox<>();
        cbMarca.setEditable(true);
        ComboBox<String> cbPeso = new ComboBox<>();
        cbPeso.setEditable(true);
        TextField txtStock = new TextField();
        txtStock.setTextFormatter(createPositiveIntegerFormatter());
        TextField txtCosto = new TextField();
        txtCosto.setTextFormatter(createPositiveDecimalFormatter());
        TextField txtPrecio = new TextField();
        txtPrecio.setTextFormatter(createPositiveDecimalFormatter());
        TextField txtUmbral = new TextField();
        txtUmbral.setPromptText("Opcional");
        txtUmbral.setTextFormatter(createPositiveIntegerFormatter());
        ComboBox<Proveedor> cbProveedor = new ComboBox<>();
        cbProveedor.setConverter(new StringConverter<>() {
            @Override
            public String toString(Proveedor object) {
                return object == null ? "Sin proveedor" : object.getNombre();
            }

            @Override
            public Proveedor fromString(String string) {
                return null;
            }
        });
        cbProveedor.setItems(proveedoresCache != null ? FXCollections.observableArrayList(proveedoresCache) : FXCollections.observableArrayList());
        if (proveedoresCache == null) {
            cargarProveedores(false);
        }

        Button btnNuevoProveedor = new Button("Nuevo proveedor");
        btnNuevoProveedor.setOnAction(ev -> {
            Proveedor nuevo = mostrarFormularioProveedor();
            if (nuevo != null) {
                registrarNuevoProveedor(nuevo, proveedor -> {
                    cbProveedor.getItems().add(proveedor);
                    cbProveedor.getSelectionModel().select(proveedor);
                });
            }
        });

        txtNombre.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().length() < 2) {
                cbMarca.getItems().clear();
                return;
            }
            obtenerMarcasAsync(newVal.trim(), marcas -> cbMarca.getItems().setAll(marcas));
        });

        cbMarca.valueProperty().addListener((obs, oldVal, newVal) -> {
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty() || newVal == null || newVal.trim().isEmpty()) {
                cbPeso.getItems().clear();
                return;
            }
            obtenerPesosAsync(nombre, newVal, pesos -> {
                List<String> valores = new ArrayList<>();
                for (Double peso : pesos) {
                    valores.add(String.format(Locale.getDefault(), "%.2f", peso));
                }
                cbPeso.getItems().setAll(valores);
            });
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(txtNombre, 1, 0);
        grid.add(new Label("Marca:"), 0, 1);
        grid.add(cbMarca, 1, 1);
        grid.add(new Label("Peso (kg):"), 0, 2);
        grid.add(cbPeso, 1, 2);
        grid.add(new Label("Stock inicial:"), 0, 3);
        grid.add(txtStock, 1, 3);
        grid.add(new Label("Costo compra:"), 0, 4);
        grid.add(txtCosto, 1, 4);
        grid.add(new Label("Precio venta:"), 0, 5);
        grid.add(txtPrecio, 1, 5);
        grid.add(new Label("Umbral alerta:"), 0, 6);
        grid.add(txtUmbral, 1, 6);
        grid.add(new Label("Proveedor:"), 0, 7);
        grid.add(cbProveedor, 1, 7);
        grid.add(btnNuevoProveedor, 2, 7);

        dialog.getDialogPane().setContent(grid);

        Button guardarButton = (Button) dialog.getDialogPane().lookupButton(guardarBtn);
        guardarButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                validarRegistroEquipo(txtNombre.getText(), txtStock.getText(), txtCosto.getText(), txtPrecio.getText());
            } catch (IllegalArgumentException ex) {
                event.consume();
                mostrarAlerta("Validación", ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == guardarBtn) {
                try {
                    String nombre = txtNombre.getText().trim();
                    String marca = Optional.ofNullable(cbMarca.getValue()).map(String::trim).orElse(null);
                    double peso = 0;
                    String pesoText = cbPeso.getEditor().getText().trim();
                    if (!pesoText.isEmpty()) {
                        peso = parseDecimal(pesoText);
                    }
                    int stock = Integer.parseInt(txtStock.getText().trim());
                    double costo = parseDecimal(txtCosto.getText().trim());
                    double precio = parseDecimal(txtPrecio.getText().trim());
                    Integer proveedorId = Optional.ofNullable(cbProveedor.getValue()).map(Proveedor::getId).orElse(null);
                    Equipo equipo = new Equipo(nombre, marca, peso, stock, costo, precio, proveedorId);
                    if (!txtUmbral.getText().isBlank()) {
                        equipo.setUmbral(Integer.parseInt(txtUmbral.getText().trim()));
                    }
                    return equipo;
                } catch (Exception e) {
                    mostrarAlerta("Error", "Datos inválidos: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
            return null;
        });

        Optional<Equipo> resultado = dialog.showAndWait();
        resultado.ifPresent(equipo -> ejecutarConsultaAsync(() -> {
            int id = DatabaseUtil.insertarEquipo(equipo);
            equipo.setId(id);
            return equipo;
        }, nuevo -> {
            String claveNombre = nuevo.getNombre().toLowerCase(Locale.ROOT);
            marcasCache.remove(claveNombre);
            pesosCache.remove(claveNombre + "|" + Optional.ofNullable(nuevo.getMarca()).orElse("").toLowerCase(Locale.ROOT));
            cargarEquipos(true);
            mostrarEstado("Equipo registrado correctamente", false);
        }, "No se pudo registrar el equipo"));
    }

    private void validarRegistroEquipo(String nombre, String stock, String costo, String precio) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (stock == null || stock.trim().isEmpty()) {
            throw new IllegalArgumentException("El stock es obligatorio");
        }
        if (costo == null || costo.trim().isEmpty()) {
            throw new IllegalArgumentException("El costo es obligatorio");
        }
        if (precio == null || precio.trim().isEmpty()) {
            throw new IllegalArgumentException("El precio de venta es obligatorio");
        }
    }
    private TextFormatter<Integer> createPositiveIntegerFormatter() {
        return new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty() || INTEGER_PATTERN.matcher(newText).matches()) {
                return change;
            }
            return null;
        });
    }

    private TextFormatter<Double> createPositiveDecimalFormatter() {
        return new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty() || DECIMAL_PATTERN.matcher(newText).matches()) {
                return change;
            }
            return null;
        });
    }

    private double parseDecimal(String value) {
        String normalizado = value.replace(" ", "").replace("$", "").replace("S/", "");
        normalizado = normalizado.replace(',', '.');
        return Double.parseDouble(normalizado);
    }
    @FXML
    private void handleActualizarStock() {
        Equipo seleccionado = tablaEquipos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Selecciona un equipo", "Debes seleccionar un equipo para actualizar el stock", Alert.AlertType.WARNING);
            return;
        }
        TextInputDialog dialog = new TextInputDialog(String.valueOf(seleccionado.getStock()));
        dialog.setTitle("Actualizar Stock");
        dialog.setHeaderText("Nuevo stock para " + seleccionado.getNombre());
        TextField editor = dialog.getEditor();
        editor.setTextFormatter(createPositiveIntegerFormatter());
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String texto = editor.getText().trim();
            if (texto.isEmpty()) {
                mostrarAlerta("Validación", "El stock no puede estar vacío", Alert.AlertType.ERROR);
                event.consume();
            }
        });
        Optional<String> resultado = dialog.showAndWait();
        resultado.ifPresent(valor -> {
            try {
                int nuevoStock = Integer.parseInt(valor.trim());
                if (nuevoStock < 0) {
                    mostrarAlerta("Validación", "El stock debe ser positivo", Alert.AlertType.ERROR);
                    return;
                }
                Equipo equipoActual = seleccionado;
                ejecutarConsultaAsync(() -> {
                    DatabaseUtil.actualizarStockEquipo(equipoActual, nuevoStock, "Ajuste manual");
                    return null;
                }, v -> {
                    mostrarEstado("Stock actualizado", false);
                    cargarEquipos(true);
                    cargarHistorial(equipoActual);
                }, "No se pudo actualizar el stock");
            } catch (NumberFormatException e) {
                mostrarAlerta("Error", "Valor numérico inválido", Alert.AlertType.ERROR);
            }
        });
    }
    @FXML
    private void handleSubirPdf() {
        Equipo seleccionado = tablaEquipos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Selecciona un equipo", "Debes seleccionar un equipo para registrar una compra", Alert.AlertType.WARNING);
            return;
        }
        Proveedor proveedor = seleccionado.getProveedorId() != null ? proveedoresMap.get(seleccionado.getProveedorId()) : null;
        if (proveedor == null) {
            if (proveedoresCache == null) {
                cargarProveedores(false);
                mostrarEstado("Cargando proveedores...", false);
                mostrarAlerta("Proveedores", "Los proveedores se están cargando, intenta nuevamente en unos segundos.", Alert.AlertType.INFORMATION);
                return;
            }
            if (proveedoresCache.isEmpty()) {
                mostrarAlerta("Proveedores", "No hay proveedores disponibles. Registra uno primero.", Alert.AlertType.WARNING);
                return;
            }
            proveedor = seleccionarProveedorParaCompra();
            if (proveedor == null) {
                mostrarEstado("No se seleccionó proveedor", true);
                return;
            }
            seleccionado.setProveedorId(proveedor.getId());
        }
        proveedoresMap.put(proveedor.getId(), proveedor);

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File archivo = fc.showOpenDialog(tablaEquipos.getScene().getWindow());
        if (archivo == null) {
            return;
        }

        CompraFormData datos = mostrarDialogoCompra(seleccionado);
        if (datos == null) {
            return;
        }

        String rutaRelativa;
        try {
            rutaRelativa = copiarFactura(archivo, seleccionado);
        } catch (IOException e) {
            mostrarError("Error de archivo", "No se pudo copiar el comprobante", e);
            return;
        }

        Proveedor proveedorFinal = proveedor;
        ejecutarConsultaAsync(() -> {
            DatabaseUtil.registrarCompra(proveedorFinal.getId(), seleccionado, datos.cantidad, datos.costoUnitario,
                    datos.precioVenta, rutaRelativa, datos.moneda);
            return null;
        }, v -> {
            mostrarEstado("Compra registrada", false);
            cargarEquipos(true);
            cargarHistorial(seleccionado);
        }, "No se pudo registrar la compra");
    }

    private CompraFormData mostrarDialogoCompra(Equipo equipo) {
        Dialog<CompraFormData> dialog = new Dialog<>();
        dialog.setTitle("Registrar compra");
        ButtonType guardarBtn = new ButtonType("Registrar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarBtn, ButtonType.CANCEL);

        TextField txtCantidad = new TextField();
        txtCantidad.setTextFormatter(createPositiveIntegerFormatter());
        TextField txtCosto = new TextField(String.format(Locale.getDefault(), "%.2f", equipo.getCostoCompra()));
        txtCosto.setTextFormatter(createPositiveDecimalFormatter());
        TextField txtPrecioVenta = new TextField(String.format(Locale.getDefault(), "%.2f", equipo.getPrecioVenta()));
        txtPrecioVenta.setTextFormatter(createPositiveDecimalFormatter());
        ComboBox<String> cbMoneda = new ComboBox<>(FXCollections.observableArrayList("PEN", "USD", "EUR"));
        cbMoneda.getSelectionModel().selectFirst();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Cantidad"), 0, 0);
        grid.add(txtCantidad, 1, 0);
        grid.add(new Label("Costo unitario"), 0, 1);
        grid.add(txtCosto, 1, 1);
        grid.add(new Label("Precio venta"), 0, 2);
        grid.add(txtPrecioVenta, 1, 2);
        grid.add(new Label("Moneda"), 0, 3);
        grid.add(cbMoneda, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Button guardar = (Button) dialog.getDialogPane().lookupButton(guardarBtn);
        guardar.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (txtCantidad.getText().trim().isEmpty() || txtCosto.getText().trim().isEmpty() || txtPrecioVenta.getText().trim().isEmpty()) {
                mostrarAlerta("Validación", "Todos los campos son obligatorios", Alert.AlertType.ERROR);
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == guardarBtn) {
                try {
                    int cantidad = Integer.parseInt(txtCantidad.getText().trim());
                    double costo = parseDecimal(txtCosto.getText().trim());
                    double precioVenta = parseDecimal(txtPrecioVenta.getText().trim());
                    if (cantidad <= 0 || costo <= 0 || precioVenta <= 0) {
                        throw new IllegalArgumentException("Los valores deben ser positivos");
                    }
                    return new CompraFormData(cantidad, costo, precioVenta, cbMoneda.getValue());
                } catch (Exception e) {
                    mostrarAlerta("Error", "Datos inválidos: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    private Proveedor seleccionarProveedorParaCompra() {
        if (proveedoresCache == null || proveedoresCache.isEmpty()) {
            mostrarAlerta("Proveedores", "No hay proveedores registrados", Alert.AlertType.WARNING);
            return null;
        }
        Dialog<Proveedor> dialog = new Dialog<>();
        dialog.setTitle("Seleccionar proveedor");
        ButtonType guardarBtn = new ButtonType("Seleccionar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarBtn, ButtonType.CANCEL);

        ComboBox<Proveedor> combo = new ComboBox<>(FXCollections.observableArrayList(proveedoresCache));
        combo.setConverter(new StringConverter<>() {
                               @Override
                               public String toString(Proveedor object) {
                                   return object == null ? "" : object.getNombre();
                               }

            @Override
            public Proveedor fromString(String string) {
                return null;
            }
        });
        combo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Proveedor item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNombre());
            }
        });

        Button btnNuevoProveedor = new Button("Nuevo proveedor");
        btnNuevoProveedor.setOnAction(event -> {
            Proveedor nuevo = mostrarFormularioProveedor();
            if (nuevo != null) {
                registrarNuevoProveedor(nuevo, proveedor -> {
                    combo.getItems().add(proveedor);
                    combo.getSelectionModel().select(proveedor);
                });
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Proveedor"), 0, 0);
        grid.add(combo, 1, 0);
        grid.add(btnNuevoProveedor, 2, 0);
        dialog.getDialogPane().setContent(grid);

        Button guardar = (Button) dialog.getDialogPane().lookupButton(guardarBtn);
        guardar.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (combo.getValue() == null) {
                mostrarAlerta("Validación", "Selecciona un proveedor", Alert.AlertType.ERROR);
                event.consume();
            }
        });

        return dialog.showAndWait().orElse(null);
    }
    @FXML
    private void handleAbrirComprobante() {
        EquipoHistorial historial = tablaHistorial.getSelectionModel().getSelectedItem();
        if (historial == null || historial.getRutaPdf() == null || historial.getRutaPdf().isBlank()) {
            mostrarAlerta("Comprobante", "No hay comprobante para abrir", Alert.AlertType.INFORMATION);
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            mostrarAlerta("Comprobante", "El sistema no soporta la apertura de archivos", Alert.AlertType.WARNING);
            return;
        }
        try {
            Path ruta = Paths.get("database").resolve(Paths.get(historial.getRutaPdf()));
            if (!Files.exists(ruta)) {
                mostrarAlerta("Comprobante", "El archivo no existe: " + ruta, Alert.AlertType.ERROR);
                return;
            }
            Desktop.getDesktop().open(ruta.toFile());
        } catch (IOException e) {
            mostrarError("Comprobante", "No se pudo abrir el comprobante", e);
        }
    }

    private String copiarFactura(File origen, Equipo equipo) throws IOException {
        Path directorio = Paths.get("database", "facturas");
        Files.createDirectories(directorio);
        String nombreArchivo = String.format("equipo_%d_%d.pdf", equipo.getId(), System.currentTimeMillis());
        Path destino = directorio.resolve(nombreArchivo);
        Files.copy(origen.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
        return Paths.get("facturas", nombreArchivo).toString().replace("\\", "/");
    }

    private Proveedor mostrarFormularioProveedor() {
        Dialog<Proveedor> dialog = new Dialog<>();
        dialog.setTitle("Nuevo proveedor");
        ButtonType guardarBtn = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarBtn, ButtonType.CANCEL);

        TextField txtNombre = new TextField();
        TextField txtContacto = new TextField();
        TextField txtTelefono = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Nombre"), 0, 0);
        grid.add(txtNombre, 1, 0);
        grid.add(new Label("Contacto"), 0, 1);
        grid.add(txtContacto, 1, 1);
        grid.add(new Label("Teléfono"), 0, 2);
        grid.add(txtTelefono, 1, 2);
        dialog.getDialogPane().setContent(grid);

        Button guardar = (Button) dialog.getDialogPane().lookupButton(guardarBtn);
        guardar.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (txtNombre.getText().trim().isEmpty()) {
                mostrarAlerta("Validación", "El nombre es obligatorio", Alert.AlertType.ERROR);
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == guardarBtn) {
                Proveedor p = new Proveedor();
                p.setNombre(txtNombre.getText().trim());
                p.setContacto(txtContacto.getText().trim());
                p.setTelefono(txtTelefono.getText().trim());
                return p;
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    private void registrarNuevoProveedor(Proveedor proveedor, Consumer<Proveedor> onSuccess) {
        ejecutarConsultaAsync(() -> {
            int id = DatabaseUtil.insertarProveedor(proveedor);
            proveedor.setId(id);
            return proveedor;
        }, creado -> {
            if (creado.getId() > 0) {
                if (proveedoresCache == null) {
                    proveedoresCache = FXCollections.observableArrayList();
                }
                proveedoresCache.add(creado);
                proveedoresMap.put(creado.getId(), creado);
                cargarProveedores(true);
                onSuccess.accept(creado);
            } else {
                mostrarEstado("No se pudo registrar el proveedor", true);
            }
        }, "No se pudo registrar el proveedor");
    }

    private void obtenerMarcasAsync(String nombre, Consumer<ObservableList<String>> consumidor) {
        String clave = nombre.toLowerCase(Locale.ROOT);
        ObservableList<String> cached = marcasCache.get(clave);
        if (cached != null) {
            Platform.runLater(() -> consumidor.accept(FXCollections.observableArrayList(cached)));
            return;
        }
        ejecutarConsultaAsync(() -> DatabaseUtil.getMarcasPorNombre(nombre), marcas -> {
            ObservableList<String> copia = FXCollections.observableArrayList(marcas);
            marcasCache.put(clave, copia);
            consumidor.accept(FXCollections.observableArrayList(copia));
        }, "No se pudieron obtener las marcas");
    }

    private void obtenerPesosAsync(String nombre, String marca, Consumer<ObservableList<Double>> consumidor) {
        String clave = nombre.toLowerCase(Locale.ROOT) + "|" + marca.toLowerCase(Locale.ROOT);
        ObservableList<Double> cached = pesosCache.get(clave);
        if (cached != null) {
            Platform.runLater(() -> consumidor.accept(FXCollections.observableArrayList(cached)));
            return;
        }
        ejecutarConsultaAsync(() -> DatabaseUtil.getPesosPorNombreMarca(nombre, marca), pesos -> {
            ObservableList<Double> copia = FXCollections.observableArrayList(pesos);
            pesosCache.put(clave, copia);
            consumidor.accept(FXCollections.observableArrayList(copia));
        }, "No se pudieron obtener los pesos");
    }

    private <T> void ejecutarConsultaAsync(Callable<T> callable, Consumer<T> onSuccess, String mensajeError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return callable.call();
            }
        };
        task.setOnSucceeded(event -> Platform.runLater(() -> onSuccess.accept(task.getValue())));
        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            Platform.runLater(() -> mostrarError("Error", mensajeError, ex));
        });
        executor.submit(task);
    }

    private void mostrarEstado(String mensaje, boolean error) {
        Runnable accion = () -> {
            lblStatus.setText(mensaje);
            lblStatus.setStyle(error ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #2ecc71;");
        };
        if (Platform.isFxApplicationThread()) {
            accion.run();
        } else {
            Platform.runLater(accion);
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(titulo);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String titulo, String mensaje, Throwable throwable) {
        LOGGER.log(Level.SEVERE, mensaje, throwable);
        mostrarEstado(mensaje, true);
        mostrarAlerta(titulo, mensaje + (throwable != null ? "\n" + throwable.getMessage() : ""), Alert.AlertType.ERROR);
    }

    private static class CompraFormData {
        private final int cantidad;
        private final double costoUnitario;
        private final double precioVenta;
        private final String moneda;

        private CompraFormData(int cantidad, double costoUnitario, double precioVenta, String moneda) {
            this.cantidad = cantidad;
            this.costoUnitario = costoUnitario;
            this.precioVenta = precioVenta;
            this.moneda = moneda;
        }
    }
}
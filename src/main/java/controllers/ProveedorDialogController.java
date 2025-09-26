package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import models.Proveedor;
import models.ProveedorProducto;
import util.DatabaseUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProveedorDialogController {

    @FXML private TextField txtNombre;
    @FXML private TextField txtContacto;
    @FXML private TextField txtTelefono;

    @FXML private TabPane tabPaneCatalogos;
    @FXML private Tab tabEquipos;
    @FXML private Tab tabInsumos;

    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    private final ObservableList<ProveedorProducto> equiposDisponibles = FXCollections.observableArrayList();
    private final ObservableList<ProveedorProducto> insumosDisponibles = FXCollections.observableArrayList();

    private Stage stage;
    private Proveedor proveedorEdicion;
    private boolean guardado;
    private Proveedor proveedorResultado;
    private boolean catalogosCargados;
    private boolean inicializandoTabs;

    @FXML
    public void initialize() {
        inicializandoTabs = true;
        if (tabPaneCatalogos != null) {
            tabPaneCatalogos.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (inicializandoTabs) {
                    return;
                }
                if (newTab != null) {
                    if (newTab == tabEquipos) {
                        mostrarCatalogoModal("Equipos", equiposDisponibles, "EQUIPO");
                    } else if (newTab == tabInsumos) {
                        mostrarCatalogoModal("Insumos", insumosDisponibles, "INSUMO");
                    }
                    Platform.runLater(() -> seleccionarTabSinEvento(newTab));
                }
            });
            Platform.runLater(() -> {
                Tab tabPorDefecto = tabEquipos != null ? tabEquipos
                        : (tabPaneCatalogos.getTabs().isEmpty() ? null : tabPaneCatalogos.getTabs().get(0));
                seleccionarTabSinEvento(tabPorDefecto);
                inicializandoTabs = false;
            });
        } else {
            inicializandoTabs = false;
        }
        cargarCatalogos();
    }

    private void seleccionarTabSinEvento(Tab tab) {
        if (tabPaneCatalogos == null || tab == null) {
            return;
        }
        inicializandoTabs = true;
        tabPaneCatalogos.getSelectionModel().select(tab);
        inicializandoTabs = false;
    }

    private void cargarCatalogos() {
        try {
            equiposDisponibles.clear();
            DatabaseUtil.listarEquipos().forEach(equipo -> {
                ProveedorProducto producto = new ProveedorProducto();
                producto.setTipo("EQUIPO");
                producto.setEquipoId(equipo.getId());
                producto.setNombreProducto(equipo.getNombre());
                equiposDisponibles.add(producto);
            });

            insumosDisponibles.clear();
            DatabaseUtil.getProductos().forEach(prod -> {
                ProveedorProducto producto = new ProveedorProducto();
                producto.setTipo("INSUMO");
                producto.setProductoId(prod.getId());
                producto.setNombreProducto(prod.getNombre());
                insumosDisponibles.add(producto);
            });
            catalogosCargados = true;
            if (proveedorEdicion != null) {
                cargarProveedorEnFormulario(proveedorEdicion);
            }
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudieron cargar los catálogos de productos.");
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedorEdicion = proveedor;
        if (catalogosCargados) {
            cargarProveedorEnFormulario(proveedor);
        }
    }

    private void cargarProveedorEnFormulario(Proveedor proveedor) {
        txtNombre.setText(Optional.ofNullable(proveedor.getNombre()).orElse(""));
        txtContacto.setText(Optional.ofNullable(proveedor.getContacto()).orElse(""));
        txtTelefono.setText(Optional.ofNullable(proveedor.getTelefono()).orElse(""));

        try {
            ObservableList<ProveedorProducto> productos = DatabaseUtil.obtenerProductosProveedor(proveedor.getId());
            aplicarSeleccion(equiposDisponibles, productos, "EQUIPO");
            aplicarSeleccion(insumosDisponibles, productos, "INSUMO");
            // Productos que ya no existen en el catálogo pero están asociados
            for (ProveedorProducto producto : productos) {
                if ("EQUIPO".equalsIgnoreCase(producto.getTipo()) &&
                        equiposDisponibles.stream().noneMatch(p -> Objects.equals(p.getEquipoId(), producto.getEquipoId()))) {
                    equiposDisponibles.add(producto);
                }
                if ("INSUMO".equalsIgnoreCase(producto.getTipo()) &&
                        insumosDisponibles.stream().noneMatch(p -> Objects.equals(p.getProductoId(), producto.getProductoId()))) {
                    insumosDisponibles.add(producto);
                }
            }
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudieron cargar los productos del proveedor.");
        }
    }

    private void aplicarSeleccion(List<ProveedorProducto> disponibles, List<ProveedorProducto> seleccionados, String tipo) {
        for (ProveedorProducto disponible : disponibles) {
            seleccionados.stream()
                    .filter(prod -> tipo.equalsIgnoreCase(prod.getTipo()))
                    .filter(prod -> ("EQUIPO".equalsIgnoreCase(tipo) && Objects.equals(prod.getEquipoId(), disponible.getEquipoId())) ||
                            ("INSUMO".equalsIgnoreCase(tipo) && Objects.equals(prod.getProductoId(), disponible.getProductoId())))
                    .findFirst()
                    .ifPresent(prod -> {
                        disponible.setSeleccionado(true);
                        disponible.setPrecio(prod.getPrecio());
                    });
        }
    }

    @FXML
    private void guardar() {
        if (!validarFormulario()) {
            return;
        }
        Proveedor proveedorAGuardar = proveedorEdicion != null ? proveedorEdicion : new Proveedor();
        proveedorAGuardar.setNombre(txtNombre.getText());
        proveedorAGuardar.setContacto(txtContacto.getText());
        proveedorAGuardar.setTelefono(txtTelefono.getText());

        List<ProveedorProducto> productosSeleccionados = obtenerProductosSeleccionados();
        try {
            if (proveedorAGuardar.getId() == 0) {
                int nuevoId = DatabaseUtil.insertarProveedor(proveedorAGuardar);
                proveedorAGuardar.setId(nuevoId);
            } else {
                DatabaseUtil.actualizarProveedor(proveedorAGuardar);
            }
            DatabaseUtil.reemplazarProductosProveedor(proveedorAGuardar.getId(), productosSeleccionados);
            proveedorAGuardar.setProductos(DatabaseUtil.obtenerProductosProveedor(proveedorAGuardar.getId()));
            proveedorResultado = proveedorAGuardar;
            guardado = true;
            cerrar();
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo guardar el proveedor.");
        }
    }

    private List<ProveedorProducto> obtenerProductosSeleccionados() {
        List<ProveedorProducto> seleccionados = new ArrayList<>();
        equiposDisponibles.stream().filter(ProveedorProducto::isSeleccionado).forEach(prod -> {
            ProveedorProducto item = new ProveedorProducto();
            item.setTipo("EQUIPO");
            item.setEquipoId(prod.getEquipoId());
            item.setNombreProducto(prod.getNombreProducto());
            item.setPrecio(prod.getPrecio());
            seleccionados.add(item);
        });
        insumosDisponibles.stream().filter(ProveedorProducto::isSeleccionado).forEach(prod -> {
            ProveedorProducto item = new ProveedorProducto();
            item.setTipo("INSUMO");
            item.setProductoId(prod.getProductoId());
            item.setNombreProducto(prod.getNombreProducto());
            item.setPrecio(prod.getPrecio());
            seleccionados.add(item);
        });
        return seleccionados;
    }

    private boolean validarFormulario() {
        if (txtNombre.getText() == null || txtNombre.getText().isBlank()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Validación", "El nombre del proveedor es obligatorio.");
            return false;
        }
        List<ProveedorProducto> seleccionados = new ArrayList<>();
        seleccionados.addAll(equiposDisponibles.stream().filter(ProveedorProducto::isSeleccionado).collect(Collectors.toList()));
        seleccionados.addAll(insumosDisponibles.stream().filter(ProveedorProducto::isSeleccionado).collect(Collectors.toList()));
        if (seleccionados.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Validación", "Selecciona al menos un producto o equipo para el proveedor.");
            return false;
        }
        boolean preciosValidos = seleccionados.stream().allMatch(prod -> prod.getPrecio() > 0);
        if (!preciosValidos) {
            mostrarAlerta(Alert.AlertType.WARNING, "Validación", "Cada producto seleccionado debe tener un precio mayor a cero.");
            return false;
        }
        return true;
    }

    @FXML
    private void cancelar() {
        cerrar();
    }

    private void cerrar() {
        if (stage != null) {
            stage.close();
        }
    }

    private void mostrarCatalogoModal(String titulo, ObservableList<ProveedorProducto> datos, String tipo) {
        Stage dialogo = new Stage();
        if (stage != null) {
            dialogo.initOwner(stage);
        }
        dialogo.initModality(Modality.APPLICATION_MODAL);
        dialogo.setTitle("Catálogo de " + titulo);

        TableView<ProveedorProducto> tabla = new TableView<>();
        tabla.setEditable(true);
        tabla.setItems(datos);
        tabla.getStyleClass().add("glass-table");

        TableColumn<ProveedorProducto, Boolean> colSeleccion = new TableColumn<>("Seleccionar");
        colSeleccion.setPrefWidth(140);
        colSeleccion.setCellValueFactory(cell -> cell.getValue().seleccionadoProperty());
        colSeleccion.setCellFactory(column -> {
            CheckBoxTableCell<ProveedorProducto, Boolean> cell = new CheckBoxTableCell<>(index -> {
                ProveedorProducto producto = tabla.getItems().get(index);
                return producto.seleccionadoProperty();
            });
            cell.setEditable(true);
            return cell;
        });

        TableColumn<ProveedorProducto, String> colNombre = new TableColumn<>("EQUIPO".equalsIgnoreCase(tipo) ? "Equipo" : "Insumo");
        colNombre.setPrefWidth(300);
        colNombre.setCellValueFactory(cell -> cell.getValue().nombreProductoProperty());
        colNombre.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : Optional.ofNullable(item).orElse("Sin nombre"));
            }
        });

        TableColumn<ProveedorProducto, Double> colPrecio = new TableColumn<>("Precio");
        colPrecio.setPrefWidth(160);
        colPrecio.setCellValueFactory(cell -> cell.getValue().precioProperty().asObject());
        colPrecio.setCellFactory(column -> new PrecioTableCell());
        colPrecio.setOnEditCommit(event -> {
            ProveedorProducto producto = event.getRowValue();
            Double nuevo = event.getNewValue();
            if (producto != null && nuevo != null) {
                producto.setPrecio(Math.max(0, nuevo));
            }
        });

        tabla.getColumns().addAll(colSeleccion, colNombre, colPrecio);
        tabla.setPlaceholder(new Label("No hay productos disponibles"));

        Label descripcion = new Label(
                "EQUIPO".equalsIgnoreCase(tipo)
                        ? "Selecciona los equipos que suministra el proveedor e indica su precio."
                        : "Selecciona los insumos y registra su precio de compra."
        );
        descripcion.getStyleClass().add("tab-description");

        VBox encabezado = new VBox(8, descripcion);
        encabezado.getStyleClass().add("tab-content");

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.getStyleClass().add("secondary-button");
        btnCerrar.setOnAction(e -> dialogo.close());

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox pie = new HBox(12, espacio, btnCerrar);
        pie.getStyleClass().add("footer-actions");

        BorderPane contenedor = new BorderPane();
        contenedor.setPadding(new Insets(20));
        contenedor.setTop(encabezado);
        BorderPane.setMargin(encabezado, new Insets(0, 0, 16, 0));
        contenedor.setCenter(tabla);
        contenedor.setBottom(pie);
        BorderPane.setMargin(pie, new Insets(16, 0, 0, 0));

        Scene escena = new Scene(contenedor, 720, 480);
        java.net.URL css = getClass().getResource("/css/proveedor_dialog.css");
        if (css != null) {
            escena.getStylesheets().add(css.toExternalForm());
        }
        dialogo.setScene(escena);
        dialogo.showAndWait();
    }

    public boolean isGuardado() {
        return guardado;
    }

    public Proveedor getProveedorResultado() {
        return proveedorResultado;
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private static class PrecioTableCell extends TableCell<ProveedorProducto, Double> {
        private final TextFieldTableCellConverter converter = new TextFieldTableCellConverter();

        PrecioTableCell() {
        }

        @Override
        public void startEdit() {
            ProveedorProducto producto = getTableRow() != null ? getTableRow().getItem() : null;
            if (producto == null || !producto.isSeleccionado()) {
                return;
            }
            super.startEdit();
            setText(null);
            setGraphic(converter.createEditor(this, producto.getPrecio()));
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setGraphic(null);
            actualizarTexto(getItem());
        }

        @Override
        protected void updateItem(Double item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                ProveedorProducto producto = getTableRow() != null ? getTableRow().getItem() : null;
                if (producto != null && producto.isSeleccionado()) {
                    if (isEditing()) {
                        setText(null);
                        setGraphic(converter.createEditor(this, producto.getPrecio()));
                    } else {
                        setGraphic(null);
                        actualizarTexto(item);
                    }
                } else {
                    setGraphic(null);
                    setText("-");
                }
            }
        }

        private void actualizarTexto(Double valor) {
            if (valor == null || valor <= 0) {
                setText("0.00");
            } else {
                setText(String.format(Locale.US, "%.2f", valor));
            }
        }

        private static class TextFieldTableCellConverter extends StringConverter<Double> {
            private javafx.scene.control.TextField editor;

            @Override
            public String toString(Double object) {
                return object == null ? "" : String.format(Locale.US, "%.2f", object);
            }

            @Override
            public Double fromString(String string) {
                if (string == null || string.isBlank()) {
                    return 0.0;
                }
                try {
                    String normalizado = string.replace(',', '.');
                    return Double.parseDouble(normalizado);
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }

            javafx.scene.control.TextField createEditor(TableCell<ProveedorProducto, Double> cell, Double valor) {
                editor = new javafx.scene.control.TextField();
                editor.setText(toString(valor));
                editor.setOnAction(evt -> commit(cell));
                editor.focusedProperty().addListener((obs, old, focused) -> {
                    if (!focused) {
                        commit(cell);
                    }
                });
                editor.textProperty().addListener((obs, old, nuevo) -> {
                    if (nuevo != null && !nuevo.matches("[0-9]*[.,]?[0-9]*")) {
                        editor.setText(old);
                    }
                });
                return editor;
            }

            private void commit(TableCell<ProveedorProducto, Double> cell) {
                Double valor = fromString(editor.getText());
                ProveedorProducto producto = cell.getTableRow() != null ? cell.getTableRow().getItem() : null;
                if (producto != null) {
                    producto.setPrecio(Math.max(0, valor));
                    cell.commitEdit(producto.getPrecio());
                } else {
                    cell.cancelEdit();
                }
            }
        }
    }
}
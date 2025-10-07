package controllers;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.CatalogoItem;
import models.Proveedor;
import models.ProveedorProducto;
import util.DatabaseUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ListaProveedoresController {

    @FXML private TableView<Proveedor> tablaProveedores;
    @FXML private TableColumn<Proveedor, String> colNombre;
    @FXML private TableColumn<Proveedor, String> colContacto;
    @FXML private TableColumn<Proveedor, String> colTelefono;
    @FXML private TableColumn<Proveedor, String> colCategorias;

    @FXML private CheckBox chkEquipos;
    @FXML private CheckBox chkInsumos;
    @FXML private ComboBox<String> cmbCategoriaProducto;
    @FXML private ComboBox<CatalogoItem> cmbProductoFiltro;

    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;

    @FXML private Button btnComparador;

    @FXML private Button btnVerEquipos;
    @FXML private Button btnVerInsumos;

    @FXML private javafx.scene.control.Label lblNombreDetalle;
    @FXML private javafx.scene.control.Label lblContactoDetalle;
    @FXML private javafx.scene.control.Label lblTelefonoDetalle;
    @FXML private javafx.scene.control.Label lblProductoSeleccionado;

    private final ObservableList<Proveedor> proveedores = FXCollections.observableArrayList();
    private FilteredList<Proveedor> proveedoresFiltrados;
    private CatalogoItem productoSeleccionado;
    private String categoriaSeleccionada;
    private final ObservableList<String> equiposDetalle = FXCollections.observableArrayList();
    private final ObservableList<String> insumosDetalle = FXCollections.observableArrayList();
    private Proveedor proveedorSeleccionado;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFiltros();
        cargarProveedores();
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colContacto.setCellValueFactory(new PropertyValueFactory<>("contacto"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colCategorias.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getResumenCategorias()));

        proveedoresFiltrados = new FilteredList<>(proveedores, this::filtrarProveedor);
        SortedList<Proveedor> ordenados = new SortedList<>(proveedoresFiltrados);
        ordenados.comparatorProperty().bind(tablaProveedores.comparatorProperty());
        tablaProveedores.setItems(ordenados);

        tablaProveedores.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, nuevo) -> mostrarDetalleProveedor(nuevo));

        btnEditar.disableProperty().bind(Bindings.isNull(tablaProveedores.getSelectionModel().selectedItemProperty()));
        btnEliminar.disableProperty().bind(Bindings.isNull(tablaProveedores.getSelectionModel().selectedItemProperty()));
        btnVerEquipos.disableProperty().bind(Bindings.isNull(tablaProveedores.getSelectionModel().selectedItemProperty()));
        btnVerInsumos.disableProperty().bind(Bindings.isNull(tablaProveedores.getSelectionModel().selectedItemProperty()));
    }

    private void configurarFiltros() {
        chkEquipos.setSelected(true);
        chkInsumos.setSelected(true);
        chkEquipos.selectedProperty().addListener((obs, old, val) -> aplicarFiltros());
        chkInsumos.selectedProperty().addListener((obs, old, val) -> aplicarFiltros());

        cmbCategoriaProducto.setItems(FXCollections.observableArrayList("Equipos", "Insumos"));
        cmbCategoriaProducto.valueProperty().addListener((obs, old, nuevo) -> manejarCambioCategoria(nuevo));
        cmbProductoFiltro.valueProperty().addListener((obs, old, nuevo) -> {
            productoSeleccionado = nuevo;
            lblProductoSeleccionado.setText(nuevo != null ? "Producto seleccionado: " + nuevo.getNombre() : "Sin filtro de producto");
            aplicarFiltros();
            tablaProveedores.refresh();
        });
        cmbCategoriaProducto.getSelectionModel().selectFirst();
    }

    private void manejarCambioCategoria(String categoria) {
        categoriaSeleccionada = categoria != null ? categoria.toUpperCase() : null;
        productoSeleccionado = null;
        cmbProductoFiltro.getSelectionModel().clearSelection();
        lblProductoSeleccionado.setText("Sin filtro de producto");
        cargarCatalogoProductos();
        aplicarFiltros();
        tablaProveedores.refresh();
    }

    private void cargarCatalogoProductos() {
        if (categoriaSeleccionada == null) {
            cmbProductoFiltro.setItems(FXCollections.emptyObservableList());
            return;
        }
        try {
            if ("EQUIPOS".equals(categoriaSeleccionada)) {
                List<CatalogoItem> items = DatabaseUtil.listarEquipos().stream()
                        .map(eq -> new CatalogoItem(eq.getId(), eq.getNombre(), "EQUIPO"))
                        .sorted(Comparator.comparing(CatalogoItem::getNombre))
                        .toList();
                cmbProductoFiltro.setItems(FXCollections.observableArrayList(items));
            } else {
                List<CatalogoItem> items = DatabaseUtil.getProductos().stream()
                        .map(prod -> new CatalogoItem(prod.getId(), prod.getNombre(), "INSUMO"))
                        .sorted(Comparator.comparing(CatalogoItem::getNombre))
                        .toList();
                cmbProductoFiltro.setItems(FXCollections.observableArrayList(items));
            }
        } catch (SQLException e) {
            mostrarError("No se pudo cargar el catálogo de productos.");
            cmbProductoFiltro.setItems(FXCollections.emptyObservableList());
        }
    }

    private void cargarProveedores() {
        try {
            proveedores.setAll(DatabaseUtil.obtenerProveedoresDetallados());
            aplicarFiltros();
        } catch (SQLException e) {
            mostrarError("No se pudieron cargar los proveedores registrados.");
        }
    }

    private boolean filtrarProveedor(Proveedor proveedor) {
        if (proveedor == null) {
            return false;
        }
        boolean mostrarEquipos = chkEquipos.isSelected();
        boolean mostrarInsumos = chkInsumos.isSelected();
        if (!mostrarEquipos && !mostrarInsumos) {
            mostrarEquipos = true;
            mostrarInsumos = true;
        }

        boolean coincideCategoria = (mostrarEquipos && proveedor.suministraEquipos()) ||
                (mostrarInsumos && proveedor.suministraInsumos()) ||
                (!proveedor.suministraEquipos() && !proveedor.suministraInsumos());

        if (!coincideCategoria) {
            return false;
        }

        if (productoSeleccionado != null && categoriaSeleccionada != null) {
            String tipo = categoriaSeleccionada.equals("EQUIPOS") ? "EQUIPO" : "INSUMO";
            return proveedor.obtenerPrecioPara(tipo, productoSeleccionado.getId()).isPresent();
        }
        return true;
    }

    private void aplicarFiltros() {
        if (proveedoresFiltrados != null) {
            proveedoresFiltrados.setPredicate(this::filtrarProveedor);
        }
    }

    private void mostrarDetalleProveedor(Proveedor proveedor) {
        proveedorSeleccionado = proveedor;

        if (proveedor == null) {
            lblNombreDetalle.setText("Selecciona un proveedor");
            lblContactoDetalle.setText("-");
            lblTelefonoDetalle.setText("-");
            equiposDetalle.clear();
            insumosDetalle.clear();
            return;
        }
        lblNombreDetalle.setText(Optional.ofNullable(proveedor.getNombre()).orElse("Sin nombre"));
        lblContactoDetalle.setText(Optional.ofNullable(proveedor.getContacto()).filter(s -> !s.isBlank()).orElse("Sin contacto"));
        lblTelefonoDetalle.setText(Optional.ofNullable(proveedor.getTelefono()).filter(s -> !s.isBlank()).orElse("Sin teléfono"));

        List<String> equipos = proveedor.getEquiposSuministrados().stream()
                .map(prod -> formatearProducto(prod, "EQUIPO"))
                .collect(Collectors.toList());
        List<String> insumos = proveedor.getInsumosSuministrados().stream()
                .map(prod -> formatearProducto(prod, "INSUMO"))
                .collect(Collectors.toList());
        equiposDetalle.setAll(equipos);
        insumosDetalle.setAll(insumos);
    }

    @FXML
    private void verEquiposProveedor() {
        if (proveedorSeleccionado == null) {
            mostrarInformacion("Selecciona un proveedor", "Debes seleccionar un proveedor para consultar sus equipos.");
            return;
        }
        String nombreProveedor = Optional.ofNullable(proveedorSeleccionado.getNombre()).orElse("este proveedor");
        mostrarVentanaSuministros(
                "Equipos suministrados por " + nombreProveedor,
                equiposDetalle,
                "Este proveedor no tiene equipos registrados."
        );
    }

    @FXML
    private void verInsumosProveedor() {
        if (proveedorSeleccionado == null) {
            mostrarInformacion("Selecciona un proveedor", "Debes seleccionar un proveedor para consultar sus insumos.");
            return;
        }
        String nombreProveedor = Optional.ofNullable(proveedorSeleccionado.getNombre()).orElse("este proveedor");
        mostrarVentanaSuministros(
                "Insumos suministrados por " + nombreProveedor,
                insumosDetalle,
                "Este proveedor no tiene insumos registrados."
        );
    }

    private void mostrarVentanaSuministros(String tituloVentana, ObservableList<String> items, String mensajeVacio) {
        Stage stage = new Stage();
        Stage owner = (Stage) tablaProveedores.getScene().getWindow();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle(tituloVentana);

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("detail-dialog");

        javafx.scene.control.Label tituloLabel = new javafx.scene.control.Label(tituloVentana);
        tituloLabel.getStyleClass().add("detail-dialog-title");
        root.getChildren().add(tituloLabel);

        if (items.isEmpty()) {
            javafx.scene.control.Label vacioLabel = new javafx.scene.control.Label(mensajeVacio);
            vacioLabel.getStyleClass().add("detail-dialog-empty");
            root.getChildren().add(vacioLabel);
        } else {
            ListView<String> lista = new ListView<>();
            lista.setItems(FXCollections.observableArrayList(items));
            lista.getStyleClass().add("detail-dialog-list");
            root.getChildren().add(lista);
        }

        Scene scene = new Scene(root, 460, 360);
        scene.getStylesheets().add(getClass().getResource("/css/proveedores.css").toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void mostrarInformacion(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private String formatearProducto(ProveedorProducto producto, String tipo) {
        String nombre = Optional.ofNullable(producto.getNombreProducto()).orElse("Producto sin nombre");
        String etiqueta = "EQUIPO".equals(tipo) ? "Equipo" : "Insumo";
        String precio = producto.getPrecio() > 0 ? String.format("$ %.2f", producto.getPrecio()) : "Sin precio";

        StringBuilder descripcion = new StringBuilder(etiqueta)
                .append(": ")
                .append(nombre)
                .append(" - ")
                .append(precio);

        if ("EQUIPO".equals(tipo)) {
            String peso = Optional.ofNullable(producto.getPeso()).map(String::trim).orElse("");
            if (!peso.isEmpty()) {
                descripcion.append(" - Peso: ").append(peso);
            }
        }

        return descripcion.toString();
    }

    @FXML
    private void agregarProveedor() {
        mostrarDialogoProveedor(null);
    }

    @FXML
    private void editarProveedor() {
        Proveedor seleccionado = tablaProveedores.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            mostrarDialogoProveedor(seleccionado);
        }
    }

    @FXML
    private void eliminarProveedor() {
        Proveedor seleccionado = tablaProveedores.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Eliminar proveedor");
        confirmacion.setHeaderText("¿Deseas eliminar al proveedor seleccionado?");
        confirmacion.setContentText(Optional.ofNullable(seleccionado.getNombre()).orElse("Proveedor sin nombre"));
        confirmacion.showAndWait().filter(response -> response == javafx.scene.control.ButtonType.OK).ifPresent(respuesta -> {
            try {
                DatabaseUtil.eliminarProveedor(seleccionado.getId());
                proveedores.remove(seleccionado);
                mostrarDetalleProveedor(null);
            } catch (SQLException e) {
                mostrarError("No se pudo eliminar al proveedor.");
            }
        });
    }

    @FXML
    private void abrirComparador() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/comparacion_precios.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Comparación de precios");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            mostrarError("No se pudo abrir la pantalla de comparación de precios.");
        }
    }

    @FXML
    private void limpiarFiltroProducto() {
        cmbProductoFiltro.getSelectionModel().clearSelection();
        productoSeleccionado = null;
        lblProductoSeleccionado.setText("Sin filtro de producto");
        aplicarFiltros();
        tablaProveedores.refresh();
    }

    @FXML
    private void refrescarTabla() {
        cargarProveedores();
        tablaProveedores.refresh();
    }

    private void mostrarDialogoProveedor(Proveedor proveedorEdicion) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogo_proveedor.fxml"));
            Parent root = loader.load();
            ProveedorDialogController controller = loader.getController();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(proveedorEdicion == null ? "Nuevo proveedor" : "Editar proveedor");
            stage.setScene(new Scene(root));
            controller.setStage(stage);
            if (proveedorEdicion != null) {
                controller.setProveedor(proveedorEdicion);
            }
            stage.showAndWait();

            if (controller.isGuardado()) {
                Proveedor proveedorActualizado = controller.getProveedorResultado();
                cargarProveedores();
                seleccionarProveedorEnTabla(proveedorActualizado.getId());
            }
        } catch (IOException e) {
            mostrarError("Ocurrió un error al abrir el formulario de proveedor.");
        }
    }

    private void seleccionarProveedorEnTabla(int proveedorId) {
        for (Proveedor proveedor : tablaProveedores.getItems()) {
            if (proveedor.getId() == proveedorId) {
                tablaProveedores.getSelectionModel().select(proveedor);
                tablaProveedores.scrollTo(proveedor);
                return;
            }
        }
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
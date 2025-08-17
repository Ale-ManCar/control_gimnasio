package controllers;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import models.Egreso;
import models.Producto;
import util.AuditoriaUtil;
import util.DatabaseUtil;
import util.EventBus;
import util.SessionManager;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

public class RegistroEgresoController implements Initializable {

    @FXML private TextField txtMonto;
    @FXML private TextArea txtDescripcion;
    @FXML private ComboBox<String> cbCategoria;
    @FXML private Button btnRegistrar;
    @FXML private Button btnCancelar;

    // Campos para compras
    @FXML private Label lblNumeroFactura;
    @FXML private TextField txtNumeroFactura;
    @FXML private Label lblProveedor;
    @FXML private ComboBox<String> cbProveedor;
    @FXML private TableView<ItemCompra> tablaItems;
    @FXML private TableColumn<ItemCompra, String> colProducto;
    @FXML private TableColumn<ItemCompra, Integer> colCantidad;
    @FXML private TableColumn<ItemCompra, Double> colCosto;
    @FXML private HBox boxItemsButtons;
    @FXML private Button btnAdjuntarFactura;

    private final ObservableList<ItemCompra> items = FXCollections.observableArrayList();
    private File archivoAdjunto;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!SessionManager.isAdmin()) {
            btnRegistrar.setDisable(true);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Permiso denegado", ButtonType.OK);
            alert.showAndWait();
        }

        cbCategoria.getItems().addAll(
                "Alquiler",
                "Servicios",
                "Mantenimiento",
                "Compra",
                "Insumos",
                "Salarios",
                "Marketing",
                "Otros"
        );
        cbCategoria.getSelectionModel().selectFirst();
        cbCategoria.valueProperty().addListener((obs, oldVal, newVal) -> mostrarCamposCompra("Compra".equals(newVal)));
        mostrarCamposCompra(false);

        // Conversión a mayúsculas
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getText();
            if (!text.isEmpty()) {
                change.setText(text.toUpperCase());
            }
            return change;
        };
        txtDescripcion.setTextFormatter(new TextFormatter<>(filter));
        txtDescripcion.setWrapText(true);
        txtDescripcion.setPrefRowCount(4);
        btnRegistrar.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-padding: 10 20; -fx-font-size: 14px;");
        btnCancelar.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; "
                + "-fx-padding: 10 20; -fx-font-size: 14px;");


        colProducto.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getProducto().getNombre()));
        colCantidad.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getCantidad()).asObject());
        colCosto.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getCosto()).asObject());
        tablaItems.setItems(items);
    }

    private void mostrarCamposCompra(boolean mostrar) {
        lblNumeroFactura.setVisible(mostrar);
        lblNumeroFactura.setManaged(mostrar);
        txtNumeroFactura.setVisible(mostrar);
        txtNumeroFactura.setManaged(mostrar);
        lblProveedor.setVisible(mostrar);
        lblProveedor.setManaged(mostrar);
        cbProveedor.setVisible(mostrar);
        cbProveedor.setManaged(mostrar);
        tablaItems.setVisible(mostrar);
        tablaItems.setManaged(mostrar);
        boxItemsButtons.setVisible(mostrar);
        boxItemsButtons.setManaged(mostrar);
        btnAdjuntarFactura.setVisible(mostrar);
        btnAdjuntarFactura.setManaged(mostrar);
        txtMonto.setEditable(!mostrar);
        if (!mostrar) {
            items.clear();
            txtNumeroFactura.clear();
            archivoAdjunto = null;
        }
    }

    @FXML
    private void handleAgregarItem() {
        ItemCompra nuevo = mostrarDialogoItem();
        if (nuevo != null) {
            items.add(nuevo);
            recalcularTotal();
        }
    }

    @FXML
    private void handleEliminarItem() {
        ItemCompra seleccionado = tablaItems.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            items.remove(seleccionado);
            recalcularTotal();
        }
    }

    private void recalcularTotal() {
        double total = 0;
        for (ItemCompra item : items) {
            total += item.getCantidad() * item.getCosto();
        }
        txtMonto.setText(String.format("%.2f", total));
    }

    private ItemCompra mostrarDialogoItem() {
        try {
            ObservableList<Producto> productos = DatabaseUtil.getProductos();
            if (productos.isEmpty()) {
                mostrarError("No hay productos registrados");
                return null;
            }

            Dialog<ItemCompra> dialog = new Dialog<>();
            dialog.setTitle("Agregar ítem");
            ButtonType btnAceptar = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(btnAceptar, ButtonType.CANCEL);

            ComboBox<Producto> cbProducto = new ComboBox<>(productos);
            cbProducto.setConverter(new StringConverter<Producto>() {
                @Override
                public String toString(Producto producto) {
                    return producto == null ? "" : producto.getNombre();
                }

                @Override
                public Producto fromString(String string) {
                    return null;
                }
            });
            cbProducto.setCellFactory(param -> new ListCell<Producto>() {
                @Override
                protected void updateItem(Producto producto, boolean empty) {
                    super.updateItem(producto, empty);
                    setText(empty || producto == null ? null : producto.getNombre());
                }
            });

            TextField txtCantidad = new TextField();
            txtCantidad.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\\\d*")) {
                    txtCantidad.setText(newVal.replaceAll("[^\\\\d]", ""));
                }
            });

            TextField txtCosto = new TextField();
            txtCosto.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\\\d*(\\\\.\\\\d*)?")) {
                    txtCosto.setText(newVal.replaceAll("[^\\\\d.]", ""));
                }
            });

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.add(new Label("Producto:"), 0, 0);
            grid.add(cbProducto, 1, 0);
            grid.add(new Label("Cantidad:"), 0, 1);
            grid.add(txtCantidad, 1, 1);
            grid.add(new Label("Costo:"), 0, 2);
            grid.add(txtCosto, 1, 2);
            dialog.getDialogPane().setContent(grid);

            Node aceptar = dialog.getDialogPane().lookupButton(btnAceptar);
            aceptar.setDisable(true);
            cbProducto.valueProperty().addListener((obs, oldVal, newVal) ->
                    aceptar.setDisable(newVal == null || txtCantidad.getText().trim().isEmpty() || txtCosto.getText().trim().isEmpty()));
            txtCantidad.textProperty().addListener((obs, oldVal, newVal) ->
                    aceptar.setDisable(cbProducto.getValue() == null || newVal.trim().isEmpty() || txtCosto.getText().trim().isEmpty()));
            txtCosto.textProperty().addListener((obs, oldVal, newVal) ->
                    aceptar.setDisable(cbProducto.getValue() == null || txtCantidad.getText().trim().isEmpty() || newVal.trim().isEmpty()));

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == btnAceptar) {
                    int cant = Integer.parseInt(txtCantidad.getText().trim());
                    double costo = Double.parseDouble(txtCosto.getText().trim());
                    return new ItemCompra(cbProducto.getValue(), cant, costo);
                }
                return null;
            });

            Optional<ItemCompra> result = dialog.showAndWait();
            return result.orElse(null);
        } catch (Exception e) {
            mostrarError("Error al cargar productos");
            return null;
        }
    }

    @FXML
    private void handleAdjuntarFactura() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar factura");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos PDF/JPG/PNG", "*.pdf", "*.jpg", "*.png")
        );
        File file = chooser.showOpenDialog(btnAdjuntarFactura.getScene().getWindow());
        if (file != null) {
            try {
                LocalDate now = LocalDate.now();
                Path dir = Paths.get("facturas",
                        String.format("%04d", now.getYear()),
                        String.format("%02d", now.getMonthValue()));
                Files.createDirectories(dir);
                Path dest = dir.resolve(file.getName());
                Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                archivoAdjunto = dest.toFile();
            } catch (Exception e) {
                mostrarError("No se pudo adjuntar la factura: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleRegistrar() {
        try {
            if (txtDescripcion.getText().trim().isEmpty()) {
                mostrarError("Por favor ingrese una descripción");
                return;
            }
            if ("Compra".equals(cbCategoria.getValue())) {
                if (txtNumeroFactura.getText().trim().isEmpty()) {
                    mostrarError("Ingrese el número de factura");
                    return;
                }
                if (items.isEmpty()) {
                    mostrarError("Agregue al menos un ítem");
                    return;
                }
            } else {
                if (txtMonto.getText().trim().isEmpty()) {
                    mostrarError("Por favor ingrese el monto");
                    return;
                }
            }

            Egreso egreso = new Egreso();
            egreso.setDescripcion(txtDescripcion.getText().trim());
            egreso.setMonto(Double.parseDouble(txtMonto.getText().trim()));
            egreso.setFecha(LocalDate.now());
            egreso.setCategoria(cbCategoria.getValue());
            egreso.setNumeroFactura(txtNumeroFactura.getText().trim());
            egreso.setRutaAdjunto(archivoAdjunto != null ? archivoAdjunto.getPath() : null);

            int egresoId = DatabaseUtil.insertarEgreso(egreso);

            if ("Compra".equals(cbCategoria.getValue())) {
                for (ItemCompra item : items) {
                    DatabaseUtil.insertarEgresoDetalle(egresoId, item.getProducto().getId(),
                            item.getCantidad(), item.getCosto());
                    DatabaseUtil.actualizarProducto(item.getProducto().getId(),
                            item.getProducto().getPrecio(), item.getCantidad());
                    int nuevoSaldo = item.getProducto().getStock() + item.getCantidad();
                    DatabaseUtil.insertMovimientoInventario(
                            item.getProducto().getId(),
                            "ENTRADA",
                            item.getCantidad(),
                            "Compra",
                            SessionManager.getUsuarioActual().getNombre(),
                            LocalDateTime.now(),
                            nuevoSaldo
                    );
                    AuditoriaUtil.registrar(SessionManager.getUsuarioActual().getNombre(),
                            "UPDATE", "PRODUCTO", item.getProducto().getId(),
                            "+" + item.getCantidad() + " unidades");
                }
            }

            AuditoriaUtil.registrar(SessionManager.getUsuarioActual().getNombre(), "CREATE", "EGRESO", null, egreso.getDescripcion());
            EventBus.fireEvent(EventBus.EventType.EGRESO_REGISTRADO);
            cerrarVentana();
        } catch (NumberFormatException e) {
            mostrarError("El monto debe ser un número válido");
        } catch (Exception e) {
            mostrarError("Error al registrar el egreso: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancelar() {
        cerrarVentana();
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de Validación");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private static class ItemCompra {
        private final Producto producto;
        private final int cantidad;
        private final double costo;

        public ItemCompra(Producto producto, int cantidad, double costo) {
            this.producto = producto;
            this.cantidad = cantidad;
            this.costo = costo;
        }

        public Producto getProducto() {
            return producto;
        }

        public int getCantidad() {
            return cantidad;
        }

        public double getCosto() {
            return costo;
        }
    }
}
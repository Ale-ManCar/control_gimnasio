package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Pair;
import javafx.util.StringConverter;
import models.Egreso;
import models.Producto;
import util.DatabaseUtil;
import util.EventBus;
import util.SessionManager;
import util.AuditoriaUtil;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import javafx.collections.ObservableList;

public class RegistroEgresoController implements Initializable {

    @FXML private TextField txtMonto;
    @FXML private TextArea txtDescripcion;
    @FXML private ComboBox<String> cbCategoria;
    @FXML private Button btnRegistrar;
    @FXML private Button btnCancelar;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!SessionManager.isAdmin()) {
            btnRegistrar.setDisable(true);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Permiso denegado", javafx.scene.control.ButtonType.OK);
            alert.showAndWait();
        }
        // Configurar categorías
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

        // Configurar conversión a mayúsculas
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getText();
            if (!text.isEmpty()) {
                change.setText(text.toUpperCase());
            }
            return change;
        };
        txtDescripcion.setTextFormatter(new TextFormatter<>(filter));

        // Configurar TextArea para salto de línea automático
        txtDescripcion.setWrapText(true);
        txtDescripcion.setPrefRowCount(4);

        // Estilizar botones
        btnRegistrar.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-padding: 10 20; -fx-font-size: 14px;");
        btnCancelar.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; "
                + "-fx-padding: 10 20; -fx-font-size: 14px;");
    }

    @FXML
    private void handleRegistrar() {
        try {
            // Validar campos
            if (txtMonto.getText().trim().isEmpty()) {
                mostrarError("Por favor ingrese el monto");
                return;
            }

            if (txtDescripcion.getText().trim().isEmpty()) {
                mostrarError("Por favor ingrese una descripción");
                return;
            }

            Producto productoSeleccionado = null;
            int cantidad = 0;
            if ("Compra".equals(cbCategoria.getValue())) {
                Pair<Producto, Integer> seleccion = mostrarDialogoCompra();
                if (seleccion == null) {
                    mostrarError("Debe seleccionar un producto y cantidad");
                    return;
                }
                productoSeleccionado = seleccion.getKey();
                cantidad = seleccion.getValue();
            }

            // Crear y registrar egreso
            Egreso egreso = new Egreso();
            egreso.setDescripcion(txtDescripcion.getText().trim());
            egreso.setMonto(Double.parseDouble(txtMonto.getText().trim()));
            egreso.setFecha(LocalDate.now());
            egreso.setCategoria(cbCategoria.getValue());

            DatabaseUtil.insertarEgreso(egreso);

            if (productoSeleccionado != null) {
                DatabaseUtil.actualizarProducto(productoSeleccionado.getId(), productoSeleccionado.getPrecio(), cantidad);
                int nuevoSaldo = productoSeleccionado.getStock() + cantidad;
                DatabaseUtil.insertMovimientoInventario(
                        productoSeleccionado.getId(),
                        "ENTRADA",
                        cantidad,
                        "Compra",
                        SessionManager.getUsuarioActual().getNombre(),
                        LocalDateTime.now(),
                        nuevoSaldo
                );
                AuditoriaUtil.registrar(SessionManager.getUsuarioActual().getNombre(),
                        "UPDATE", "PRODUCTO", productoSeleccionado.getId(),
                        "+" + cantidad + " unidades");
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

    private Pair<Producto, Integer> mostrarDialogoCompra() {
        try {
            ObservableList<Producto> productos = DatabaseUtil.getProductos();
            if (productos.isEmpty()) {
                mostrarError("No hay productos registrados");
                return null;
            }

            Dialog<Pair<Producto, Integer>> dialog = new Dialog<>();
            dialog.setTitle("Seleccionar producto");
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

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.add(new Label("Producto:"), 0, 0);
            grid.add(cbProducto, 1, 0);
            grid.add(new Label("Cantidad:"), 0, 1);
            grid.add(txtCantidad, 1, 1);
            dialog.getDialogPane().setContent(grid);

            Node aceptar = dialog.getDialogPane().lookupButton(btnAceptar);
            aceptar.setDisable(true);
            cbProducto.valueProperty().addListener((obs, oldVal, newVal) -> {
                aceptar.setDisable(newVal == null || txtCantidad.getText().trim().isEmpty());
            });
            txtCantidad.textProperty().addListener((obs, oldVal, newVal) -> {
                aceptar.setDisable(cbProducto.getValue() == null || newVal.trim().isEmpty());
            });

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == btnAceptar) {
                    int cant = Integer.parseInt(txtCantidad.getText().trim());
                    return new Pair<>(cbProducto.getValue(), cant);
                }
                return null;
            });

            Optional<Pair<Producto, Integer>> result = dialog.showAndWait();
            return result.orElse(null);
        } catch (Exception e) {
            mostrarError("Error al cargar productos");
            return null;
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
}
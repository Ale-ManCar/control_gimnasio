package controllers;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import models.Producto;
import models.VentaItem;
import util.DatabaseUtil;
import util.EventBus;
import util.AuditoriaUtil;
import util.SessionManager;
import models.Role;

import java.util.Optional;

public class InventarioController {

    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, String> colProducto;
    @FXML private TableColumn<Producto, Integer> colUnidades;
    @FXML private TableColumn<Producto, Double> colPrecio;
    @FXML private ComboBox<Producto> cbProductos;
    @FXML private Label lblCantidad;
    @FXML private Label lblTotalVenta;
    @FXML private Label lblTotalCarrito;

    @FXML private TableView<VentaItem> tablaCarrito;
    @FXML private TableColumn<VentaItem, String> colCarritoNombre;
    @FXML private TableColumn<VentaItem, Integer> colCarritoUnidades;
    @FXML private TableColumn<VentaItem, Double> colCarritoPrecio;
    @FXML private TableColumn<VentaItem, Double> colCarritoTotal;

    private ObservableList<Producto> listaProductos = FXCollections.observableArrayList();
    private ObservableList<VentaItem> carrito = FXCollections.observableArrayList();

    private int cantidadActual = 1;

    @FXML
    public void initialize() {
        if (!SessionManager.tienePermiso(Role.RECEPCIONISTA) && !SessionManager.tienePermiso(Role.ADMIN)) {
            return;
        }
        configurarTabla();
        configurarTablaCarrito();
        cargarProductos();
        configurarComboBox();
        actualizarLabelCantidad();
        actualizarTotalCarrito();
    }

    private void configurarTabla() {
        colProducto.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colUnidades.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));

        colProducto.prefWidthProperty().bind(tablaProductos.widthProperty().multiply(0.55));
        colUnidades.prefWidthProperty().bind(tablaProductos.widthProperty().multiply(0.20));
        colPrecio.prefWidthProperty().bind(tablaProductos.widthProperty().multiply(0.20));

        colPrecio.setCellFactory(column -> new TableCell<Producto, Double>() {
            @Override
            protected void updateItem(Double precio, boolean empty) {
                super.updateItem(precio, empty);
                if (empty || precio == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", precio));
                }
            }
        });
    }

    private void configurarTablaCarrito() {
        colCarritoNombre.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getNombreProducto()));
        colCarritoUnidades.setCellValueFactory(new PropertyValueFactory<>("unidades"));
        colCarritoPrecio.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getPrecioUnitario()));
        colCarritoTotal.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getTotal()));

        colCarritoNombre.prefWidthProperty().bind(tablaCarrito.widthProperty().multiply(0.35));
        colCarritoUnidades.prefWidthProperty().bind(tablaCarrito.widthProperty().multiply(0.20));
        colCarritoPrecio.prefWidthProperty().bind(tablaCarrito.widthProperty().multiply(0.20));
        colCarritoTotal.prefWidthProperty().bind(tablaCarrito.widthProperty().multiply(0.20));

        colCarritoNombre.setCellFactory(column -> new TableCell<VentaItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        colCarritoUnidades.setCellFactory(column -> new TableCell<VentaItem, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.toString());
                    setAlignment(Pos.CENTER);
                }
            }
        });

        colCarritoPrecio.setCellFactory(column -> new TableCell<VentaItem, Double>() {
            @Override
            protected void updateItem(Double precio, boolean empty) {
                super.updateItem(precio, empty);
                if (empty || precio == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(String.format("$%.2f", precio));
                    setAlignment(Pos.CENTER);
                }
            }
        });

        colCarritoTotal.setCellFactory(column -> new TableCell<VentaItem, Double>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                if (empty || total == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(String.format("$%.2f", total));
                    setAlignment(Pos.CENTER);
                }
            }
        });

        tablaCarrito.setItems(carrito);
    }

    private void cargarProductos() {
        try {
            listaProductos.setAll(DatabaseUtil.getProductos());
            tablaProductos.setItems(listaProductos);
            cbProductos.setItems(listaProductos);
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudieron cargar los productos");
            e.printStackTrace();
        }
    }

    private void configurarComboBox() {
        cbProductos.setConverter(new StringConverter<Producto>() {
            @Override
            public String toString(Producto producto) {
                return producto == null ? "" : producto.getNombre();
            }

            @Override
            public Producto fromString(String string) {
                return null;
            }
        });

        cbProductos.setCellFactory(param -> new ListCell<Producto>() {
            @Override
            protected void updateItem(Producto producto, boolean empty) {
                super.updateItem(producto, empty);
                if (empty || producto == null) {
                    setText(null);
                } else {
                    setText(producto.getNombre());
                }
            }
        });

        cbProductos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                cantidadActual = 1;
                actualizarLabelCantidad();
                calcularTotalVenta();
            }
        });
    }

    @FXML
    private void aumentarCantidad() {
        Producto producto = cbProductos.getSelectionModel().getSelectedItem();
        if (producto == null) return;

        if (cantidadActual < producto.getStock()) {
            cantidadActual++;
            actualizarLabelCantidad();
            calcularTotalVenta();
        } else {
            mostrarAlerta("Advertencia", "No puedes vender más de lo que hay en stock");
        }
    }

    @FXML
    private void disminuirCantidad() {
        if (cantidadActual > 1) {
            cantidadActual--;
            actualizarLabelCantidad();
            calcularTotalVenta();
        }
    }

    private void actualizarLabelCantidad() {
        if (lblCantidad != null) {
            lblCantidad.setText(String.valueOf(cantidadActual));
        }
    }

    @FXML
    private void calcularTotalVenta() {
        Producto productoSeleccionado = cbProductos.getSelectionModel().getSelectedItem();
        if (productoSeleccionado == null) {
            lblTotalVenta.setText("");
            return;
        }
        double total = cantidadActual * productoSeleccionado.getPrecio();
        lblTotalVenta.setText(String.format("$%.2f", total));
    }

    @FXML
    private void handleAgregarAlCarrito() {
        Producto producto = cbProductos.getSelectionModel().getSelectedItem();
        if (producto == null) {
            mostrarAlerta("Error", "Seleccione un producto para agregar al carrito");
            return;
        }

        if (cantidadActual <= 0) {
            mostrarAlerta("Error", "Las unidades deben ser mayor que cero");
            return;
        }

        if (cantidadActual > producto.getStock()) {
            mostrarAlerta("Error", "Stock insuficiente para agregar al carrito");
            return;
        }

        Optional<VentaItem> itemExistente = carrito.stream()
                .filter(item -> item.getProducto().getId() == producto.getId())
                .findFirst();

        if (itemExistente.isPresent()) {
            VentaItem item = itemExistente.get();
            int nuevaCantidad = item.getUnidades() + cantidadActual;

            if (nuevaCantidad > producto.getStock()) {
                mostrarAlerta("Error", "Stock insuficiente para agregar esa cantidad al carrito");
                return;
            }

            item.setUnidades(nuevaCantidad);
        } else {
            carrito.add(new VentaItem(producto, cantidadActual));
        }

        tablaCarrito.refresh();
        actualizarTotalCarrito();
        limpiarVenta();
    }

    @FXML
    private void handleConfirmarVenta() {
        if (carrito.isEmpty()) {
            mostrarAlerta("Error", "No hay productos en el carrito");
            return;
        }

        try {
            double totalVenta = carrito.stream().mapToDouble(VentaItem::getTotal).sum();

            // Registrar la venta en la base de datos
            DatabaseUtil.registrarVenta(totalVenta);

            for (VentaItem item : carrito) {
                DatabaseUtil.registrarSalidaProducto(item.getProducto().getId(), item.getUnidades());
            }

            AuditoriaUtil.registrarAccion(
                    SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 0,
                    "Venta",
                    "Total: " + totalVenta
            );

            carrito.clear();
            cargarProductos();
            actualizarTotalCarrito();
            mostrarDialogoVentaExitosa();

            EventBus.fireVentaRealizadaEvent();

        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo completar la venta");
            e.printStackTrace();
        }
    }

    private void mostrarDialogoVentaExitosa() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Venta Exitosa");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        VBox contenido = new VBox(15);
        contenido.setPadding(new Insets(20));
        contenido.setAlignment(Pos.CENTER);

        Label icono = new Label("\u2714");
        icono.setStyle("-fx-font-size: 48px; -fx-text-fill: #4CAF50;");

        Label titulo = new Label("¡Venta completada correctamente!");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        contenido.getChildren().addAll(icono, titulo);
        dialog.getDialogPane().setContent(contenido);

        dialog.showAndWait();
    }

    private void actualizarTotalCarrito() {
        double totalVenta = carrito.stream().mapToDouble(VentaItem::getTotal).sum();
        lblTotalCarrito.setText(String.format("$%.2f", totalVenta));
    }

    private void limpiarVenta() {
        cbProductos.getSelectionModel().clearSelection();
        cantidadActual = 1;
        actualizarLabelCantidad();
        lblTotalVenta.setText("");
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
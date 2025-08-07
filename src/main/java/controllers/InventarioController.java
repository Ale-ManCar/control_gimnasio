package controllers;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import models.Producto;
import models.VentaItem;
import util.DatabaseUtil;

import java.sql.SQLException;
import java.util.Optional;

public class InventarioController {

    @FXML private TextField txtNombreProducto;
    @FXML private TextField txtStock;
    @FXML private TextField txtPrecio;
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

        colCarritoPrecio.setCellFactory(column -> new TableCell<VentaItem, Double>() {
            @Override
            protected void updateItem(Double precio, boolean empty) {
                super.updateItem(precio, empty);
                setText(empty || precio == null ? null : String.format("$%.2f", precio));
            }
        });

        colCarritoTotal.setCellFactory(column -> new TableCell<VentaItem, Double>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                setText(empty || total == null ? null : String.format("$%.2f", total));
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

        // Verificar si el producto ya está en el carrito para sumar cantidades
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
            for (VentaItem item : carrito) {
                DatabaseUtil.actualizarStockProducto(item.getProducto().getId(), item.getUnidades());
            }

            carrito.clear();
            cargarProductos();
            actualizarTotalCarrito();

            // Mostrar diálogo personalizado con diseño mejorado
            mostrarDialogoVentaExitosa();

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

        Label icono = new Label("\u2714"); // ✔ check mark unicode
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

    private void limpiarFormulario() {
        txtNombreProducto.clear();
        txtStock.clear();
        txtPrecio.clear();
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

    @FXML
    private void handleIngresarProducto() {
        // Crear diálogo personalizado
        Dialog<Producto> dialog = new Dialog<>();
        dialog.setTitle("Ingresar Nuevo Producto");
        dialog.setHeaderText("Complete los detalles del producto");

        // Configurar botones
        ButtonType btnIngresar = new ButtonType("Ingresar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnIngresar, ButtonType.CANCEL);

        // Crear campos del formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre del producto");
        TextField txtStock = new TextField();
        txtStock.setPromptText("Cantidad en stock");
        ComboBox<String> cbTipo = new ComboBox<>();
        cbTipo.getItems().addAll("PACA", "KG", "LB");
        cbTipo.setValue("KG"); // Valor por defecto

        TextField txtUnidadesPorPaca = new TextField("0");
        txtUnidadesPorPaca.setPromptText("Unidades por paca");
        TextField txtPrecioCompra = new TextField();
        txtPrecioCompra.setPromptText("Precio de compra");
        TextField txtPrecioVenta = new TextField();
        txtPrecioVenta.setPromptText("Precio de venta");

        // Etiquetas para mostrar ganancias/pérdidas
        Label lblResultado = new Label();
        lblResultado.setStyle("-fx-font-weight: bold;");

        // Añadir campos al grid
        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(txtNombre, 1, 0);
        grid.add(new Label("Stock:"), 0, 1);
        grid.add(txtStock, 1, 1);
        grid.add(new Label("Tipo:"), 0, 2);
        grid.add(cbTipo, 1, 2);
        grid.add(new Label("Unidades por paca:"), 0, 3);
        grid.add(txtUnidadesPorPaca, 1, 3);
        grid.add(new Label("Precio Compra:"), 0, 4);
        grid.add(txtPrecioCompra, 1, 4);
        grid.add(new Label("Precio Venta:"), 0, 5);
        grid.add(txtPrecioVenta, 1, 5);
        grid.add(lblResultado, 0, 6, 2, 1); // Ocupa 2 columnas

        // Mostrar/ocultar campo de unidades según tipo
        txtUnidadesPorPaca.setVisible(false);
        cbTipo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean esPaca = "PACA".equals(newVal);
            txtUnidadesPorPaca.setVisible(esPaca);
        });

        // Listener para calcular ganancias en tiempo real
        ChangeListener<String> calculador = (observable, oldValue, newValue) -> {
            try {
                String tipo = cbTipo.getValue();
                double precioCompra = Double.parseDouble(txtPrecioCompra.getText());
                double precioVenta = Double.parseDouble(txtPrecioVenta.getText());
                int stock = Integer.parseInt(txtStock.getText());

                double gananciaUnidad = 0;
                String detalle = "";

                if ("PACA".equals(tipo)) {
                    int unidadesPorPaca = Integer.parseInt(txtUnidadesPorPaca.getText());
                    double costoPorUnidad = precioCompra / unidadesPorPaca;
                    gananciaUnidad = precioVenta - costoPorUnidad;
                    detalle = String.format("(Costo por unidad: $%.2f)", costoPorUnidad);
                } else {
                    gananciaUnidad = precioVenta - precioCompra;
                    detalle = String.format("(Costo por %s: $%.2f)", tipo, precioCompra);
                }

                double gananciaTotal = gananciaUnidad * stock;

                if (gananciaUnidad >= 0) {
                    lblResultado.setText(String.format("GANANCIA: $%.2f por unidad | $%.2f total %s",
                            gananciaUnidad, gananciaTotal, detalle));
                    lblResultado.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                } else {
                    lblResultado.setText(String.format("PÉRDIDA: $%.2f por unidad | $%.2f total %s",
                            Math.abs(gananciaUnidad), Math.abs(gananciaTotal), detalle));
                    lblResultado.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                }
            } catch (NumberFormatException e) {
                lblResultado.setText("Complete todos los campos numéricos");
                lblResultado.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
            }
        };

        // Añadir listeners a los campos relevantes
        txtPrecioCompra.textProperty().addListener(calculador);
        txtPrecioVenta.textProperty().addListener(calculador);
        txtStock.textProperty().addListener(calculador);
        txtUnidadesPorPaca.textProperty().addListener(calculador);
        cbTipo.valueProperty().addListener((obs, oldVal, newVal) -> calculador.changed(null, null, null));

        dialog.getDialogPane().setContent(grid);

        // Convertir resultado a objeto Producto
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnIngresar) {
                try {
                    String nombre = txtNombre.getText().trim();
                    int stock = Integer.parseInt(txtStock.getText().trim());
                    String tipo = cbTipo.getValue();
                    double precioCompra = Double.parseDouble(txtPrecioCompra.getText().trim());
                    double precioVenta = Double.parseDouble(txtPrecioVenta.getText().trim());
                    int unidadesPorPaca = 0;

                    if ("PACA".equals(tipo)) {
                        unidadesPorPaca = Integer.parseInt(txtUnidadesPorPaca.getText().trim());
                    }

                    if (nombre.isEmpty()) {
                        mostrarAlerta("Error", "El nombre del producto es requerido");
                        return null;
                    }

                    return new Producto(nombre, stock, precioVenta, tipo, precioCompra, unidadesPorPaca);
                } catch (NumberFormatException e) {
                    mostrarAlerta("Error", "Stock, Precio compra y Precio venta deben ser valores numéricos");
                    return null;
                }
            }
            return null;
        });

        // Procesar resultado
        Optional<Producto> resultado = dialog.showAndWait();
        resultado.ifPresent(producto -> {
            try {
                DatabaseUtil.insertarProducto(producto);
                cargarProductos();
                mostrarAlerta("Éxito", "Producto registrado correctamente");
            } catch (Exception e) {
                mostrarAlerta("Error", "No se pudo registrar el producto");
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleEditarProducto() {
        Producto seleccionado = tablaProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Advertencia", "Debe seleccionar un producto para editar.");
            return;
        }

        Dialog<Producto> dialog = new Dialog<>();
        dialog.setTitle("Editar Producto");
        dialog.setHeaderText(null);

        ButtonType botonActualizar = new ButtonType("Actualizar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(botonActualizar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        Label lblPrecio = new Label("Precio nuevo:");
        TextField tfPrecio = new TextField(String.format("%.2f", seleccionado.getPrecio()));

        Label lblStockActual = new Label("Stock actual:");
        Label lblStockValor = new Label(String.valueOf(seleccionado.getStock()));
        lblStockValor.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        Label lblSumar = new Label("Unidades a sumar:");
        TextField tfStock = new TextField("0");

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        grid.add(lblPrecio, 0, 0);
        grid.add(tfPrecio, 1, 0);
        grid.add(lblStockActual, 0, 1);
        grid.add(lblStockValor, 1, 1);
        grid.add(lblSumar, 0, 2);
        grid.add(tfStock, 1, 2);
        grid.add(lblError, 0, 3, 2, 1);

        dialog.getDialogPane().setContent(grid);

        Node btnActualizar = dialog.getDialogPane().lookupButton(botonActualizar);
        btnActualizar.setDisable(true);

        ChangeListener<String> validador = (obs, oldVal, newVal) -> {
            boolean precioOk = esPrecioValido(tfPrecio.getText());
            boolean stockOk = esStockValido(tfStock.getText());

            if (!precioOk) {
                lblError.setText("Precio inválido (debe ser número >= 0)");
            } else if (!stockOk) {
                lblError.setText("Stock inválido (debe ser entero >= 0)");
            } else {
                lblError.setText("");
            }

            btnActualizar.setDisable(!(precioOk && stockOk));
        };

        tfPrecio.textProperty().addListener(validador);
        tfStock.textProperty().addListener(validador);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == botonActualizar) {
                double nuevoPrecio = Double.parseDouble(tfPrecio.getText());
                int unidadesSumar = Integer.parseInt(tfStock.getText());

                Producto editado = new Producto();
                editado.setId(seleccionado.getId());
                editado.setPrecio(nuevoPrecio);
                editado.setStock(unidadesSumar);
                return editado;
            }
            return null;
        });

        Optional<Producto> resultado = dialog.showAndWait();
        resultado.ifPresent(prod -> {
            try {
                DatabaseUtil.actualizarProducto(prod.getId(), prod.getPrecio(), prod.getStock());
                mostrarAlerta("Éxito", "Producto actualizado correctamente.");
                cargarProductos();
            } catch (Exception e) {
                mostrarAlerta("Error", "No se pudo actualizar el producto.");
                e.printStackTrace();
            }
        });
    }

    private boolean esPrecioValido(String texto) {
        try {
            double val = Double.parseDouble(texto);
            return val >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean esStockValido(String texto) {
        try {
            int val = Integer.parseInt(texto);
            return val >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @FXML
    private void handleEliminarProducto() {
        Producto seleccionado = tablaProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Advertencia", "Debe seleccionar un producto para eliminar.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Confirmar Eliminación");
        dialog.setHeaderText(null);

        VBox contenido = new VBox(15);
        contenido.setPadding(new Insets(25));
        contenido.setAlignment(Pos.CENTER_LEFT);

        Label titulo = new Label("¿Estás seguro que deseas eliminar este producto?");
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label nombreProducto = new Label("» " + seleccionado.getNombre());
        nombreProducto.setStyle("-fx-font-size: 14px; -fx-text-fill: #d32f2f; -fx-font-weight: bold;");

        Label nota = new Label("Esta acción no se puede deshacer.");
        nota.setStyle("-fx-font-size: 13px; -fx-text-fill: #757575;");

        contenido.getChildren().addAll(titulo, nombreProducto, nota);
        dialog.getDialogPane().setContent(contenido);

        ButtonType btnEliminar = new ButtonType("Eliminar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnEliminar, btnCancelar);

        Platform.runLater(() -> {
            Button eliminarBtn = (Button) dialog.getDialogPane().lookupButton(btnEliminar);
            eliminarBtn.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold;");
            eliminarBtn.setDefaultButton(false);

            Button cancelarBtn = (Button) dialog.getDialogPane().lookupButton(btnCancelar);
            cancelarBtn.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: black;");
        });

        Optional<ButtonType> resultado = dialog.showAndWait();
        if (resultado.isPresent() && resultado.get() == btnEliminar) {
            try {
                eliminarProducto(seleccionado.getId());
                mostrarAlerta("Éxito", "Producto eliminado correctamente.");
                cargarProductos();
            } catch (Exception e) {
                mostrarAlerta("Error", "No se pudo eliminar el producto.");
                e.printStackTrace();
            }
        }
    }

    private void eliminarProducto(int idProducto) throws SQLException {
        String sql = "DELETE FROM productos WHERE id = ?";
        DatabaseUtil.executeUpdate(sql, idProducto);
    }
}
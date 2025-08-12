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
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Paint;
import javafx.util.StringConverter;
import models.Producto;
import models.VentaItem;
import org.kordamp.ikonli.javafx.FontIcon;
import util.DatabaseUtil;
import util.EventBus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
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
                DatabaseUtil.actualizarStockProducto(item.getProducto().getId(), item.getUnidades());
            }

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

    @FXML
    private void handleIngresarProducto() {
        Dialog<Producto> dialog = new Dialog<>();
        dialog.setTitle("Ingresar Nuevo Producto");
        dialog.setHeaderText(null);

        // Estilo para el diálogo
        dialog.getDialogPane().setStyle(
                "-fx-background-color: linear-gradient(to bottom, #2c3e50, #1a1a2e);" +
                        "-fx-padding: 20;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-color: rgba(255,255,255,0.1);" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 0);");
        dialog.getDialogPane().getScene().getWindow().setOnShown(e ->
                Platform.runLater(() -> dialog.getDialogPane().requestLayout()));

        ButtonType btnIngresar = new ButtonType("Ingresar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnIngresar, ButtonType.CANCEL);

        // Botón personalizado
        Button ingresarButton = (Button) dialog.getDialogPane().lookupButton(btnIngresar);

        ingresarButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #4CAF50, #2ECC71);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 25;" +
                        "-fx-padding: 8 20;");

        Button cancelarButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelarButton.setStyle(
                "-fx-background-color: #6C757D;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 25;" +
                        "-fx-padding: 8 20;");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1);" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 8, 0, 0, 0);");
        grid.setPrefWidth(500);

        VBox contenedor = new VBox(15);
        contenedor.setAlignment(Pos.TOP_CENTER);
        Label titulo = new Label("Ingresar Nuevo Producto");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subtitulo = new Label("Complete los detalles del producto");
        subtitulo.setStyle("-fx-text-fill: #bdc3c7; -fx-padding: 0 0 10 0;");
        contenedor.getChildren().addAll(titulo, subtitulo, grid);

        // Campos del formulario
        TextField txtNombre = createStyledTextField("Nombre del producto", true);
        ComboBox<String> cbTipo = createStyledComboBox("PACA", "KG", "LB");
        TextField txtPesoTotal = createStyledTextField("Peso total (kg/lb)", false);
        TextField txtPesoScoop = createStyledTextField("Peso por scoop (g)", false);
        TextField txtPrecioCompra = createStyledTextField("Precio compra (envase)", true);
        TextField txtPrecioVenta = createStyledTextField("Precio venta", true);
        TextField txtUnidadesPorPaca = createStyledTextField("Unidades por paca", false);

        // Labels para mostrar resultados
        Label lblStockCalculado = new Label();
        lblStockCalculado.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");

        Label lblResultado = new Label();
        lblResultado.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 0 0 0;");
        lblResultado.setWrapText(true);
        lblResultado.setMaxWidth(Double.MAX_VALUE);

        // Iconos para cada campo
        Label iconNombre = createIconLabel("fas-box", "#3498db"); // Caja
        Label iconTipo = createIconLabel("fas-cube", "#e74c3c");   // Tipo
        Label iconPrecioCompra = createIconLabel("fas-money-bill-wave", "#2ecc71"); // Precio de compra
        Label iconPrecioVenta = createIconLabel("fas-tag", "#f39c12"); // Precio de venta
        Label iconUnidades = createIconLabel("fas-cubes", "#9b59b6"); // Unidades
        Label iconPesoTotal = createIconLabel("fas-weight", "#1abc9c"); // Peso total
        Label iconScoop = createIconLabel("fas-utensil-spoon", "#e67e22"); // Peso por scoop

        // Organización de los campos en el grid
        grid.add(iconNombre, 0, 0);
        grid.add(txtNombre, 1, 0, 2, 1);

        grid.add(iconTipo, 0, 1);
        grid.add(cbTipo, 1, 1, 2, 1);

        grid.add(iconPrecioCompra, 0, 2);
        grid.add(txtPrecioCompra, 1, 2, 2, 1);

        grid.add(iconPrecioVenta, 0, 3);
        grid.add(txtPrecioVenta, 1, 3, 2, 1);

        grid.add(iconUnidades, 0, 4);
        grid.add(txtUnidadesPorPaca, 1, 4, 2, 1);

        grid.add(iconPesoTotal, 0, 5);
        grid.add(txtPesoTotal, 1, 5, 2, 1);

        grid.add(iconScoop, 0, 6);
        grid.add(txtPesoScoop, 1, 6, 2, 1);

        grid.add(lblStockCalculado, 0, 7, 3, 1);
        grid.add(lblResultado, 0, 8, 3, 1);

        // Configuración inicial de visibilidad
        txtPesoTotal.setVisible(false);
        txtPesoScoop.setVisible(false);
        iconPesoTotal.setVisible(false);
        iconScoop.setVisible(false);

        txtUnidadesPorPaca.setVisible(true);
        iconUnidades.setVisible(true);

        // Listener para cambiar visibilidad según el tipo
        cbTipo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean esPaca = "PACA".equals(newVal);
            boolean esSuplemento = "KG".equals(newVal) || "LB".equals(newVal);

            // Actualizar visibilidad
            txtUnidadesPorPaca.setVisible(esPaca);
            iconUnidades.setVisible(esPaca);
            txtPesoTotal.setVisible(esSuplemento);
            txtPesoScoop.setVisible(esSuplemento);
            iconPesoTotal.setVisible(esSuplemento);
            iconScoop.setVisible(esSuplemento);

            // Actualizar placeholders
            txtPrecioVenta.setPromptText(esSuplemento ?
                    "Precio por scoop" : "Precio por unidad");

            // Limpiar campos no relevantes
            if (esPaca) {
                txtPesoTotal.clear();
                txtPesoScoop.clear();
            } else {
                txtUnidadesPorPaca.clear();
            }

            // Actualizar texto informativo
            lblStockCalculado.setText(esPaca ?
                    "Ingrese unidades por paca para calcular ganancias" :
                    "Complete peso total y peso por scoop para calcular ganancias");
        });

        // Listener para cálculos en tiempo real
        ChangeListener<String> calculador = (observable, oldValue, newValue) -> {
            try {
                String tipo = cbTipo.getValue();
                double precioCompra = Double.parseDouble(txtPrecioCompra.getText());
                double precioVenta = Double.parseDouble(txtPrecioVenta.getText());

                int stock = 0;
                String detalle = "";
                double gananciaUnidad = 0;
                String unidadTexto = "";

                if ("PACA".equals(tipo)) {
                    int unidadesPorPaca = Integer.parseInt(txtUnidadesPorPaca.getText());
                    double costoPorUnidad = precioCompra / unidadesPorPaca;
                    gananciaUnidad = precioVenta - costoPorUnidad;
                    stock = unidadesPorPaca;
                    detalle = String.format("(Costo por unidad: $%.2f)", costoPorUnidad);
                    unidadTexto = "unidad";
                    lblStockCalculado.setText(String.format("Unidades disponibles: %d | Costo/unidad: $%.2f",
                            stock, costoPorUnidad));
                }
                else if ("KG".equals(tipo) || "LB".equals(tipo)) {
                    double pesoTotal = Double.parseDouble(txtPesoTotal.getText());
                    double pesoScoop = Double.parseDouble(txtPesoScoop.getText());

                    if ("KG".equals(tipo)) {
                        stock = (int) ((pesoTotal * 1000) / pesoScoop); // KG a gramos
                    } else {
                        stock = (int) ((pesoTotal * 453.592) / pesoScoop); // LB a gramos
                    }

                    double costoPorScoop = precioCompra / stock;
                    gananciaUnidad = precioVenta - costoPorScoop;
                    detalle = String.format("(Costo por scoop: $%.2f)", costoPorScoop);
                    unidadTexto = "scoop";
                    lblStockCalculado.setText(String.format("Servicios disponibles: %d | Costo/scoop: $%.2f",
                            stock, costoPorScoop));
                }

                double gananciaTotal = gananciaUnidad * stock;

                if (gananciaUnidad >= 0) {
                    lblResultado.setText(String.format("▲ GANANCIA: $%.2f/%s | TOTAL: $%.2f",
                            gananciaUnidad, unidadTexto, gananciaTotal));
                    lblResultado.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                } else {
                    lblResultado.setText(String.format("▼ $%.2f/%s | TOTAL: $%.2f",
                            Math.abs(gananciaUnidad), unidadTexto, Math.abs(gananciaTotal)));
                    lblResultado.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            } catch (NumberFormatException e) {
                lblResultado.setText("Complete los campos requeridos");
                lblResultado.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
            }
        };

        // Añadir listeners
        txtPrecioCompra.textProperty().addListener(calculador);
        txtPrecioVenta.textProperty().addListener(calculador);
        txtPesoTotal.textProperty().addListener(calculador);
        txtPesoScoop.textProperty().addListener(calculador);
        txtUnidadesPorPaca.textProperty().addListener(calculador);
        cbTipo.valueProperty().addListener((obs, oldVal, newVal) -> calculador.changed(null, null, null));

        // Mensaje inicial
        lblStockCalculado.setText("Seleccione tipo de producto y complete los campos");
        lblStockCalculado.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");

        dialog.getDialogPane().setContent(contenedor);

        // Convertir resultado a objeto Producto
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnIngresar) {
                try {
                    String nombre = txtNombre.getText().trim();
                    String tipo = cbTipo.getValue();
                    BigDecimal bdPrecioCompra = new BigDecimal(txtPrecioCompra.getText().trim());
                    BigDecimal bdPrecioVenta = new BigDecimal(txtPrecioVenta.getText().trim());
                    int stock = 0;
                    int unidadesPorPaca = 0;
                    BigDecimal bdPesoTotal = BigDecimal.ZERO;
                    BigDecimal bdPesoScoop = BigDecimal.ZERO;

                    if (nombre.isEmpty()) {
                        mostrarAlerta("Error", "El nombre del producto es requerido");
                        return null;
                    }

                    if ("PACA".equals(tipo)) {
                        unidadesPorPaca = Integer.parseInt(txtUnidadesPorPaca.getText().trim());
                        stock = unidadesPorPaca;
                    }
                    else if ("KG".equals(tipo) || "LB".equals(tipo)) {
                        bdPesoTotal = new BigDecimal(txtPesoTotal.getText().trim());
                        bdPesoScoop = new BigDecimal(txtPesoScoop.getText().trim());

                        if ("KG".equals(tipo)) {
                            BigDecimal gramosTotal = bdPesoTotal.multiply(BigDecimal.valueOf(1000));
                            BigDecimal scoops = gramosTotal.divide(bdPesoScoop, 0, RoundingMode.DOWN);
                            stock = scoops.intValue();
                        } else {
                            BigDecimal gramosTotal = bdPesoTotal.multiply(BigDecimal.valueOf(453.592));
                            BigDecimal scoops = gramosTotal.divide(bdPesoScoop, 0, RoundingMode.DOWN);
                            stock = scoops.intValue();
                        }
                    }

                    Producto nuevo = new Producto();
                    nuevo.setNombre(nombre);
                    nuevo.setTipo(tipo);
                    nuevo.setPrecioCompra(bdPrecioCompra.doubleValue());
                    nuevo.setPrecio(bdPrecioVenta.doubleValue());
                    nuevo.setStock(stock);
                    nuevo.setUnidadesPorPaca(unidadesPorPaca);
                    nuevo.setPesoTotal(bdPesoTotal.doubleValue());
                    nuevo.setPesoScoop(bdPesoScoop.doubleValue());

                    return nuevo;
                } catch (NumberFormatException e) {
                    mostrarAlerta("Error", "Valores numéricos inválidos: " + e.getMessage());
                    return null;
                } catch (ArithmeticException e) {
                    mostrarAlerta("Error", "Error en cálculo matemático: " + e.getMessage());
                    return null;
                } catch (Exception e) {
                    mostrarAlerta("Error", "Error inesperado: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

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

    private TextField createStyledTextField(String prompt, boolean required) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle(
                "-fx-padding: 8 12;" +
                        "-fx-background-color: rgba(255,255,255,0.9);" +
                        "-fx-text-fill: #2c3e50;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-color: transparent;");

        if (required) {
            field.setStyle(field.getStyle() + "-fx-border-color: #3498db; -fx-border-width: 1.5;");
        }

        return field;
    }

    private ComboBox<String> createStyledComboBox(String... items) {
        ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(items));
        combo.setValue(items[0]);
        combo.setStyle(
                "-fx-padding: 8 12;" +
                        "-fx-background-color: rgba(255,255,255,0.9);" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-color: #3498db;" +
                        "-fx-border-width: 1.5;");
        combo.setPrefWidth(200);
        return combo;
    }

    private Label createIconLabel(String iconCode, String color) {
        // Mapeo de códigos a emojis
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(16);
        icon.setIconColor(Paint.valueOf(color));
        Label wrapper = new Label();
        wrapper.setGraphic(icon);
        wrapper.setStyle("-fx-padding: 0 10 0 0;");
        return wrapper;
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
package controllers;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import models.Producto;
import models.Role;
import org.kordamp.ikonli.javafx.FontIcon;
import util.AuditoriaUtil;
import util.DatabaseUtil;
import util.SessionManager;
import util.StockAlertUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class InsumosAdminController implements Initializable {

    @FXML private TableView<Producto> tablaInsumos;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, Integer> colStock;
    @FXML private TableColumn<Producto, Double> colPrecio;
    @FXML private TableColumn<Producto, Integer> colUmbral;
    @FXML private Label lblStock;
    @FXML private Label lblPrecio;
    @FXML private Label lblUmbral;
    @FXML private Label lblStockInicial;

    private final ObservableList<Producto> listaProductos = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (!SessionManager.tienePermiso(Role.ADMIN)) {
            if (tablaInsumos != null) {
                tablaInsumos.setDisable(true);
            }
            return;
        }
        configurarTabla();
        configurarSemaforoTabla();
        tablaInsumos.setItems(listaProductos);
        cargarProductos();
        tablaInsumos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, nuevo) -> mostrarDetalles(nuevo));
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colUmbral.setCellValueFactory(new PropertyValueFactory<>("umbral"));

        colNombre.prefWidthProperty().bind(tablaInsumos.widthProperty().multiply(0.40));
        colStock.prefWidthProperty().bind(tablaInsumos.widthProperty().multiply(0.20));
        colPrecio.prefWidthProperty().bind(tablaInsumos.widthProperty().multiply(0.20));
        colUmbral.prefWidthProperty().bind(tablaInsumos.widthProperty().multiply(0.20));

        colPrecio.setCellFactory(column -> new TableCell<>() {
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

    private void configurarSemaforoTabla() {
        tablaInsumos.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Producto producto, boolean empty) {
                super.updateItem(producto, empty);
                if (empty || producto == null) {
                    setStyle("");
                    setTooltip(null);
                } else {
                    StockAlertUtil.StockStatus status = StockAlertUtil.evaluate(producto);
                    String color = status.getSuggestedColor();
                    setStyle(String.format("-fx-background-color: %s;", color));
                    Tooltip tooltip = new Tooltip(status.getTooltipText());
                    setTooltip(tooltip);
                }
            }
        });
    }

    private void mostrarDetalles(Producto producto) {
        if (producto == null) {
            lblStock.setText("-");
            lblPrecio.setText("-");
            lblUmbral.setText("-");
            lblStockInicial.setText("-");
            lblStockInicial.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
            lblStockInicial.setTooltip(null);
        } else {
            lblStock.setText(String.valueOf(producto.getStock()));
            lblPrecio.setText(String.format(Locale.getDefault(), "$%.2f", producto.getPrecio()));
            lblUmbral.setText(String.valueOf(producto.getUmbral()));
            StockAlertUtil.StockStatus status = StockAlertUtil.evaluate(producto);
            String stockInicialTexto = producto.getStockInicial() > 0
                    ? String.valueOf(producto.getStockInicial())
                    : "N/D";
            lblStockInicial.setText(String.format("%s (punto medio: %d)",
                    stockInicialTexto, status.getPuntoMedio()));
            String textoColor = switch (status.getLevel()) {
                case OPTIMO -> "-fx-text-fill: #2E7D32;";
                case PREVENCION -> "-fx-text-fill: #F57C00;";
                default -> "-fx-text-fill: #C62828;";
            };
            lblStockInicial.setStyle(textoColor + "-fx-font-size: 16px; -fx-font-weight: bold;");
            Tooltip tooltip = new Tooltip(status.getTooltipText());
            lblStockInicial.setTooltip(tooltip);
        }
    }

    private void cargarProductos() {
        try {
            listaProductos.setAll(DatabaseUtil.getProductos());
            if (!listaProductos.isEmpty()) {
                tablaInsumos.getSelectionModel().selectFirst();
            } else {
                mostrarDetalles(null);
            }
        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudieron cargar los insumos.");
        }
    }

    @FXML
    private void handleRegistrarInsumo() {
        Dialog<Producto> dialog = crearDialogoRegistro();
        Optional<Producto> resultado = dialog.showAndWait();
        resultado.ifPresent(producto -> {
            try {
                DatabaseUtil.insertarProducto(producto);
                AuditoriaUtil.registrarAccion(
                        SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 0,
                        "Registro insumo",
                        producto.getNombre()
                );
                cargarProductos();
                mostrarDialogoExito("¡Insumo registrado correctamente!");
            } catch (Exception e) {
                mostrarAlerta("Error", "No se pudo registrar el insumo.");
            }
        });
    }

    private Dialog<Producto> crearDialogoRegistro() {
        Dialog<Producto> dialog = new Dialog<>();
        dialog.setTitle("Registrar Insumo");
        dialog.setHeaderText(null);

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

        ButtonType btnRegistrar = new ButtonType("Registrar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnRegistrar, ButtonType.CANCEL);

        Button registrarButton = (Button) dialog.getDialogPane().lookupButton(btnRegistrar);
        registrarButton.setStyle(
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
        grid.setPadding(new javafx.geometry.Insets(20));
        grid.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1);" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 8, 0, 0, 0);");
        grid.setPrefWidth(520);

        VBox contenedor = new VBox(15);
        contenedor.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        Label titulo = new Label("Registrar nuevo insumo");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subtitulo = new Label("Complete los detalles del insumo");
        subtitulo.setStyle("-fx-text-fill: #bdc3c7; -fx-padding: 0 0 10 0;");
        contenedor.getChildren().addAll(titulo, subtitulo, grid);

        TextField txtNombre = createStyledTextField("Nombre del insumo", true);
        ComboBox<String> cbTipo = createStyledComboBox("PACA", "KG", "LB");
        TextField txtPesoTotal = createStyledTextField("Peso total (kg/lb)", false);
        TextField txtPesoScoop = createStyledTextField("Peso por scoop (g)", false);
        TextField txtPrecioCompra = createStyledTextField("Precio compra (envase)", true);
        TextField txtPrecioVenta = createStyledTextField("Precio venta", true);
        TextField txtUnidadesPorPaca = createStyledTextField("Unidades por paca", false);
        TextField txtUmbral = createStyledTextField("Umbral mínimo", false);
        TextField txtStockInicial = createStyledTextField("Stock inicial", true);

        txtNombre.setTextFormatter(new TextFormatter<>(change -> {
            change.setText(change.getText().toUpperCase());
            return change;
        }));
        txtPrecioCompra.setTextFormatter(createNumericFormatter(true));
        txtPrecioVenta.setTextFormatter(createNumericFormatter(true));
        txtPesoTotal.setTextFormatter(createNumericFormatter(true));
        txtPesoScoop.setTextFormatter(createNumericFormatter(true));
        txtUnidadesPorPaca.setTextFormatter(createNumericFormatter(false));
        txtUmbral.setTextFormatter(createNumericFormatter(false));
        txtStockInicial.setTextFormatter(createNumericFormatter(false));

        Label lblStockCalculado = new Label();
        lblStockCalculado.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");

        Label lblResultado = new Label();
        lblResultado.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 0 0 0;");
        lblResultado.setWrapText(true);
        lblResultado.setMaxWidth(Double.MAX_VALUE);

        Label iconNombre = createIconLabel("fas-box", "#3498db");
        Label iconTipo = createIconLabel("fas-cube", "#e74c3c");
        Label iconPrecioCompra = createIconLabel("fas-money-bill-wave", "#2ecc71");
        Label iconPrecioVenta = createIconLabel("fas-tag", "#f39c12");
        Label iconUnidades = createIconLabel("fas-cubes", "#9b59b6");
        Label iconPesoTotal = createIconLabel("fas-weight", "#1abc9c");
        Label iconScoop = createIconLabel("fas-utensil-spoon", "#e67e22");
        Label iconUmbral = createIconLabel("fas-bell", "#f1c40f");
        Label iconStockInicial = createIconLabel("fas-flag-checkered", "#16a085");

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

        grid.add(iconUmbral, 0, 7);
        grid.add(txtUmbral, 1, 7, 2, 1);

        grid.add(iconStockInicial, 0, 8);
        grid.add(txtStockInicial, 1, 8, 2, 1);

        grid.add(lblStockCalculado, 0, 9, 3, 1);
        grid.add(lblResultado, 0, 10, 3, 1);

        Label lblValidacion = new Label();
        lblValidacion.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold;");
        lblValidacion.setWrapText(true);
        grid.add(lblValidacion, 0, 11, 3, 1);

        txtPesoTotal.setVisible(false);
        txtPesoScoop.setVisible(false);
        iconPesoTotal.setVisible(false);
        iconScoop.setVisible(false);

        txtUnidadesPorPaca.setVisible(true);
        iconUnidades.setVisible(true);

        cbTipo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean esPaca = "PACA".equals(newVal);
            boolean esSuplemento = "KG".equals(newVal) || "LB".equals(newVal);

            txtUnidadesPorPaca.setVisible(esPaca);
            iconUnidades.setVisible(esPaca);
            txtPesoTotal.setVisible(esSuplemento);
            txtPesoScoop.setVisible(esSuplemento);
            iconPesoTotal.setVisible(esSuplemento);
            iconScoop.setVisible(esSuplemento);

            txtPrecioVenta.setPromptText(esSuplemento ?
                    "Precio por scoop" : "Precio por unidad");

            if (esPaca) {
                txtPesoTotal.clear();
                txtPesoScoop.clear();
            } else {
                txtUnidadesPorPaca.clear();
            }

            lblStockCalculado.setText(esPaca ?
                    "Ingrese unidades por paca para calcular ganancias" :
                    "Complete peso total y peso por scoop para calcular ganancias");
        });

        ChangeListener<String> calculador = (observable, oldValue, newValue) -> {
            try {
                String tipo = cbTipo.getValue();
                double precioCompra = Double.parseDouble(txtPrecioCompra.getText());
                double precioVenta = Double.parseDouble(txtPrecioVenta.getText());

                int stock = 0;
                double gananciaUnidad;
                if ("PACA".equals(tipo)) {
                    int unidadesPorPaca = Integer.parseInt(txtUnidadesPorPaca.getText());
                    int stockInicial = Integer.parseInt(txtStockInicial.getText());
                    double costoPorUnidad = precioCompra / unidadesPorPaca;
                    gananciaUnidad = precioVenta - costoPorUnidad;
                    stock = stockInicial;
                    lblStockCalculado.setText(String.format("Stock inicial: %d | Costo/unidad: $%.2f",
                            stockInicial, costoPorUnidad));
                } else if ("KG".equals(tipo) || "LB".equals(tipo)) {
                    double pesoTotal = Double.parseDouble(txtPesoTotal.getText());
                    double pesoScoop = Double.parseDouble(txtPesoScoop.getText());

                    if ("KG".equals(tipo)) {
                        stock = (int) ((pesoTotal * 1000) / pesoScoop);
                    } else {
                        stock = (int) ((pesoTotal * 453.592) / pesoScoop);
                    }

                    double costoPorScoop = precioCompra / stock;
                    gananciaUnidad = precioVenta - costoPorScoop;
                    lblStockCalculado.setText(String.format("Servicios disponibles: %d | Costo/scoop: $%.2f",
                            stock, costoPorScoop));
                } else {
                    gananciaUnidad = 0;
                }

                double gananciaTotal = gananciaUnidad * stock;

                if (gananciaUnidad >= 0) {
                    lblResultado.setText(String.format("▲ GANANCIA: $%.2f | TOTAL: $%.2f",
                            gananciaUnidad, gananciaTotal));
                    lblResultado.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                } else {
                    lblResultado.setText(String.format("▼ PÉRDIDA: $%.2f | TOTAL: $%.2f",
                            Math.abs(gananciaUnidad), Math.abs(gananciaTotal)));
                    lblResultado.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            } catch (NumberFormatException e) {
                lblResultado.setText("Complete los campos requeridos");
                lblResultado.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
            }
        };

        txtPrecioCompra.textProperty().addListener(calculador);
        txtPrecioVenta.textProperty().addListener(calculador);
        txtPesoTotal.textProperty().addListener(calculador);
        txtPesoScoop.textProperty().addListener(calculador);
        txtUnidadesPorPaca.textProperty().addListener(calculador);
        txtStockInicial.textProperty().addListener(calculador);
        cbTipo.valueProperty().addListener((obs, oldVal, newVal) -> calculador.changed(null, null, null));

        lblStockCalculado.setText("Seleccione tipo de insumo y complete los campos");
        lblStockCalculado.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");

        dialog.getDialogPane().setContent(contenedor);

        ChangeListener<String> limpiarValidacion = (obs, oldVal, newVal) -> lblValidacion.setText("");
        txtStockInicial.textProperty().addListener(limpiarValidacion);
        txtUmbral.textProperty().addListener(limpiarValidacion);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnRegistrar) {
                try {
                    String nombre = txtNombre.getText().trim().toLowerCase(Locale.ROOT);
                    String tipo = cbTipo.getValue();
                    BigDecimal bdPrecioCompra = new BigDecimal(txtPrecioCompra.getText().trim());
                    BigDecimal bdPrecioVenta = new BigDecimal(txtPrecioVenta.getText().trim());

                    bdPrecioCompra = bdPrecioCompra.setScale(2, RoundingMode.HALF_UP);
                    bdPrecioVenta = bdPrecioVenta.setScale(2, RoundingMode.HALF_UP);

                    Producto nuevo = new Producto();
                    nuevo.setNombre(nombre);
                    nuevo.setTipo(tipo);
                    nuevo.setPrecioCompra(bdPrecioCompra.doubleValue());
                    nuevo.setPrecio(bdPrecioVenta.doubleValue());

                    int umbral = txtUmbral.getText().isBlank() ? 0 : Integer.parseInt(txtUmbral.getText());
                    nuevo.setUmbral(umbral);

                    if ("PACA".equals(tipo)) {
                        int unidadesPorPaca = Integer.parseInt(txtUnidadesPorPaca.getText());
                        nuevo.setUnidadesPorPaca(unidadesPorPaca);
                    } else {
                        double pesoTotal = Double.parseDouble(txtPesoTotal.getText());
                        double pesoScoop = Double.parseDouble(txtPesoScoop.getText());

                        nuevo.setPesoTotal(pesoTotal);
                        nuevo.setPesoScoop(pesoScoop);
                    }

                    if (txtStockInicial.getText().isBlank()) {
                        lblValidacion.setText("Ingrese el stock inicial del producto.");
                        return null;
                    }

                    int stockIngresado = Integer.parseInt(txtStockInicial.getText());

                    nuevo.setStock(stockIngresado);
                    nuevo.setStockInicial(stockIngresado);

                    return nuevo;
                } catch (Exception e) {
                    lblValidacion.setText("Complete los campos requeridos correctamente.");
                    return null;
                }
            }
            return null;
        });

        return dialog;
    }

    @FXML
    private void handleEditarInsumo() {
        Producto seleccionado = tablaInsumos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Advertencia", "Debe seleccionar un insumo para editar.");
            return;
        }

        Dialog<Producto> dialog = new Dialog<>();
        dialog.setTitle("Editar Insumo");
        dialog.setHeaderText(null);

        ButtonType botonActualizar = new ButtonType("Actualizar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(botonActualizar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new javafx.geometry.Insets(20));

        Label lblPrecio = new Label("Precio nuevo:");
        TextField tfPrecio = new TextField(String.format(Locale.getDefault(), "%.2f", seleccionado.getPrecio()));

        Label lblStockActual = new Label("Stock actual:");
        Label lblStockValor = new Label(String.valueOf(seleccionado.getStock()));
        lblStockValor.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        Label lblSumar = new Label("Unidades a sumar:");
        TextField tfStock = new TextField("0");

        Label lblUmbralNuevo = new Label("Umbral mínimo:");
        TextField tfUmbral = new TextField(String.valueOf(seleccionado.getUmbral()));

        Label lblStockInicialTitulo = new Label("Stock inicial:");
        Label lblStockInicialValor = new Label(String.valueOf(seleccionado.getStockInicial()));
        lblStockInicialValor.setStyle("-fx-font-weight: bold; -fx-text-fill: #1565C0;");

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        grid.add(lblPrecio, 0, 0);
        grid.add(tfPrecio, 1, 0);
        grid.add(lblStockActual, 0, 1);
        grid.add(lblStockValor, 1, 1);
        grid.add(lblSumar, 0, 2);
        grid.add(tfStock, 1, 2);
        grid.add(lblUmbralNuevo, 0, 3);
        grid.add(tfUmbral, 1, 3);
        grid.add(lblStockInicialTitulo, 0, 4);
        grid.add(lblStockInicialValor, 1, 4);
        grid.add(lblError, 0, 5, 2, 1);

        dialog.getDialogPane().setContent(grid);

        Node btnActualizar = dialog.getDialogPane().lookupButton(botonActualizar);
        btnActualizar.setDisable(true);

        ChangeListener<String> validador = (obs, oldVal, newVal) -> {
            boolean precioOk = esPrecioValido(tfPrecio.getText());
            boolean stockOk = esStockValido(tfStock.getText());
            boolean umbralOk = esStockValido(tfUmbral.getText());

            if (!precioOk) {
                lblError.setText("Precio inválido (debe ser número >= 0)");
            } else if (!stockOk) {
                lblError.setText("Unidades a sumar inválidas (entero >= 0)");
            } else if (!umbralOk) {
                lblError.setText("Umbral inválido (debe ser entero >= 0)");
            } else {
                lblError.setText("");
            }

            btnActualizar.setDisable(!(precioOk && stockOk && umbralOk));
        };

        tfPrecio.textProperty().addListener(validador);
        tfStock.textProperty().addListener(validador);
        tfUmbral.textProperty().addListener(validador);
        validador.changed(null, null, null);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == botonActualizar) {
                Producto editado = new Producto();
                editado.setId(seleccionado.getId());
                editado.setPrecio(Double.parseDouble(tfPrecio.getText()));
                editado.setStock(Integer.parseInt(tfStock.getText()));
                editado.setUmbral(Integer.parseInt(tfUmbral.getText()));
                return editado;
            }
            return null;
        });

        Optional<Producto> resultado = dialog.showAndWait();
        resultado.ifPresent(prod -> {
            try {
                DatabaseUtil.actualizarProducto(prod.getId(), prod.getPrecio(), prod.getUmbral());
                if (prod.getStock() > 0) {
                    DatabaseUtil.registrarEntradaProducto(prod.getId(), prod.getStock());
                }

                AuditoriaUtil.registrarAccion(
                        SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 0,
                        "Actualización insumo",
                        seleccionado.getNombre()
                );

                mostrarAlerta("Éxito", "Insumo actualizado correctamente.");
                cargarProductos();
            } catch (Exception e) {
                mostrarAlerta("Error", "No se pudo actualizar el insumo.");
            }
        });
    }

    @FXML
    private void handleEliminarInsumo() {
        Producto seleccionado = tablaInsumos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Advertencia", "Debe seleccionar un insumo para eliminar.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Confirmar eliminación");
        dialog.setHeaderText(null);

        VBox contenido = new VBox(15);
        contenido.setPadding(new javafx.geometry.Insets(25));
        contenido.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label titulo = new Label("¿Está seguro de eliminar este insumo?");
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
                DatabaseUtil.executeUpdate("DELETE FROM productos WHERE id = ?", seleccionado.getId());
                AuditoriaUtil.registrarAccion(
                        SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 0,
                        "Eliminación insumo",
                        seleccionado.getNombre()
                );
                mostrarAlerta("Éxito", "Insumo eliminado correctamente.");
                cargarProductos();
            } catch (Exception e) {
                mostrarAlerta("Error", "No se pudo eliminar el insumo.");
            }
        }
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

    private TextFormatter<String> createNumericFormatter(boolean allowDecimal) {
        String pattern = allowDecimal ? "\\d*(\\.\\d*)?" : "\\d*";
        return new TextFormatter<>(change ->
                change.getControlNewText().matches(pattern) ? change : null);
    }

    private Label createIconLabel(String iconCode, String color) {
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(16);
        icon.setIconColor(Paint.valueOf(color));
        Label wrapper = new Label();
        wrapper.setGraphic(icon);
        wrapper.setStyle("-fx-padding: 0 10 0 0;");
        return wrapper;
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

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarDialogoExito(String mensaje) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Operación exitosa");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().setStyle(
                "-fx-background-color: linear-gradient(to bottom, #2c3e50, #1a1a2e);" +
                        "-fx-padding: 20;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-color: rgba(255,255,255,0.1);" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 0);");

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.setStyle(
                "-fx-background-color: linear-gradient(to right, #4CAF50, #2ECC71);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 25;" +
                        "-fx-padding: 8 20;");

        VBox contenido = new VBox(15);
        contenido.setPadding(new javafx.geometry.Insets(20));
        contenido.setAlignment(javafx.geometry.Pos.CENTER);

        Label icono = new Label("\u2714");
        icono.setStyle("-fx-font-size: 48px; -fx-text-fill: #4CAF50;");

        Label titulo = new Label(mensaje);
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        contenido.getChildren().addAll(icono, titulo);
        dialog.getDialogPane().setContent(contenido);

        dialog.showAndWait();
    }
}
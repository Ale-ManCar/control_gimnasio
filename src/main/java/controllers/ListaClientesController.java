package controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import models.Cliente;
import util.DatabaseUtil;
import javafx.scene.paint.Color;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ListaClientesController implements Initializable {

    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, String> colNombreCompleto;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colEstado;
    @FXML private TableColumn<Cliente, Void> colAcciones;
    @FXML private TextField txtBuscar;
    @FXML private Button btnLimpiar;
    @FXML private Label lblTitulo;

    private ObservableList<Cliente> clientesOriginales = FXCollections.observableArrayList();
    private Cliente clienteEditado;
    private String estiloOriginalTabla;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarEstilosGlobales();
        configurarColumnas();
        cargarClientes();
        configurarBusqueda();

        estiloOriginalTabla = tablaClientes.getStyle();
        clientesOriginales.setAll(tablaClientes.getItems());

        configurarFilas();
        ajustarAnchoColumnas();
        eliminarBarrasDesplazamiento();
    }

    private void configurarEstilosGlobales() {
        lblTitulo.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #4A6CF7;" +
                        "-fx-padding: 0 0 15px 0;"
        );

        tablaClientes.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-background-color: #ffffff;" +
                        "-fx-border-radius: 10px;" +
                        "-fx-background-radius: 10px;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);"
        );
    }

    private void configurarColumnas() {
        colNombreCompleto.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        colNombreCompleto.setCellFactory(column -> new TableCell<Cliente, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER; "
                            + "-fx-font-weight: bold; "
                            + "-fx-text-fill: black; "
                            + "-fx-background-color: transparent;");
                }
            }
        });

        colTelefono.setCellFactory(column -> new TableCell<Cliente, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER; "
                            + "-fx-font-weight: bold; "
                            + "-fx-text-fill: black; "
                            + "-fx-background-color: transparent;");
                }
            }
        });

        colEstado.setCellFactory(column -> new TableCell<Cliente, String>() {
            private final StackPane container = new StackPane();
            private final Circle indicador = new Circle(5);
            private final Label texto = new Label();

            {
                container.setAlignment(Pos.CENTER);
                HBox caja = new HBox(5, indicador, texto);
                caja.setAlignment(Pos.CENTER);
                container.getChildren().add(caja);

                texto.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            }

            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setGraphic(null);
                } else {
                    texto.setText(estado);
                    if ("Activo".equals(estado)) {
                        indicador.setFill(Color.web("#28A745"));
                        texto.setStyle("-fx-text-fill: #28A745; -fx-font-weight: bold;");
                    } else {
                        indicador.setFill(Color.web("#DC3545"));
                        texto.setStyle("-fx-text-fill: #DC3545; -fx-font-weight: bold;");
                    }
                    setGraphic(container);
                    setStyle("-fx-alignment: CENTER; "
                            + "-fx-background-color: transparent;");
                }
            }
        });

        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button();

            {
                btnEditar.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-font-size: 16px;" +
                                "-fx-cursor: hand;"
                );

                btnEditar.setText("✏️");
                btnEditar.setTooltip(new Tooltip("Editar cliente"));

                btnEditar.setOnMouseEntered(e ->
                        btnEditar.setStyle("-fx-background-color: #e0f0ff; -fx-font-size: 16px; -fx-cursor: hand;")
                );
                btnEditar.setOnMouseExited(e ->
                        btnEditar.setStyle("-fx-background-color: transparent; -fx-font-size: 16px; -fx-cursor: hand;")
                );

                btnEditar.setOnAction(event -> {
                    Cliente cliente = getTableView().getItems().get(getIndex());
                    mostrarDialogoEdicion(cliente);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnEditar);
                }
            }
        });
    }

    private void eliminarBarrasDesplazamiento() {
        tablaClientes.setStyle(tablaClientes.getStyle() +
                " -fx-scroll-bar-policy: never;"
        );

        Platform.runLater(() -> {
            for (javafx.scene.Node node : tablaClientes.lookupAll(".scroll-bar")) {
                node.setVisible(false);
                node.setManaged(false);
            }

            for (javafx.scene.Node node : tablaClientes.lookupAll(".corner")) {
                node.setVisible(false);
                node.setManaged(false);
            }

            for (javafx.scene.Node node : tablaClientes.lookupAll(".scroll-pane")) {
                if (node instanceof javafx.scene.control.ScrollPane) {
                    ((ScrollPane) node).setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                    ((ScrollPane) node).setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                }
            }
        });
    }

    private void ajustarAnchoColumnas() {
        Platform.runLater(() -> {
            double anchoTotal = tablaClientes.getWidth();
            if (anchoTotal > 0) {
                colNombreCompleto.setPrefWidth(anchoTotal * 0.40);
                colTelefono.setPrefWidth(anchoTotal * 0.25);
                colEstado.setPrefWidth(anchoTotal * 0.20);
                colAcciones.setPrefWidth(anchoTotal * 0.15);
                tablaClientes.requestLayout();
            }
        });

        tablaClientes.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0) {
                double anchoTotal = newVal.doubleValue();
                colNombreCompleto.setPrefWidth(anchoTotal * 0.40);
                colTelefono.setPrefWidth(anchoTotal * 0.25);
                colEstado.setPrefWidth(anchoTotal * 0.20);
                colAcciones.setPrefWidth(anchoTotal * 0.15);
                tablaClientes.requestLayout();
            }
        });
    }

    private void configurarBusqueda() {
        txtBuscar.setPromptText("Buscar cliente...");
        txtBuscar.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-padding: 8px 15px;" +
                        "-fx-background-radius: 20px;" +
                        "-fx-border-radius: 20px;" +
                        "-fx-border-color: #ced4da;" +
                        "-fx-background-color: #ffffff;"
        );

        btnLimpiar.setStyle(
                "-fx-background-color: #6C757D;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8px 15px;" +
                        "-fx-background-radius: 20px;" +
                        "-fx-border-radius: 20px;" +
                        "-fx-cursor: hand;"
        );
        btnLimpiar.setOnMouseEntered(e ->
                btnLimpiar.setStyle(
                        "-fx-background-color: #5a6268;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 8px 15px;" +
                                "-fx-background-radius: 20px;" +
                                "-fx-border-radius: 20px;" +
                                "-fx-cursor: hand;"
                )
        );
        btnLimpiar.setOnMouseExited(e ->
                btnLimpiar.setStyle(
                        "-fx-background-color: #6C757D;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 8px 15px;" +
                                "-fx-background-radius: 20px;" +
                                "-fx-border-radius: 20px;" +
                                "-fx-cursor: hand;"
                )
        );

        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            filtrarClientes();
        });
    }

    private void configurarFilas() {
        tablaClientes.setRowFactory(tv -> {
            TableRow<Cliente> row = new TableRow<Cliente>() {
                @Override
                protected void updateItem(Cliente cliente, boolean empty) {
                    super.updateItem(cliente, empty);

                    if (empty || cliente == null) {
                        setBackground(null);
                        setGraphic(null);
                        setStyle("");
                        setTooltip(null);
                    } else {
                        // TOOLTIP ESTILO DASHBOARD
                        Tooltip tooltip = new Tooltip(cliente.getTooltipText());
                        tooltip.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                        setTooltip(tooltip);

                        if (isSelected()) {
                            setStyle("-fx-background-color: #e6f2ff; "
                                    + "-fx-border-color: #e0e0e0; "
                                    + "-fx-border-width: 0 0 1px 0;");
                        } else if (isHover()) {
                            setStyle("-fx-background-color: #e6f2ff; "
                                    + "-fx-border-color: #e0e0e0; "
                                    + "-fx-border-width: 0 0 1px 0;");
                        } else {
                            setStyle("-fx-background-color: #ffffff; "
                                    + "-fx-border-color: #e0e0e0; "
                                    + "-fx-border-width: 0 0 1px 0;");
                        }
                    }
                }
            };

            row.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    row.setStyle("-fx-background-color: #e6f2ff; "
                            + "-fx-border-color: #e0e0e0; "
                            + "-fx-border-width: 0 0 1px 0;");
                } else {
                    if (row.isHover()) {
                        row.setStyle("-fx-background-color: #e6f2ff; "
                                + "-fx-border-color: #e0e0e0; "
                                + "-fx-border-width: 0 0 1px 0;");
                    } else {
                        row.setStyle("-fx-background-color: #ffffff; "
                                + "-fx-border-color: #e0e0e0; "
                                + "-fx-border-width: 0 0 1px 0;");
                    }
                }
            });

            row.hoverProperty().addListener((obs, oldVal, isHovering) -> {
                if (isHovering && !row.isSelected()) {
                    row.setStyle("-fx-background-color: #e6f2ff; "
                            + "-fx-border-color: #e0e0e0; "
                            + "-fx-border-width: 0 0 1px 0;");
                } else if (!row.isSelected()) {
                    row.setStyle("-fx-background-color: #ffffff; "
                            + "-fx-border-color: #e0e0e0; "
                            + "-fx-border-width: 0 0 1px 0;");
                }
            });

            return row;
        });
    }

    @FXML
    private void filtrarClientes() {
        String filtro = txtBuscar.getText().trim().toLowerCase();

        if (filtro.isEmpty()) {
            tablaClientes.setItems(clientesOriginales);
        } else {
            ObservableList<Cliente> filtrados = FXCollections.observableArrayList();
            for (Cliente cliente : clientesOriginales) {
                if (cliente.getNombreCompleto().toLowerCase().contains(filtro) ||
                        cliente.getTelefono().contains(filtro)) {
                    filtrados.add(cliente);
                }
            }

            filtrados.sort(Comparator.comparing(Cliente::getNombreCompleto, String.CASE_INSENSITIVE_ORDER));
            tablaClientes.setItems(filtrados);
        }

        tablaClientes.refresh();
        tablaClientes.requestLayout();
        ajustarAnchoColumnas();
    }

    @FXML
    private void limpiarFiltro() {
        Cliente seleccionado = tablaClientes.getSelectionModel().getSelectedItem();
        int scrollPosition = tablaClientes.getSelectionModel().getSelectedIndex();

        txtBuscar.clear();
        tablaClientes.setItems(clientesOriginales);
        tablaClientes.setStyle(estiloOriginalTabla);

        if (seleccionado != null) {
            tablaClientes.getSelectionModel().select(seleccionado);
        }
        tablaClientes.scrollTo(scrollPosition);

        tablaClientes.refresh();
        tablaClientes.requestLayout();
        ajustarAnchoColumnas();
    }

    private void mostrarDialogoEdicion(Cliente cliente) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/confirmar_edicion_dialog.fxml"));
            VBox dialogContent = loader.load();

            ConfirmarEdicionDialogController controller = loader.getController();
            controller.setNombreCliente(cliente.getNombreCompleto());

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(tablaClientes.getScene().getWindow());
            dialog.initStyle(StageStyle.TRANSPARENT);
            dialog.setScene(new Scene(dialogContent));

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), dialogContent);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            controller.btnConfirmar.setOnAction(e -> {
                dialog.close();
                abrirVentanaEdicion(cliente);
            });

            controller.btnCancelar.setOnAction(e -> dialog.close());

            dialog.show();
            fadeIn.play();

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo cargar el diálogo de confirmación");
        }
    }

    private void abrirVentanaEdicion(Cliente cliente) {
        try {
            this.clienteEditado = cliente;

            URL fxmlUrl = getClass().getResource("/fxml/editar_cliente.fxml");
            if (fxmlUrl == null) {
                throw new IOException("Archivo FXML no encontrado: /fxml/editar_cliente.fxml");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            EditarClienteController controller = loader.getController();
            if (controller == null) {
                throw new IOException("Controlador no inicializado");
            }
            controller.setCliente(cliente);

            Stage stage = new Stage();
            stage.setTitle("Editar Cliente");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            recargarClientes();

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir la ventana de edición:\n" + e.getMessage());
        }
    }

    private void aplicarEfectoExito(TableRow<Cliente> row) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(150), row);
        scale.setFromX(1);
        scale.setFromY(1);
        scale.setToX(1.03);
        scale.setToY(1.03);
        scale.setCycleCount(4);
        scale.setAutoReverse(true);
        scale.play();
    }

    private void recargarClientes() {
        String telefonoEditado = clienteEditado != null ? clienteEditado.getTelefono() : null;

        tablaClientes.getItems().clear();
        cargarClientes();
        clientesOriginales.setAll(tablaClientes.getItems());
        ajustarAnchoColumnas();

        if (telefonoEditado != null) {
            for (int i = 0; i < tablaClientes.getItems().size(); i++) {
                Cliente c = tablaClientes.getItems().get(i);
                if (c.getTelefono().equals(telefonoEditado)) {
                    int finalI = i;
                    Platform.runLater(() -> {
                        TableRow<Cliente> row = (TableRow<Cliente>) tablaClientes.lookup(".table-row-cell:" + finalI);
                        if (row != null) {
                            aplicarEfectoExito(row);
                        }
                    });
                    break;
                }
            }
        }

        clienteEditado = null;
    }

    private void cargarClientes() {
        String sql = "SELECT nombres, apellidos, telefono, tipoMembresia, fecha_vencimiento FROM clientes WHERE activo = 1";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            ObservableList<Cliente> clientesTemp = FXCollections.observableArrayList();

            while (rs.next()) {
                Cliente cliente = new Cliente(
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("telefono"),
                        rs.getString("tipoMembresia"),
                        LocalDate.parse(rs.getString("fecha_vencimiento"))
                );
                clientesTemp.add(cliente);
            }

            clientesTemp.sort(Comparator.comparing(Cliente::getNombreCompleto, String.CASE_INSENSITIVE_ORDER));
            tablaClientes.getItems().setAll(clientesTemp);

        } catch (SQLException e) {
            mostrarAlerta("Error de Base de Datos", "No se pudieron cargar los clientes");
        }
    }

    @FXML
    private void handleVolver() {
        Stage stage = (Stage) tablaClientes.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleVerInactivos() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/lista_clientes_inactivos.fxml"));
            Stage stage = (Stage) tablaClientes.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo abrir inactivos");
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: #ffffff;" +
                        "-fx-font-size: 14px;" +
                        "-fx-border-radius: 10px;" +
                        "-fx-background-radius: 10px;"
        );

        alert.showAndWait();
    }
}
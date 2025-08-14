package controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import models.Cliente;
import util.DatabaseUtil;
import util.AuditoriaUtil;
import util.SessionManager;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;

public class ListaClientesInactivosController {

    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, String> colNombreCompleto;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colEstado;
    @FXML private TableColumn<Cliente, Void> colAcciones;
    @FXML private TextField txtBuscar;
    @FXML private Button btnLimpiar;
    @FXML private Label lblTitulo;

    private ObservableList<Cliente> clientesOriginales = FXCollections.observableArrayList();
    private String estiloOriginalTabla;

    @FXML
    public void initialize() {
        configurarEstilos();
        configurarColumnas();
        cargarClientes();
        configurarBusqueda();
        ajustarAnchoColumnas();
        eliminarBarrasDesplazamiento();
        configurarFilas();

        estiloOriginalTabla = tablaClientes.getStyle();
    }

    private void configurarEstilos() {
        lblTitulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #4A6CF7; -fx-padding: 0 0 15px 0;");
        lblTitulo.setAlignment(Pos.CENTER);
        tablaClientes.setStyle("-fx-font-size: 14px; -fx-background-color: #ffffff; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
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
                    indicador.setFill(Color.web("#DC3545"));
                    texto.setStyle("-fx-text-fill: #DC3545; -fx-font-weight: bold;");
                    setGraphic(container);
                    setStyle("-fx-alignment: CENTER; "
                            + "-fx-background-color: transparent;");
                }
            }
        });

        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnActivar = new Button();
            private final Button btnEliminar = new Button();
            private final HBox container = new HBox(10, btnActivar, btnEliminar);

            {
                container.setAlignment(Pos.CENTER);

                FontIcon iconoActivar = new FontIcon(FontAwesomeSolid.REDO);
                iconoActivar.setIconColor(Color.web("#28a745"));
                iconoActivar.setIconSize(18);
                btnActivar.setGraphic(iconoActivar);

                FontIcon iconoEliminar = new FontIcon(FontAwesomeSolid.TRASH);
                iconoEliminar.setIconColor(Color.web("#dc3545"));
                iconoEliminar.setIconSize(18);
                btnEliminar.setGraphic(iconoEliminar);

                String estiloBase = "-fx-background-color: transparent; -fx-cursor: hand;";
                btnActivar.setStyle(estiloBase);
                btnEliminar.setStyle(estiloBase);

                btnActivar.setOnMouseEntered(e ->
                        btnActivar.setStyle("-fx-background-color: #e0f0ff; " + estiloBase)
                );
                btnActivar.setOnMouseExited(e ->
                        btnActivar.setStyle(estiloBase)
                );

                btnEliminar.setOnMouseEntered(e ->
                        btnEliminar.setStyle("-fx-background-color: #ffe0e0; " + estiloBase)
                );
                btnEliminar.setOnMouseExited(e ->
                        btnEliminar.setStyle(estiloBase)
                );

                btnActivar.setTooltip(new Tooltip("Reactivar cliente"));
                btnEliminar.setTooltip(new Tooltip("Eliminar cliente permanentemente"));

                if (!SessionManager.isAdmin()) {
                    container.getChildren().remove(btnEliminar);
                }

                btnActivar.setOnAction(event -> {
                    Cliente cliente = getTableView().getItems().get(getIndex());
                    activarCliente(cliente);
                });

                btnEliminar.setOnAction(event -> {
                    Cliente cliente = getTableView().getItems().get(getIndex());
                    eliminarCliente(cliente);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });
    }

    private void cargarClientes() {
        String sql = "SELECT nombres, apellidos, telefono, tipoMembresia, fecha_vencimiento FROM clientes WHERE activo = 0";

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
            clientesOriginales.setAll(tablaClientes.getItems());
        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudieron cargar clientes inactivos");
        }
    }

    private void activarCliente(Cliente cliente) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/renovacion.fxml"));
            Parent root = loader.load();

            RenovacionController controller = loader.getController();
            controller.setClienteInactivo(cliente);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Reactivar Cliente");
            stage.showAndWait();

            recargarClientes();
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo abrir renovación");
        }
    }

    private void eliminarCliente(Cliente cliente) {
        if (!SessionManager.isAdmin()) {
            mostrarAlerta("Permiso denegado", "Solo un administrador puede eliminar clientes.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/confirmar_eliminacion_dialog.fxml")
            );
            VBox dialogContent = loader.load();

            ConfirmarEliminacionDialogController controller = loader.getController();
            controller.setNombreCliente(cliente.getNombreCompleto());

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(tablaClientes.getScene().getWindow());
            dialog.initStyle(StageStyle.TRANSPARENT);
            dialog.setScene(new Scene(dialogContent));

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), dialogContent);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            controller.btnCancelar.setOnAction(e -> dialog.close());

            controller.btnEliminar.setOnAction(e -> {
                dialog.close();
                ejecutarEliminacion(cliente);
            });

            dialog.show();
            fadeIn.play();

        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo cargar el diálogo de confirmación");
        }
    }

    private void ejecutarEliminacion(Cliente cliente) {
        if (!SessionManager.isAdmin()) {
            mostrarAlerta("Permiso denegado", "Solo un administrador puede eliminar clientes.");
            return;
        }
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);

            Integer clienteId = null;
            String sqlId = "SELECT id FROM clientes WHERE telefono = ?";
            try (PreparedStatement stmtId = conn.prepareStatement(sqlId)) {
                stmtId.setString(1, cliente.getTelefono());
                ResultSet rs = stmtId.executeQuery();
                if (rs.next()) {
                    clienteId = rs.getInt("id");
                }
            }

            String deletePagos = "DELETE FROM pagos WHERE cliente_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deletePagos)) {
                stmt.setInt(1, clienteId);
                stmt.executeUpdate();
            }

            String deleteCliente = "DELETE FROM clientes WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteCliente)) {
                stmt.setInt(1, clienteId);
                stmt.executeUpdate();
            }

            conn.commit();
            AuditoriaUtil.registrar(
                    SessionManager.getUsuarioActual().getNombre(),
                    "DELETE",
                    "CLIENTE",
                    clienteId,
                    cliente.getNombreCompleto() + " - " + cliente.getTelefono()
            );
            recargarClientes();
            mostrarToastExito("Cliente eliminado permanentemente");

        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudo eliminar cliente");
        }
    }

    private void mostrarToastExito(String mensaje) {
        try {
            Stage stage = (Stage) tablaClientes.getScene().getWindow();
            ToastController.showToast(stage, mensaje, ToastController.SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void recargarClientes() {
        tablaClientes.getItems().clear();
        cargarClientes();
        clientesOriginales.setAll(tablaClientes.getItems());
        Platform.runLater(this::ajustarAnchoColumnas);
    }

    private void configurarBusqueda() {
        txtBuscar.setPromptText("Buscar cliente...");
        txtBuscar.setStyle("-fx-font-size: 14px; -fx-padding: 8px 15px; -fx-background-radius: 20px; -fx-border-radius: 20px; -fx-border-color: #ced4da; -fx-background-color: #ffffff;");

        btnLimpiar.setStyle("-fx-background-color: #6C757D; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 20px; -fx-border-radius: 20px; -fx-cursor: hand;");
        btnLimpiar.setOnMouseEntered(e -> btnLimpiar.setStyle("-fx-background-color: #5a6268; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 20px; -fx-border-radius: 20px; -fx-cursor: hand;"));
        btnLimpiar.setOnMouseExited(e -> btnLimpiar.setStyle("-fx-background-color: #6C757D; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 20px; -fx-border-radius: 20px; -fx-cursor: hand;"));

        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            filtrarClientes();
            Platform.runLater(() -> {
                ajustarAnchoColumnas();
                tablaClientes.refresh();
                tablaClientes.requestLayout();
            });
        });
    }

    private void filtrarClientes() {
        String filtro = txtBuscar.getText().trim().toLowerCase();

        if (filtro.isEmpty()) {
            tablaClientes.setItems(clientesOriginales);
        } else {
            ObservableList<Cliente> filtrados = FXCollections.observableArrayList();
            for (Cliente cliente : clientesOriginales) {
                if (cliente.getNombreCompleto().toLowerCase().contains(filtro) || cliente.getTelefono().contains(filtro)) {
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

    private void ajustarAnchoColumnas() {
        Platform.runLater(() -> {
            double anchoTotal = tablaClientes.getWidth();
            if (anchoTotal > 0) {
                colNombreCompleto.setPrefWidth(anchoTotal * 0.52);
                colTelefono.setPrefWidth(anchoTotal * 0.15);
                colEstado.setPrefWidth(anchoTotal * 0.15);
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

    private void eliminarBarrasDesplazamiento() {
        tablaClientes.setStyle(tablaClientes.getStyle() + " -fx-scroll-bar-policy: never;");

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

    @FXML
    private void handleVolver() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/lista_clientes.fxml"));
            Stage stage = (Stage) tablaClientes.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo volver");
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
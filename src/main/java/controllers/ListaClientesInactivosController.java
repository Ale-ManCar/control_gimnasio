package controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import models.Cliente;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import util.DatabaseUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Locale;

public class ListaClientesInactivosController {

    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, String> colNombreCompleto;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colEstado;
    @FXML private TableColumn<Cliente, Void> colAcciones;
    @FXML private TextField txtBuscar;
    @FXML private Button btnLimpiar;
    @FXML private Label lblTitulo;
    @FXML private Label lblResumen;

    private static final String[] AVATAR_COLORES = {
            "#5B8DEF", "#FF8C42", "#34C759", "#FF6B6B", "#A55EEA", "#20CFC3"
    };
    private static final Locale LOCALE_ES = new Locale("es", "ES");

    private final ObservableList<Cliente> clientesOriginales = FXCollections.observableArrayList();
    private String estiloOriginalTabla;

    @FXML
    public void initialize() {
        configurarEstilosGlobales();
        configurarColumnas();
        cargarClientes();
        configurarBusqueda();
        ajustarAnchoColumnas();
        eliminarBarrasDesplazamiento();
        configurarFilas();

        estiloOriginalTabla = tablaClientes.getStyle();
        actualizarResumen();
    }

    private void configurarEstilosGlobales() {
        lblTitulo.setStyle(
                "-fx-font-size: 26px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #f5f7ff;" +
                        "-fx-padding: 0 0 6px 0;"
        );

        if (lblResumen != null) {
            lblResumen.setStyle(
                    "-fx-text-fill: rgba(255,255,255,0.78);" +
                            "-fx-font-size: 14px;" +
                            "-fx-font-weight: semi-bold;"
            );
        }

        tablaClientes.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-background-color: rgba(255,255,255,0.12);" +
                        "-fx-border-radius: 18px;" +
                        "-fx-background-radius: 18px;" +
                        "-fx-padding: 6px;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 18, 0, 0, 0);"
        );

        Label placeholder = new Label("No se encontraron clientes inactivos con los filtros aplicados.");
        placeholder.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 14px; -fx-padding: 20px;");
        tablaClientes.setPlaceholder(placeholder);
    }

    private void configurarColumnas() {
        colNombreCompleto.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefonoVisible"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        colNombreCompleto.setCellFactory(column -> new TableCell<>() {
            private final Circle avatarCircle = new Circle(18);
            private final Label iniciales = new Label();
            private final StackPane avatar = new StackPane();
            private final Label nombre = new Label();
            private final Label detalle = new Label();
            private final VBox textContainer = new VBox(nombre, detalle);
            private final HBox content = new HBox(12, avatar, textContainer);

            {
                avatar.getChildren().addAll(avatarCircle, iniciales);
                avatar.setPrefSize(36, 36);
                avatarCircle.setSmooth(true);
                iniciales.setTextFill(Color.WHITE);
                iniciales.setFont(Font.font("Arial", FontWeight.BOLD, 13));

                textContainer.setAlignment(Pos.CENTER_LEFT);
                textContainer.setSpacing(2);

                nombre.setStyle("-fx-font-weight: bold; -fx-text-fill: #f0f4ff; -fx-font-size: 15px;");
                detalle.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 12px;");

                content.setAlignment(Pos.CENTER_LEFT);
                content.setPadding(new Insets(4, 0, 4, 0));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Cliente cliente = getTableRow() != null ? getTableRow().getItem() : null;
                String nombreCompleto = cliente != null ? cliente.getNombreCompleto() : item;
                nombre.setText(nombreCompleto != null ? nombreCompleto : "");

                String membresia = cliente != null ? cliente.getTipoMembresia() : "";
                String fechaVencimiento = cliente != null ? cliente.getFecha_vencimiento() : "";
                if (membresia == null || membresia.isBlank()) {
                    detalle.setText(fechaVencimiento != null && !fechaVencimiento.isBlank()
                            ? "Venció: " + fechaVencimiento
                            : "Sin membresía asignada");
                } else {
                    String resumen = membresia.toUpperCase(LOCALE_ES);
                    if (fechaVencimiento != null && !fechaVencimiento.isBlank()) {
                        resumen += " • Venció: " + fechaVencimiento;
                    }
                    detalle.setText(resumen);
                }

                String inicial = nombreCompleto != null && !nombreCompleto.isBlank()
                        ? nombreCompleto.substring(0, 1).toUpperCase(LOCALE_ES)
                        : "?";
                iniciales.setText(inicial);
                avatarCircle.setFill(Color.web(obtenerColorAvatar(nombreCompleto)));

                setGraphic(content);
                setText(null);
                setStyle("-fx-alignment: CENTER_LEFT;");
            }
        });

        colTelefono.setCellFactory(column -> new TableCell<>() {
            private final FontIcon iconoTelefono = new FontIcon(FontAwesomeSolid.PHONE);
            private final Label telefono = new Label();
            private final HBox content = new HBox(8, iconoTelefono, telefono);

            {
                iconoTelefono.setIconColor(Color.web("#70a1ff"));
                iconoTelefono.setIconSize(16);
                telefono.setStyle("-fx-text-fill: #f8f9ff; -fx-font-weight: semi-bold;");
                content.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    telefono.setText(formatearTelefono(item));
                    setGraphic(content);
                    setText(null);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        colEstado.setCellFactory(column -> new TableCell<>() {
            private final Label estadoLabel = new Label();
            private final HBox container = new HBox(estadoLabel);

            {
                estadoLabel.setPadding(new Insets(4, 14, 4, 14));
                estadoLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                container.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    estadoLabel.setText(estado);
                    estadoLabel.setStyle("-fx-background-color: rgba(220,53,69,0.22); -fx-text-fill: #ff6b6b; -fx-background-radius: 999; -fx-font-size: 12px;");
                    setGraphic(container);
                    setText(null);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnActivar = new Button();
            private final Button btnEliminar = new Button();
            private final FontIcon iconoActivar = new FontIcon(FontAwesomeSolid.REDO);
            private final FontIcon iconoEliminar = new FontIcon(FontAwesomeSolid.TRASH);
            private final HBox container = new HBox(8, btnActivar, btnEliminar);

            {
                iconoActivar.setIconColor(Color.web("#34C759"));
                iconoActivar.setIconSize(18);
                btnActivar.setGraphic(iconoActivar);
                btnActivar.setPadding(new Insets(6));
                btnActivar.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-background-radius: 10px;");
                btnActivar.setTooltip(new Tooltip("Reactivar cliente"));

                btnActivar.setOnMouseEntered(e ->
                        btnActivar.setStyle("-fx-background-color: rgba(52,199,89,0.18); -fx-cursor: hand; -fx-background-radius: 10px;")
                );
                btnActivar.setOnMouseExited(e ->
                        btnActivar.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-background-radius: 10px;")
                );

                btnActivar.setOnAction(event -> {
                    Cliente cliente = getTableView().getItems().get(getIndex());
                    activarCliente(cliente);
                });

                iconoEliminar.setIconColor(Color.web("#ff6b6b"));
                iconoEliminar.setIconSize(18);
                btnEliminar.setGraphic(iconoEliminar);
                btnEliminar.setPadding(new Insets(6));
                btnEliminar.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-background-radius: 10px;");
                btnEliminar.setTooltip(new Tooltip("Eliminar cliente permanentemente"));

                btnEliminar.setOnMouseEntered(e ->
                        btnEliminar.setStyle("-fx-background-color: rgba(255,107,107,0.18); -fx-cursor: hand; -fx-background-radius: 10px;")
                );
                btnEliminar.setOnMouseExited(e ->
                        btnEliminar.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-background-radius: 10px;")
                );

                btnEliminar.setOnAction(event -> {
                    Cliente cliente = getTableView().getItems().get(getIndex());
                    eliminarCliente(cliente);
                });

                container.setAlignment(Pos.CENTER);
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
        String sql = "SELECT nombres, apellidos, telefono, telefono_visible, tipoMembresia, fecha_vencimiento " +
                "FROM clientes WHERE activo = 0";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            ObservableList<Cliente> clientesTemp = FXCollections.observableArrayList();

            while (rs.next()) {
                Cliente cliente = new Cliente(
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("telefono"),
                        rs.getString("telefono_visible"),
                        rs.getString("tipoMembresia"),
                        LocalDate.parse(rs.getString("fecha_vencimiento"))
                );
                clientesTemp.add(cliente);
            }

            clientesTemp.sort(Comparator.comparing(Cliente::getNombreCompleto, String.CASE_INSENSITIVE_ORDER));
            tablaClientes.getItems().setAll(clientesTemp);
            clientesOriginales.setAll(tablaClientes.getItems());
            actualizarResumen();
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
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);

            String deletePagos = "DELETE FROM pagos WHERE cliente_id = (SELECT id FROM clientes WHERE telefono = ?)";
            try (PreparedStatement stmt = conn.prepareStatement(deletePagos)) {
                stmt.setString(1, cliente.getTelefono());
                stmt.executeUpdate();
            }

            String deleteCliente = "DELETE FROM clientes WHERE telefono = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteCliente)) {
                stmt.setString(1, cliente.getTelefono());
                stmt.executeUpdate();
            }

            conn.commit();
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
        Platform.runLater(() -> {
            ajustarAnchoColumnas();
            actualizarResumen();
        });
    }

    private void configurarBusqueda() {
        txtBuscar.setPromptText("Buscar por nombre, teléfono o membresía");
        txtBuscar.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-padding: 10px 18px;" +
                        "-fx-background-radius: 24px;" +
                        "-fx-border-radius: 24px;" +
                        "-fx-border-color: rgba(255,255,255,0.25);" +
                        "-fx-border-width: 1px;" +
                        "-fx-background-color: rgba(255,255,255,0.12);" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: rgba(255,255,255,0.55);"
        );

        btnLimpiar.setStyle(
                "-fx-background-color: rgba(255,255,255,0.14);" +
                        "-fx-text-fill: #f5f7ff;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8px 18px;" +
                        "-fx-background-radius: 22px;" +
                        "-fx-border-radius: 22px;" +
                        "-fx-border-color: rgba(255,255,255,0.25);" +
                        "-fx-border-width: 1px;" +
                        "-fx-cursor: hand;"
        );
        btnLimpiar.setOnMouseEntered(e ->
                btnLimpiar.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.28);" +
                                "-fx-text-fill: #111320;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 8px 18px;" +
                                "-fx-background-radius: 22px;" +
                                "-fx-border-radius: 22px;" +
                                "-fx-border-color: rgba(255,255,255,0.35);" +
                                "-fx-border-width: 1px;" +
                                "-fx-cursor: hand;"
                )
        );
        btnLimpiar.setOnMouseExited(e ->
                btnLimpiar.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.14);" +
                                "-fx-text-fill: #f5f7ff;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 8px 18px;" +
                                "-fx-background-radius: 22px;" +
                                "-fx-border-radius: 22px;" +
                                "-fx-border-color: rgba(255,255,255,0.25);" +
                                "-fx-border-width: 1px;" +
                                "-fx-cursor: hand;"
                )
        );

        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> filtrarClientes());
    }

    private void filtrarClientes() {
        String filtro = txtBuscar.getText().trim().toLowerCase();

        if (filtro.isEmpty()) {
            tablaClientes.setItems(clientesOriginales);
        } else {
            ObservableList<Cliente> filtrados = FXCollections.observableArrayList();
            for (Cliente cliente : clientesOriginales) {
                if (cliente.getNombreCompleto().toLowerCase().contains(filtro) || cliente.getTelefonoVisible().contains(filtro)) {
                    filtrados.add(cliente);
                }
            }

            filtrados.sort(Comparator.comparing(Cliente::getNombreCompleto, String.CASE_INSENSITIVE_ORDER));
            tablaClientes.setItems(filtrados);
        }

        tablaClientes.refresh();
        tablaClientes.requestLayout();
        ajustarAnchoColumnas();
        actualizarResumen();
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
        actualizarResumen();
    }

    private void configurarFilas() {
        tablaClientes.setRowFactory(tv -> {
            TableRow<Cliente> row = new TableRow<>() {
                @Override
                protected void updateItem(Cliente cliente, boolean empty) {
                    super.updateItem(cliente, empty);

                    if (empty || cliente == null) {
                        setBackground(null);
                        setGraphic(null);
                        setStyle("");
                        setTooltip(null);
                    } else {
                        Tooltip tooltip = new Tooltip(cliente.getTooltipText());
                        tooltip.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                        setTooltip(tooltip);

                        String baseColor = obtenerColorFila(getIndex());
                        String estiloBase = "-fx-background-color: " + baseColor + "; " +
                                "-fx-border-color: transparent; " +
                                "-fx-border-width: 0 0 1px 0;";

                        if (isSelected()) {
                            setStyle("-fx-background-color: rgba(255,99,132,0.25); " +
                                    "-fx-border-color: transparent; " +
                                    "-fx-border-width: 0 0 1px 0;");
                        } else if (isHover()) {
                            setStyle("-fx-background-color: rgba(255,99,132,0.18); " +
                                    "-fx-border-color: transparent; " +
                                    "-fx-border-width: 0 0 1px 0;");
                        } else {
                            setStyle(estiloBase);
                        }
                    }
                }
            };

            row.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    row.setStyle("-fx-background-color: rgba(255,99,132,0.25); " +
                            "-fx-border-color: transparent; " +
                            "-fx-border-width: 0 0 1px 0;");
                } else {
                    if (row.isHover()) {
                        row.setStyle("-fx-background-color: rgba(255,99,132,0.18); " +
                                "-fx-border-color: transparent; " +
                                "-fx-border-width: 0 0 1px 0;");
                    } else {
                        row.setStyle("-fx-background-color: " + obtenerColorFila(row.getIndex()) + "; " +
                                "-fx-border-color: transparent; " +
                                "-fx-border-width: 0 0 1px 0;");
                    }
                }
            });

            row.hoverProperty().addListener((obs, oldVal, isHovering) -> {
                if (isHovering && !row.isSelected()) {
                    row.setStyle("-fx-background-color: rgba(255,99,132,0.18); " +
                            "-fx-border-color: transparent; " +
                            "-fx-border-width: 0 0 1px 0;");
                } else if (!row.isSelected()) {
                    row.setStyle("-fx-background-color: " + obtenerColorFila(row.getIndex()) + "; " +
                            "-fx-border-color: transparent; " +
                            "-fx-border-width: 0 0 1px 0;");
                }
            });

            return row;
        });
    }

    private String obtenerColorFila(int index) {
        if (index < 0) {
            return "transparent";
        }
        return index % 2 == 0 ? "rgba(255,107,107,0.12)" : "rgba(255,255,255,0.04)";
    }

    private String obtenerColorAvatar(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return AVATAR_COLORES[0];
        }
        int index = Math.abs(nombre.hashCode()) % AVATAR_COLORES.length;
        return AVATAR_COLORES[index];
    }

    private String formatearTelefono(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            return "Sin teléfono";
        }

        String soloDigitos = telefono.replaceAll("[^0-9]", "");
        if (!soloDigitos.isEmpty()) {
            return soloDigitos;
        }
        return telefono.trim();
    }

    private void ajustarAnchoColumnas() {
        Platform.runLater(() -> {
            double anchoTotal = tablaClientes.getWidth();
            if (anchoTotal > 0) {
                colNombreCompleto.setPrefWidth(anchoTotal * 0.45);
                colTelefono.setPrefWidth(anchoTotal * 0.20);
                colEstado.setPrefWidth(anchoTotal * 0.18);
                colAcciones.setPrefWidth(anchoTotal * 0.12);
                tablaClientes.requestLayout();
            }
        });

        tablaClientes.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0) {
                double anchoTotal = newVal.doubleValue();
                colNombreCompleto.setPrefWidth(anchoTotal * 0.45);
                colTelefono.setPrefWidth(anchoTotal * 0.25);
                colEstado.setPrefWidth(anchoTotal * 0.18);
                colAcciones.setPrefWidth(anchoTotal * 0.12);
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
                if (node instanceof ScrollPane) {
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

    private void actualizarResumen() {
        if (lblResumen == null) {
            return;
        }

        int totalInactivos = clientesOriginales.size();
        int visibles = tablaClientes.getItems() != null ? tablaClientes.getItems().size() : 0;

        if (visibles == totalInactivos) {
            lblResumen.setText(String.format("Clientes inactivos: %d", totalInactivos));
        } else {
            lblResumen.setText(String.format("Clientes inactivos: %d • Mostrando: %d", totalInactivos, visibles));
        }
    }
}

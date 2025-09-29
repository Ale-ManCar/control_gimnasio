package controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
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
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
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
    @FXML private Label lblResumen;

    private static final String[] AVATAR_COLORES = {
            "#5B8DEF", "#FF8C42", "#34C759", "#FF6B6B", "#A55EEA", "#20CFC3"
    };
    private static final Locale LOCALE_ES = new Locale("es", "ES");
    private static final String PREFIJO_ASISTENCIA = "asistencia_";
    private final ObservableList<Cliente> clientesOriginales = FXCollections.observableArrayList();
    private Cliente clienteEditado;
    private String estiloOriginalTabla;
    private final Preferences preferencias = Preferences.userNodeForPackage(ListaClientesController.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarEstilosGlobales();
        configurarColumnas();
        cargarClientes();
        configurarBusqueda();

        estiloOriginalTabla = tablaClientes.getStyle();
        configurarFilas();
        ajustarAnchoColumnas();
        eliminarBarrasDesplazamiento();
        actualizarResumen();
    }

    private void configurarEstilosGlobales() {
        lblTitulo.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #f5f7ff;" +
                        "-fx-padding: 0 0 6px 0;"
        );

        lblResumen.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.78);" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: semi-bold;"
        );

        tablaClientes.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-background-color: rgba(255,255,255,0.12);" +
                        "-fx-border-radius: 18px;" +
                        "-fx-background-radius: 18px;" +
                        "-fx-padding: 6px;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 18, 0, 0, 0);"
        );

        Label placeholder = new Label("No se encontraron clientes activos con los filtros aplicados.");
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
            private Cliente clienteActual;
            private final ChangeListener<Boolean> asistenciaListener = (obs, oldVal, newVal) -> aplicarEstiloAsistencia(newVal);

            {
                avatar.getChildren().addAll(avatarCircle, iniciales);
                avatar.setPrefSize(36, 36);
                avatarCircle.setSmooth(true);
                iniciales.setTextFill(Color.WHITE);
                iniciales.setFont(Font.font("Arial", FontWeight.BOLD, 13));

                textContainer.setAlignment(Pos.CENTER_LEFT);
                textContainer.setSpacing(2);

                detalle.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 12px;");

                content.setAlignment(Pos.CENTER_LEFT);
                content.setPadding(new Insets(4, 0, 4, 0));
            }

            private void aplicarEstiloAsistencia(boolean asistio) {
                if (asistio) {
                    nombre.setStyle("-fx-font-weight: bold; -fx-text-fill: #34C759; -fx-font-size: 15px;");
                } else {
                    nombre.setStyle("-fx-font-weight: bold; -fx-text-fill: #f0f4ff; -fx-font-size: 15px;");
                }
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (clienteActual != null) {
                    clienteActual.asistioHoyProperty().removeListener(asistenciaListener);
                    clienteActual = null;
                }

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Cliente cliente = getTableRow().getItem();
                    String nombreCompleto = cliente != null ? cliente.getNombreCompleto() : item;
                    nombre.setText(nombreCompleto);

                    String membresia = cliente != null ? cliente.getTipoMembresia() : "";
                    if (membresia == null || membresia.isBlank()) {
                        detalle.setText("Sin membresía asignada");
                    } else {
                        detalle.setText(membresia.toUpperCase(LOCALE_ES));
                    }

                    String inicial = nombreCompleto != null && !nombreCompleto.isBlank()
                            ? nombreCompleto.substring(0, 1).toUpperCase(LOCALE_ES)
                            : "?";
                    iniciales.setText(inicial);
                    avatarCircle.setFill(Color.web(obtenerColorAvatar(nombreCompleto)));

                    if (cliente != null) {
                        aplicarEstiloAsistencia(cliente.isAsistioHoy());
                        cliente.asistioHoyProperty().addListener(asistenciaListener);
                        clienteActual = cliente;
                    } else {
                        aplicarEstiloAsistencia(false);
                    }

                    setGraphic(content);
                    setText(null);
                    setStyle("-fx-alignment: CENTER_LEFT;");
                }
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
                    if ("Activo".equalsIgnoreCase(estado)) {
                        estadoLabel.setStyle("-fx-background-color: rgba(46,204,113,0.18); -fx-text-fill: #2ecc71; -fx-background-radius: 999; -fx-font-size: 12px;");
                    } else {
                        estadoLabel.setStyle("-fx-background-color: rgba(220,53,69,0.22); -fx-text-fill: #ff6b6b; -fx-background-radius: 999; -fx-font-size: 12px;");
                    }
                    setGraphic(container);
                    setText(null);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button();
            private final Button btnCheck = new Button();
            private final FontIcon iconoEditar = new FontIcon(FontAwesomeSolid.EDIT);
            private final FontIcon iconoCheck = new FontIcon(FontAwesomeSolid.CHECK);
            private final HBox contenedor = new HBox(8, btnCheck, btnEditar);

            {
                iconoEditar.setIconColor(Color.web("#4a6cf7"));
                iconoEditar.setIconSize(18);
                btnEditar.setGraphic(iconoEditar);
                btnEditar.setPadding(new Insets(6));
                btnEditar.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-background-radius: 10px;");
                btnEditar.setTooltip(new Tooltip("Editar cliente"));

                btnEditar.setOnMouseEntered(e ->
                        btnEditar.setStyle("-fx-background-color: rgba(74,108,247,0.18); -fx-cursor: hand; -fx-background-radius: 10px;")
                );
                btnEditar.setOnMouseExited(e ->
                        btnEditar.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-background-radius: 10px;")
                );

                btnEditar.setOnAction(event -> {
                    Cliente cliente = getTableView().getItems().get(getIndex());
                    if (cliente != null) {
                        mostrarDialogoEdicion(cliente);
                    }
                });

                iconoCheck.setIconSize(18);
                btnCheck.setGraphic(iconoCheck);
                btnCheck.setPadding(new Insets(6));
                btnCheck.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-background-radius: 10px;");
                btnCheck.setTooltip(new Tooltip("Registrar asistencia diaria"));

                btnCheck.setOnMouseEntered(e ->
                        btnCheck.setStyle("-fx-background-color: rgba(52,199,89,0.18); -fx-cursor: hand; -fx-background-radius: 10px;")
                );
                btnCheck.setOnMouseExited(e ->
                        btnCheck.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-background-radius: 10px;")
                );

                btnCheck.setOnAction(event -> {
                    Cliente cliente = getTableView().getItems().get(getIndex());
                    if (cliente != null) {
                        boolean nuevoEstado = !cliente.isAsistioHoy();
                        cliente.setAsistioHoy(nuevoEstado);
                        actualizarRegistroAsistencia(cliente);
                        actualizarIconoAsistencia(cliente);
                        getTableView().refresh();
                    }
                });

                contenedor.setAlignment(Pos.CENTER);
            }

            private void actualizarIconoAsistencia(Cliente cliente) {
                if (cliente != null && cliente.isAsistioHoy()) {
                    iconoCheck.setIconColor(Color.web("#34C759"));
                } else {
                    iconoCheck.setIconColor(Color.web("#a5b1c2"));
                }
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Cliente cliente = getTableView().getItems().get(getIndex());
                    actualizarIconoAsistencia(cliente);
                    setGraphic(contenedor);
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

                        String baseColor = obtenerColorFila(getIndex());
                        String estiloBase = "-fx-background-color: " + baseColor + "; "
                                + "-fx-border-color: transparent; "
                                + "-fx-border-width: 0 0 1px 0;";

                        if (isSelected()) {
                            setStyle("-fx-background-color: rgba(46,139,255,0.25); "
                                    + "-fx-border-color: transparent; "
                                    + "-fx-border-width: 0 0 1px 0;");
                        } else if (isHover()) {
                            setStyle("-fx-background-color: rgba(74,108,247,0.25); "
                                    + "-fx-border-color: transparent; "
                                    + "-fx-border-width: 0 0 1px 0;");
                        } else {
                            setStyle(estiloBase);
                        }
                    }
                }
            };

            row.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    row.setStyle("-fx-background-color: rgba(46,139,255,0.25); "
                            + "-fx-border-color: transparent; "
                            + "-fx-border-width: 0 0 1px 0;");
                } else {
                    if (row.isHover()) {
                        row.setStyle("-fx-background-color: rgba(74,108,247,0.25); "
                                + "-fx-border-color: transparent; "
                                + "-fx-border-width: 0 0 1px 0;");
                    } else {
                        row.setStyle("-fx-background-color: " + obtenerColorFila(row.getIndex()) + "; "
                                + "-fx-border-color: transparent; "
                                + "-fx-border-width: 0 0 1px 0;");
                    }
                }
            });

            row.hoverProperty().addListener((obs, oldVal, isHovering) -> {
                if (isHovering && !row.isSelected()) {
                    row.setStyle("-fx-background-color: rgba(74,108,247,0.25); "
                            + "-fx-border-color: transparent; "
                            + "-fx-border-width: 0 0 1px 0;");
                } else if (!row.isSelected()) {
                    row.setStyle("-fx-background-color: " + obtenerColorFila(row.getIndex()) + "; "
                            + "-fx-border-color: transparent; "
                            + "-fx-border-width: 0 0 1px 0;");
                }
            });

            return row;
        });
    }

    private String obtenerColorFila(int index) {
        if (index < 0) {
            return "transparent";
        }
        return index % 2 == 0 ? "rgba(74,108,247,0.08)" : "rgba(255,255,255,0.04)";
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

    private boolean tieneAsistenciaRegistradaHoy(Cliente cliente) {
        if (cliente == null) {
            return false;
        }

        String clave = generarClaveAsistencia(cliente);
        String fechaGuardada = preferencias.get(clave, null);

        if (fechaGuardada == null) {
            return false;
        }

        try {
            LocalDate fecha = LocalDate.parse(fechaGuardada);
            if (fecha.isEqual(LocalDate.now())) {
                return true;
            } else {
                preferencias.remove(clave);
            }
        } catch (DateTimeParseException ex) {
            preferencias.remove(clave);
        }

        return false;
    }

    private void actualizarRegistroAsistencia(Cliente cliente) {
        if (cliente == null) {
            return;
        }

        String clave = generarClaveAsistencia(cliente);
        if (cliente.isAsistioHoy()) {
            preferencias.put(clave, LocalDate.now().toString());
        } else {
            preferencias.remove(clave);
        }
    }

    private String generarClaveAsistencia(Cliente cliente) {
        String identificador = cliente.getTelefono();
        if (identificador == null || identificador.isBlank()) {
            identificador = cliente.getNombreCompleto();
        }
        if (identificador == null || identificador.isBlank()) {
            identificador = "sin_datos";
        }

        String limpio = identificador.toLowerCase(LOCALE_ES).replaceAll("[^a-z0-9]", "_");
        return PREFIJO_ASISTENCIA + limpio;
    }

    private void actualizarResumen() {
        if (lblResumen == null) {
            return;
        }

        int totalActivos = clientesOriginales.size();
        int visibles = tablaClientes.getItems() != null ? tablaClientes.getItems().size() : 0;

        if (visibles == totalActivos) {
            lblResumen.setText(String.format("Clientes activos: %d", totalActivos));
        } else {
            lblResumen.setText(String.format("Clientes activos: %d • Mostrando: %d", totalActivos, visibles));
        }
    }

    @FXML
    private void filtrarClientes() {
        String filtro = txtBuscar.getText().trim().toLowerCase();
        String filtroNumerico = filtro.replaceAll("[^0-9]", "");

        if (filtro.isEmpty()) {
            tablaClientes.setItems(clientesOriginales);
        } else {
            ObservableList<Cliente> filtrados = FXCollections.observableArrayList();
            for (Cliente cliente : clientesOriginales) {
                String nombre = cliente.getNombreCompleto().toLowerCase();
                String telefono = cliente.getTelefonoVisible() != null ? cliente.getTelefonoVisible().toLowerCase() : "";
                String telefonoNumerico = telefono.replaceAll("[^0-9]", "");
                String membresia = cliente.getTipoMembresia() != null ? cliente.getTipoMembresia().toLowerCase() : "";

                boolean coincideNombre = nombre.contains(filtro);
                boolean coincideTelefono = telefono.contains(filtro) ||
                        (!filtroNumerico.isEmpty() && telefonoNumerico.contains(filtroNumerico));
                boolean coincideMembresia = membresia.contains(filtro);

                if (coincideNombre || coincideTelefono || coincideMembresia) {
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
        String sql = "SELECT nombres, apellidos, telefono, telefono_visible, tipoMembresia, fecha_vencimiento " +
                "FROM clientes WHERE activo = 1 " +
                "AND COALESCE(LOWER(tipoMembresia), '') <> 'diario'";

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
                cliente.setAsistioHoy(tieneAsistenciaRegistradaHoy(cliente));
                clientesTemp.add(cliente);
            }

            clientesTemp.sort(Comparator.comparing(Cliente::getNombreCompleto, String.CASE_INSENSITIVE_ORDER));
            tablaClientes.getItems().setAll(clientesTemp);
            clientesOriginales.setAll(clientesTemp);
            actualizarResumen();

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

package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.paint.Color;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import models.Cliente;
import util.AuditoriaScheduler;
import util.DatabaseUtil;
import util.EventBus;
import util.ReporteUtil;
import models.Turno;
import util.SessionManager;
import models.Role;
import models.User;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class RecepcionistaDashboardController implements Initializable {

    @FXML private AnchorPane cardClientes;
    @FXML private TableColumn<Cliente, String> colMembresia;
    @FXML private AnchorPane cardPagos;
    @FXML private AnchorPane cardVencimientos;
    @FXML private TableView<Cliente> tablaClientesProximosAVencer;
    @FXML private Label lblMensaje;
    @FXML private Label lblRecepcionista;
//  @FXML private Button btnVerTodos;

    @FXML private TableColumn<Cliente, String> colCliente;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colVencimiento;
    @FXML private TableColumn<Cliente, Integer> colDiasRestantes;
    @FXML private TableColumn<Cliente, Void> colAlerta;
    @FXML private TableColumn<Cliente, Void> colAccion;

    private static final DateTimeFormatter FORMATO_FECHA_AMIGABLE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", new Locale("es", "ES"));

    private MetricCardController ctrlClientes;
    private MetricCardController ctrlPagos;
    private MetricCardController ctrlVencimientos;

    private Consumer<EventBus.EventType> dashboardListener;
    private Turno turnoActual;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!SessionManager.tienePermiso(Role.RECEPCIONISTA)) {
            lblMensaje.setText("Acceso denegado");
            return;
        }

        actualizarTituloRecepcionista();
        try {

            Platform.runLater(() -> {
                Stage stage = (Stage) cardClientes.getScene().getWindow();

                dashboardListener = eventType -> {
                    if (eventType == EventBus.EventType.EGRESO_REGISTRADO ||
                            eventType == EventBus.EventType.DATOS_ACTUALIZADOS ||
                            eventType == EventBus.EventType.VENTA_REALIZADA) {
                        Platform.runLater(this::cargarDatosTarjetas);
                    }
                };
                EventBus.registerListener(EventBus.EventType.EGRESO_REGISTRADO, dashboardListener);
                EventBus.registerListener(EventBus.EventType.DATOS_ACTUALIZADOS, dashboardListener);
                EventBus.registerListener(EventBus.EventType.VENTA_REALIZADA, dashboardListener);

                stage.setOnCloseRequest(e -> {
                    EventBus.unregisterListener(EventBus.EventType.EGRESO_REGISTRADO, dashboardListener);
                    EventBus.unregisterListener(EventBus.EventType.DATOS_ACTUALIZADOS, dashboardListener);
                    EventBus.unregisterListener(EventBus.EventType.VENTA_REALIZADA, dashboardListener);
                });
            });

            configurarTablaSinScroll();

            colCliente.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
            colMembresia.setCellValueFactory(new PropertyValueFactory<>("tipoMembresia"));
            colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefonoVisible"));
            colVencimiento.setCellValueFactory(new PropertyValueFactory<>("fecha_vencimiento"));
            colDiasRestantes.setCellValueFactory(new PropertyValueFactory<>("diasRestantes"));

            configurarColumnasPersonalizadas();

            Label placeholder = new Label("No hay clientes próximos a vencer en los próximos 7 días.");
            placeholder.getStyleClass().add("table-placeholder-label");
            tablaClientesProximosAVencer.setPlaceholder(placeholder);
            inicializarTarjetasMetricas();
            cargarDatosTarjetas();
            cargarClientesProximosAVencer();

            final PseudoClass statusSafe = PseudoClass.getPseudoClass("status-safe");
            final PseudoClass statusApproaching = PseudoClass.getPseudoClass("status-approaching");
            final PseudoClass statusUrgent = PseudoClass.getPseudoClass("status-urgent");
            final PseudoClass statusExpired = PseudoClass.getPseudoClass("status-expired");

            tablaClientesProximosAVencer.setRowFactory(tv -> new TableRow<>() {
                @Override
                protected void updateItem(Cliente cliente, boolean empty) {
                    super.updateItem(cliente, empty);

                    pseudoClassStateChanged(statusSafe, false);
                    pseudoClassStateChanged(statusApproaching, false);
                    pseudoClassStateChanged(statusUrgent, false);
                    pseudoClassStateChanged(statusExpired, false);

                    if (empty || cliente == null) {
                        setTooltip(null);
                    } else {
                        int dias = cliente.getDiasRestantes();

                        if (dias >= 5) {
                            pseudoClassStateChanged(statusSafe, true);
                        } else if (dias >= 3) {
                            pseudoClassStateChanged(statusApproaching, true);
                        } else if (dias >= 1) {
                            pseudoClassStateChanged(statusUrgent, true);
                        } else {
                            pseudoClassStateChanged(statusExpired, true);
                        }

                        Tooltip tooltip = new Tooltip(cliente.getTooltipText());
                        tooltip.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; " +
                                "-fx-background-color: rgba(15,23,42,0.92); -fx-text-fill: #e2e8f0; " +
                                "-fx-padding: 10 12; -fx-background-radius: 10; -fx-border-radius: 10; " +
                                "-fx-border-color: rgba(148,163,184,0.45);");
                        setTooltip(tooltip);
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            lblMensaje.setText("Error al inicializar el panel.");
        }
        EventBus.registerListener(this::cargarDatosTarjetas);
        iniciarTurno();
    }

    private void actualizarTituloRecepcionista() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && lblRecepcionista != null) {
            lblRecepcionista.setText(currentUser.getUsername());
        }
    }

    private void configurarTablaSinScroll() {
        tablaClientesProximosAVencer.setFixedCellSize(44);
        tablaClientesProximosAVencer.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        double alturaVisible = calcularAlturaTabla(0);
        tablaClientesProximosAVencer.setPrefHeight(alturaVisible);
        tablaClientesProximosAVencer.setMinHeight(alturaVisible);
        tablaClientesProximosAVencer.setMaxHeight(alturaVisible);
    }

    private void iniciarTurno() {
        try {
            if (SessionManager.getCurrentUser() != null) {
                int usuarioId = SessionManager.getCurrentUser().getId();
                turnoActual = DatabaseUtil.obtenerTurnoActivo(usuarioId);
                if (turnoActual != null && cerrarTurnoAnteriorSiCorresponde(turnoActual)) {
                    SessionManager.setTurnoId(-1);
                    turnoActual = DatabaseUtil.obtenerTurnoActivo(usuarioId);
                }
                if (turnoActual == null) {
                    boolean turnoReabierto = false;
                    Turno ultimoTurno = DatabaseUtil.obtenerUltimoTurnoFinalizado(usuarioId);
                    if (ultimoTurno != null) {
                        String fechaFinStr = ultimoTurno.getFecha_fin();
                        if (fechaFinStr != null && !fechaFinStr.trim().isEmpty()) {
                            try {
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                LocalDate fechaFin = LocalDateTime.parse(fechaFinStr, formatter).toLocalDate();
                                if (fechaFin.isEqual(LocalDate.now())) {
                                    DatabaseUtil.reabrirTurno(ultimoTurno.getId());
                                    turnoActual = DatabaseUtil.obtenerTurnoPorId(ultimoTurno.getId());
                                    turnoReabierto = true;
                                }
                            } catch (DateTimeParseException e) {
                                // Si no se puede parsear la fecha, se iniciará un nuevo turno más adelante.
                            }
                        }
                    }
                    if (!turnoReabierto) {
                        int id = DatabaseUtil.iniciarTurno(usuarioId);
                        turnoActual = DatabaseUtil.obtenerTurnoPorId(id);
                    }
                }
                if (turnoActual != null) {
                    SessionManager.setTurnoId(turnoActual.getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean cerrarTurnoAnteriorSiCorresponde(Turno turno) {
        LocalDateTime inicio = DatabaseUtil.parseDateTime(turno.getFecha_inicio());
        if (inicio == null) {
            return false;
        }
        LocalDate fechaInicio = inicio.toLocalDate();
        LocalDate hoy = LocalDate.now();
        if (!fechaInicio.isBefore(hoy)) {
            return false;
        }

        LocalDateTime cierreEstimado = fechaInicio.plusDays(1).atStartOfDay().minusSeconds(1);
        LocalDateTime ahora = LocalDateTime.now();
        if (cierreEstimado.isAfter(ahora)) {
            cierreEstimado = ahora;
        }

        double ingresosVentas = DatabaseUtil.obtenerTotalVentasDesde(inicio, cierreEstimado);
        double ingresosClientes = 0.0;
        try {
            Map<String, Number> resumen = DatabaseUtil.obtenerIngresosPagos(turno.getUsuario_id(), inicio, cierreEstimado);
            Number total = resumen.get("total");
            if (total != null) {
                ingresosClientes = total.doubleValue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String stockFinal = DatabaseUtil.obtenerStockJson();
        DatabaseUtil.finalizarTurno(turno.getId(), stockFinal, ingresosVentas, ingresosClientes, cierreEstimado);

        boolean cerrado = false;
        try {
            Turno turnoFinalizado = DatabaseUtil.obtenerTurnoPorId(turno.getId());
            if (turnoFinalizado != null && turnoFinalizado.getFecha_fin() != null && !turnoFinalizado.getFecha_fin().isBlank()) {
                turnoFinalizado.setStock_final(stockFinal);
                Path rutaExistente = null;
                if (turnoFinalizado.getResumenGenerado() != null && !turnoFinalizado.getResumenGenerado().isBlank()) {
                    rutaExistente = Paths.get(turnoFinalizado.getResumenGenerado());
                }
                if (rutaExistente == null) {
                    String rutaPrimerTurno = DatabaseUtil.obtenerResumenGeneradoPrimerTurnoDelDia(
                            turnoFinalizado.getUsuario_id(),
                            cierreEstimado
                    );
                    if (rutaPrimerTurno != null && !rutaPrimerTurno.isBlank()) {
                        rutaExistente = Paths.get(rutaPrimerTurno);
                    }
                }
                Path rutaGenerada = AuditoriaScheduler.generarResumenDiario(turnoFinalizado, rutaExistente);
                if (rutaGenerada != null) {
                    DatabaseUtil.marcarResumenGenerado(turnoFinalizado.getId(), rutaGenerada.toString());
                }
                cerrado = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cerrado;
    }

    @FXML
    private void handleFinalizarTurno(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/finalizar_turno.fxml"));
            Parent root = loader.load();
            FinalizarTurnoController controller = loader.getController();
            controller.setDashboardStage((Stage) ((Node) event.getSource()).getScene().getWindow());
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Finalizar turno");
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void inicializarTarjetasMetricas() throws IOException {
        FXMLLoader loaderClientes = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneClientes = loaderClientes.load();
        ctrlClientes = loaderClientes.getController();
        ctrlClientes.setTitulo("Clientes Activos");
        ctrlClientes.setIconLiteral("fas-users");
        paneClientes.prefWidthProperty().bind(cardClientes.widthProperty());
        paneClientes.prefHeightProperty().bind(cardClientes.heightProperty());
        cardClientes.getChildren().add(paneClientes);
        paneClientes.setOnMouseClicked(event -> abrirListaClientes());

        FXMLLoader loaderPagos = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane panePagos = loaderPagos.load();
        ctrlPagos = loaderPagos.getController();
        ctrlPagos.setTitulo("Pagos Recibidos");
        ctrlPagos.setIconLiteral("fas-hand-holding-usd");
        panePagos.prefWidthProperty().bind(cardPagos.widthProperty());
        panePagos.prefHeightProperty().bind(cardPagos.heightProperty());
        cardPagos.getChildren().add(panePagos);
        panePagos.setOnMouseClicked(e -> abrirPagos());

        FXMLLoader loaderVencimientos = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneVencimientos = loaderVencimientos.load();
        ctrlVencimientos = loaderVencimientos.getController();
        ctrlVencimientos.setTitulo("Próximos a Vencer");
        ctrlVencimientos.setIconLiteral("fas-hourglass-half");
        paneVencimientos.prefWidthProperty().bind(cardVencimientos.widthProperty());
        paneVencimientos.prefHeightProperty().bind(cardVencimientos.heightProperty());
        cardVencimientos.getChildren().add(paneVencimientos);
        paneVencimientos.setOnMouseClicked(e -> handleVerTodos(null));
    }

    private void configurarColumnasPersonalizadas() {
        colAlerta.setCellFactory(column -> new TableCell<Cliente, Void>() {
            private final FontIcon statusIcon = new FontIcon();

            {
                statusIcon.setIconSize(16);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Cliente cliente = getTableView().getItems().get(getIndex());
                    int dias = cliente.getDiasRestantes();

                    if (dias >= 5) {
                        statusIcon.setIconCode(FontAwesomeSolid.CHECK_CIRCLE);
                        statusIcon.setIconColor(Color.web("#22c55e"));
                    } else if (dias >= 3) {
                        statusIcon.setIconCode(FontAwesomeSolid.CLOCK);
                        statusIcon.setIconColor(Color.web("#facc15"));
                    } else if (dias >= 1) {
                        statusIcon.setIconCode(FontAwesomeSolid.EXCLAMATION_CIRCLE);
                        statusIcon.setIconColor(Color.web("#fb923c"));
                    } else {
                        statusIcon.setIconCode(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
                        statusIcon.setIconColor(Color.web("#f87171"));
                    }

                    setGraphic(statusIcon);
                }
            }
        });

        colCliente.setCellFactory(column -> new TableCell<Cliente, String>() {
            private final Label lblNombre = new Label();

            {
                lblNombre.getStyleClass().add("client-name");
                setAlignment(Pos.CENTER_LEFT);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(String nombre, boolean empty) {
                super.updateItem(nombre, empty);
                if (empty || nombre == null) {
                    setGraphic(null);
                } else {
                    lblNombre.setText(nombre);
                    setGraphic(lblNombre);
                }
            }
        });

        colTelefono.setCellFactory(column -> new TableCell<Cliente, String>() {
            private final FontIcon iconoTelefono = new FontIcon(FontAwesomeSolid.PHONE);
            private final Label lblTelefono = new Label();
            private final HBox contenedor = new HBox(8, iconoTelefono, lblTelefono);

            {
                iconoTelefono.setIconColor(Color.web("#38bdf8"));
                iconoTelefono.setIconSize(13);
                lblTelefono.getStyleClass().add("cell-primary-text");
                contenedor.getStyleClass().add("phone-cell");
                contenedor.setAlignment(Pos.CENTER_LEFT);
                setAlignment(Pos.CENTER_LEFT);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(String telefono, boolean empty) {
                super.updateItem(telefono, empty);
                if (empty || telefono == null || telefono.isBlank()) {
                    setGraphic(null);
                } else {
                    lblTelefono.setText(telefono);
                    setGraphic(contenedor);
                }
            }
        });

        colMembresia.setCellFactory(column -> new TableCell<Cliente, String>() {
            private final Label etiquetaMembresia = new Label();

            {
                etiquetaMembresia.getStyleClass().add("membership-chip");
                etiquetaMembresia.setPadding(new Insets(4, 12, 4, 12));
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(String membresia, boolean empty) {
                super.updateItem(membresia, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    String texto = (membresia == null || membresia.isBlank()) ? "Sin definir" : membresia;
                    etiquetaMembresia.setText(texto);
                    setGraphic(etiquetaMembresia);
                }
            }
        });

        colVencimiento.setCellFactory(column -> new TableCell<Cliente, String>() {
            private final Label lblFecha = new Label();

            {
                lblFecha.getStyleClass().add("cell-secondary-text");
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(String fecha, boolean empty) {
                super.updateItem(fecha, empty);
                if (empty || fecha == null || fecha.isBlank()) {
                    setGraphic(null);
                } else {
                    try {
                        LocalDate date = LocalDate.parse(fecha);
                        lblFecha.setText(date.format(FORMATO_FECHA_AMIGABLE));
                    } catch (DateTimeParseException e) {
                        lblFecha.setText(fecha);
                    }
                    setGraphic(lblFecha);
                }
            }
        });

        colDiasRestantes.setCellFactory(column -> new TableCell<Cliente, Integer>() {
            private final Label badgeDias = new Label();

            {
                badgeDias.getStyleClass().add("pill-badge");
                badgeDias.setPadding(new Insets(4, 14, 4, 14));
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(Integer dias, boolean empty) {
                super.updateItem(dias, empty);
                if (empty || dias == null) {
                    setGraphic(null);
                } else {
                    String texto;
                    String estilo = "badge-safe";

                    if (dias < 0) {
                        texto = "Vencido";
                        estilo = "badge-expired";
                    } else if (dias == 0) {
                        texto = "Vence hoy";
                        estilo = "badge-urgent";
                    } else if (dias <= 2) {
                        texto = dias + (dias == 1 ? " día" : " días");
                        estilo = "badge-urgent";
                    } else if (dias <= 4) {
                        texto = dias + " días";
                        estilo = "badge-warning";
                    } else {
                        texto = dias + " días";
                    }

                    badgeDias.getStyleClass().setAll("pill-badge", estilo);
                    badgeDias.setText(texto);
                    setGraphic(badgeDias);
                }
            }
        });

        colAccion.setCellFactory(column -> new TableCell<Cliente, Void>() {
            private final Button btnReactivar = new Button();
            private final FontIcon iconoReactivar = new FontIcon(FontAwesomeSolid.REDO);

            {
                iconoReactivar.setIconColor(Color.web("#22c55e"));
                iconoReactivar.setIconSize(16);
                btnReactivar.setGraphic(iconoReactivar);
                btnReactivar.getStyleClass().add("table-icon-button");
                btnReactivar.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                btnReactivar.setFocusTraversable(false);
                btnReactivar.setTooltip(new Tooltip("Reactivar cliente"));

                btnReactivar.setOnAction(event -> {
                    Cliente cliente = getTableView().getItems().get(getIndex());
                    abrirRenovacionConCliente(cliente);
                });

                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnReactivar);
                }
            }
        });
    }

    private void abrirListaClientes() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/lista_clientes.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Lista de Clientes Activos");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al abrir lista de clientes");
        }
    }

    private void cargarDatosTarjetas() {
        try (Connection conn = DatabaseUtil.getConnection()) {
            String sqlClientes = "SELECT COUNT(*) AS total FROM clientes WHERE activo = 1 " +
                    "AND (tipoMembresia IS NULL OR LOWER(tipoMembresia) <> 'diario')";
            try (PreparedStatement ps = conn.prepareStatement(sqlClientes);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ctrlClientes.setValor(rs.getString("total"));
                }
            }

            String sqlVencimientos = "SELECT COUNT(*) AS total FROM clientes " +
                    "WHERE activo = 1 " +
                    "AND LOWER(tipoMembresia) <> 'diario' " +
                    "AND date(fecha_vencimiento) BETWEEN date('now') AND date('now', '+7 days')";
            try (PreparedStatement ps = conn.prepareStatement(sqlVencimientos);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ctrlVencimientos.setValor(rs.getString("total"));
                }
            }

            double totalPagos = DatabaseUtil.obtenerTotalPagosDelMesActual();
            double totalVentas = DatabaseUtil.obtenerTotalVentasDelMes();
            double totalEgresos = DatabaseUtil.obtenerTotalEgresosDelMes();

            double balance = (totalPagos + totalVentas) - totalEgresos;

            ctrlPagos.setValor(String.format("$ %.2f", balance));

            String tooltipText = String.format(
                    "Membresías: $%.2f\nVentas: $%.2f\nEgresos: $%.2f",
                    totalPagos, totalVentas, totalEgresos
            );

            Tooltip tooltip = new Tooltip(tooltipText);
            tooltip.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: #2D2D2D; "
                    + "-fx-text-fill: #FFFFFF; -fx-border-width: 1px; -fx-border-color: #555555; "
                    + "-fx-border-radius: 4px; -fx-background-radius: 4px;");

            Tooltip.install(cardPagos, tooltip);

        } catch (SQLException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al cargar datos métricos.");
        }
    }

    private void abrirRenovacionConCliente(Cliente cliente) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/renovacion.fxml"));
            Parent root = loader.load();

            RenovacionController controller = loader.getController();
            controller.precargarCliente(cliente);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Renovar Membresía");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al abrir renovación.");
        }
    }

    private void cargarClientesProximosAVencer() {
        ObservableList<Cliente> clientes = FXCollections.observableArrayList();
        String sql = "SELECT nombres, apellidos, telefono, telefono_visible, tipoMembresia, fecha_vencimiento " +
                "FROM clientes " +
                "WHERE activo = 1 " +
                "AND LOWER(tipoMembresia) <> 'diario' " +
                "AND date(fecha_vencimiento) BETWEEN date('now') AND date('now', '+7 days') " +
                "ORDER BY fecha_vencimiento";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            boolean hayClientes = false;
            while (rs.next()) {
                hayClientes = true;
                Cliente cliente = new Cliente(
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("telefono"),
                        rs.getString("telefono_visible"),
                        rs.getString("tipoMembresia"),
                        LocalDate.parse(rs.getString("fecha_vencimiento"))
                );

                cliente.setDiasRestantes();

                clientes.add(cliente);
            }

            tablaClientesProximosAVencer.setItems(clientes);
            ajustarAlturaTabla();
            lblMensaje.setText(hayClientes ? "" : "No hay clientes próximos a vencer en los próximos 7 días.");

        } catch (SQLException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al cargar clientes próximos a vencer.");
        }
    }

    private void ajustarAlturaTabla() {
        double alturaTotal = calcularAlturaTabla(tablaClientesProximosAVencer.getItems().size());
        tablaClientesProximosAVencer.setPrefHeight(alturaTotal);
        tablaClientesProximosAVencer.setMinHeight(alturaTotal);
        tablaClientesProximosAVencer.setMaxHeight(alturaTotal);

        Platform.runLater(() -> tablaClientesProximosAVencer.requestLayout());
    }

    private double calcularAlturaTabla(int cantidadFilas) {
        double alturaPorFila = tablaClientesProximosAVencer.getFixedCellSize();
        if (alturaPorFila <= 0) {
            alturaPorFila = 44;
        }

        int filasVisibles = 4;
        double alturaCabecera = 48;

        return (filasVisibles * alturaPorFila) + alturaCabecera;
    }

    private void abrirPagos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/pagos.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Pagos activos");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al abrir pagos activos");
        }
    }

    public void handleExportarPDF() {
        ReporteUtil.generarReporteFinanciero(8, 2025);
    }

    @FXML
    private void handleRegistroCliente(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/registro_cliente.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Registro de Cliente");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            lblMensaje.setText("No se pudo abrir el formulario de registro.");
        }
    }

    @FXML
    private void abrirCoaches(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/lista_coaches.fxml"));
            Parent root = loader.load();
            ListaCoachesController controller = loader.getController();
            controller.setModoRecepcionista(true);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Coaches");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al abrir coaches");
        }
    }

    @FXML
    private void handleVerTodos(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/renovacion.fxml"));
            Parent root = loader.load();

            RenovacionController controller = loader.getController();
            controller.setModoTodosClientes(true);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Todos los Clientes Activos - Renovación");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al abrir todos los clientes");
        }
    }

    @FXML
    private void abrirInventario() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/inventario_ventas.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Gestión de Inventario y Ventas");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.paint.Color;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import models.Cliente;
import util.DatabaseUtil;
import util.EventBus;
import util.ReporteUtil;
import util.SessionManager;
import util.AuditoriaUtil;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class DashboardController implements Initializable {

    @FXML private AnchorPane cardClientes;
    @FXML private AnchorPane cardPagos;
    @FXML private AnchorPane cardVencimientos;
    @FXML private TableView<Cliente> tablaClientesProximosAVencer;
    @FXML private Label lblMensaje;
    @FXML private Label lblUsuarioRol;
    @FXML private Button btnEgresos;
    @FXML private Button btnAuditoria;
    @FXML private Button btnUsuarios;
    @FXML private Button btnLogout;
//  @FXML private Button btnVerTodos;

    @FXML private TableColumn<Cliente, String> colCliente;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colVencimiento;
    @FXML private TableColumn<Cliente, Integer> colDiasRestantes;
    @FXML private TableColumn<Cliente, Void> colAlerta;
    @FXML private TableColumn<Cliente, Void> colAccion;

    private MetricCardController ctrlClientes;
    private MetricCardController ctrlPagos;
    private MetricCardController ctrlVencimientos;

    private Consumer<EventBus.EventType> dashboardListener;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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
                    AuditoriaUtil.registrar(SessionManager.getUsuarioActual().getNombre(), "LOGOUT", "USUARIO", SessionManager.getUsuarioActual().getId(), "Cierre de sesión");
                    EventBus.unregisterListener(EventBus.EventType.EGRESO_REGISTRADO, dashboardListener);
                    EventBus.unregisterListener(EventBus.EventType.DATOS_ACTUALIZADOS, dashboardListener);
                    EventBus.unregisterListener(EventBus.EventType.VENTA_REALIZADA, dashboardListener);
                });
            });

            configurarTablaSinScroll();

            colCliente.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
            colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
            colVencimiento.setCellValueFactory(new PropertyValueFactory<>("fecha_vencimiento"));
            colDiasRestantes.setCellValueFactory(new PropertyValueFactory<>("diasRestantes"));

            colAlerta.setCellFactory(column -> new TableCell<Cliente, Void>() {
                private final Label warningIcon = new Label("⚠");

                {
                    warningIcon.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        Cliente cliente = getTableView().getItems().get(getIndex());
                        setGraphic(cliente.getDiasRestantes() <= 3 ? warningIcon : null);
                    }
                }
            });

            colAccion.setCellFactory(column -> new TableCell<Cliente, Void>() {
                private final Button btnReactivar = new Button();

                {
                    FontIcon iconoReactivar = new FontIcon(FontAwesomeSolid.REDO);
                    iconoReactivar.setIconColor(Color.web("#28a745"));
                    iconoReactivar.setIconSize(18);
                    btnReactivar.setGraphic(iconoReactivar);
                    btnReactivar.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                    btnReactivar.setTooltip(new Tooltip("Reactivar cliente"));

                    btnReactivar.setOnAction(event -> {
                        Cliente cliente = getTableView().getItems().get(getIndex());
                        abrirRenovacionConCliente(cliente);
                    });
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

            centrarContenidoColumnas();
            inicializarTarjetasMetricas();
            cargarDatosTarjetas();
            cargarClientesProximosAVencer();
            lblUsuarioRol.setText(SessionManager.getUsuarioActual().getNombre() + " (" + SessionManager.getUsuarioActual().getRol() + ")");
            if (!SessionManager.isAdmin()) {
                btnEgresos.setVisible(false);
                btnAuditoria.setVisible(false);
                btnUsuarios.setVisible(false);
            }

            tablaClientesProximosAVencer.setRowFactory(tv -> {
                TableRow<Cliente> row = new TableRow<Cliente>() {
                    @Override
                    protected void updateItem(Cliente cliente, boolean empty) {
                        super.updateItem(cliente, empty);
                        if (cliente == null || empty) {
                            setStyle("");
                        } else {
                            int dias = cliente.getDiasRestantes();
                            String baseStyle = "";

                            if (dias >= 5 && dias <= 7) {
                                baseStyle = "-fx-background-color: #e8f5e9;";
                            } else if (dias >= 3 && dias <= 4) {
                                baseStyle = "-fx-background-color: #fff3e0;";
                            } else if (dias >= 0 && dias <= 3) {
                                baseStyle = "-fx-background-color: #ffebee;";
                            } else if (dias < 0) {
                                baseStyle = "-fx-background-color: #ffcdd2;";
                            }

                            setStyle(baseStyle + (isSelected() ?
                                    " -fx-font-weight: bold; -fx-text-fill: black;" :
                                    " -fx-text-fill: black;"));
                        }
                    }
                };

                row.setOnMouseEntered(event -> {
                    if (!row.isEmpty()) {
                        Tooltip tooltip = new Tooltip(row.getItem().getTooltipText());
                        tooltip.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                        Tooltip.install(row, tooltip);
                    }
                });
                row.setOnMouseExited(event -> {
                    if (!row.isEmpty()) {
                        Tooltip.uninstall(row, null);
                    }
                });
                return row;
            });

        } catch (Exception e) {
            e.printStackTrace();
            lblMensaje.setText("Error al inicializar el panel.");
        }

        EventBus.registerListener(this::cargarDatosTarjetas);
    }

    private void configurarTablaSinScroll() {
        tablaClientesProximosAVencer.setFixedCellSize(30);
        tablaClientesProximosAVencer.setStyle(
                "-fx-scroll-bar-policy-vertical: never;" +
                        "-fx-scroll-bar-policy-horizontal: never;" +
                        "-fx-padding: 0;"
        );

        tablaClientesProximosAVencer.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                ScrollBar vbar = (ScrollBar) tablaClientesProximosAVencer.lookup(".scroll-bar:vertical");
                ScrollBar hbar = (ScrollBar) tablaClientesProximosAVencer.lookup(".scroll-bar:horizontal");
                if (vbar != null) vbar.setVisible(false);
                if (hbar != null) hbar.setVisible(false);
            }
        });

        tablaClientesProximosAVencer.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    @FXML
    private void abrirRegistroEgreso(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/registro_egreso.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Registrar Egreso");
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
        paneClientes.prefWidthProperty().bind(cardClientes.widthProperty());
        paneClientes.prefHeightProperty().bind(cardClientes.heightProperty());
        cardClientes.getChildren().add(paneClientes);
        paneClientes.setOnMouseClicked(event -> abrirListaClientes());

        FXMLLoader loaderPagos = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane panePagos = loaderPagos.load();
        ctrlPagos = loaderPagos.getController();
        ctrlPagos.setTitulo("Pagos Recibidos");
        panePagos.prefWidthProperty().bind(cardPagos.widthProperty());
        panePagos.prefHeightProperty().bind(cardPagos.heightProperty());
        cardPagos.getChildren().add(panePagos);
        panePagos.setOnMouseClicked(e -> handleVerIngresosMensuales(null));

        FXMLLoader loaderVencimientos = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneVencimientos = loaderVencimientos.load();
        ctrlVencimientos = loaderVencimientos.getController();
        ctrlVencimientos.setTitulo("Próximos a Vencer");
        paneVencimientos.prefWidthProperty().bind(cardVencimientos.widthProperty());
        paneVencimientos.prefHeightProperty().bind(cardVencimientos.heightProperty());
        cardVencimientos.getChildren().add(paneVencimientos);
        paneVencimientos.setOnMouseClicked(e -> handleVerTodos(null));
    }

    private void centrarContenidoColumnas() {
        centrarColumna(colCliente);
        centrarColumna(colTelefono);
        centrarColumna(colVencimiento);
        centrarColumna(colDiasRestantes);
        colAlerta.setStyle("-fx-alignment: CENTER;");
        colAccion.setStyle("-fx-alignment: CENTER;");
    }

    private <T> void centrarColumna(TableColumn<Cliente, T> columna) {
        columna.setStyle("-fx-alignment: CENTER;");
        columna.setCellFactory(column -> new TableCell<Cliente, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    setAlignment(Pos.CENTER);
                    setStyle("-fx-text-fill: black;");
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
            String sqlClientes = "SELECT COUNT(*) AS total FROM clientes WHERE activo = 1";
            try (PreparedStatement ps = conn.prepareStatement(sqlClientes);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ctrlClientes.setValor(rs.getString("total"));
                }
            }

            String sqlVencimientos = "SELECT COUNT(*) AS total FROM clientes " +
                    "WHERE activo = 1 " +
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
        String sql = "SELECT nombres, apellidos, telefono, tipoMembresia, fecha_vencimiento " +
                "FROM clientes " +
                "WHERE activo = 1 " +
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
        int filas = tablaClientesProximosAVencer.getItems().size();
        double alturaPorFila = 30;
        double alturaCabecera = 35;

        double alturaTotal = Math.max(150, (filas * alturaPorFila) + alturaCabecera);
        tablaClientesProximosAVencer.setPrefHeight(alturaTotal);

        Platform.runLater(() -> tablaClientesProximosAVencer.requestLayout());
    }

    @FXML
    private void handleVerIngresosMensuales(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ingresos_mensuales.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Ingresos Mensuales Detallados");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al abrir ingresos mensuales");
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
    private void handleRegistroCoach(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/registro_coach.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Registro de Coach");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            lblMensaje.setText("No se pudo abrir el formulario de coaches.");
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

    @FXML
    private void abrirAuditoria(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/auditoria.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void abrirUsuarios(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/usuarios.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Gestión de Usuarios");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        AuditoriaUtil.registrar(SessionManager.getUsuarioActual().getNombre(), "LOGOUT", "USUARIO", SessionManager.getUsuarioActual().getId(), "Cierre de sesión");
        SessionManager.clear();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
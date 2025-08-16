package controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import util.AuditoriaUtil;
import util.DatabaseUtil;
import util.EventBus;
import util.ReporteUtil;
import util.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class AdminDashboardController implements Initializable {

    @FXML private FlowPane metricsContainer;
    @FXML private Label lblMensaje;
    @FXML private Label lblUsuarioRol;

    private MetricCardController ctrlClientes;
    private MetricCardController ctrlPagos;
    private MetricCardController ctrlVencimientos;

    private Node cardPagosNode;

    private Consumer<EventBus.EventType> dashboardListener;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            verificarCierreMensual();

            Platform.runLater(() -> {
                Stage stage = (Stage) metricsContainer.getScene().getWindow();

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
                    AuditoriaUtil.registrar(
                            SessionManager.getUsuarioActual().getNombre(),
                            "LOGOUT",
                            "USUARIO",
                            SessionManager.getUsuarioActual().getId(),
                            "Cierre de sesión"
                    );
                    EventBus.unregisterListener(EventBus.EventType.EGRESO_REGISTRADO, dashboardListener);
                    EventBus.unregisterListener(EventBus.EventType.DATOS_ACTUALIZADOS, dashboardListener);
                    EventBus.unregisterListener(EventBus.EventType.VENTA_REALIZADA, dashboardListener);
                });
            });

            inicializarTarjetasMetricas();
            cargarDatosTarjetas();
            lblUsuarioRol.setText(SessionManager.getUsuarioActual().getNombre() + " (" +
                    SessionManager.getUsuarioActual().getRol() + ")");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void inicializarTarjetasMetricas() throws IOException {
        metricsContainer.getChildren().clear();

        FXMLLoader loaderClientes = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneClientes = loaderClientes.load();
        ctrlClientes = loaderClientes.getController();
        ctrlClientes.setTitulo("Clientes Activos");
        paneClientes.setOnMouseClicked(e -> abrirListaClientes());
        metricsContainer.getChildren().add(paneClientes);

        FXMLLoader loaderPagos = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane panePagos = loaderPagos.load();
        ctrlPagos = loaderPagos.getController();
        ctrlPagos.setTitulo("Pagos Recibidos");
        panePagos.setOnMouseClicked(e -> handleVerIngresosMensuales(null));
        metricsContainer.getChildren().add(panePagos);
        cardPagosNode = panePagos;

        FXMLLoader loaderVencimientos = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneVencimientos = loaderVencimientos.load();
        ctrlVencimientos = loaderVencimientos.getController();
        ctrlVencimientos.setTitulo("Próximos a Vencer");
        paneVencimientos.setOnMouseClicked(e -> handleVerTodos(null));
        metricsContainer.getChildren().add(paneVencimientos);
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
            tooltip.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: #2D2D2D; " +
                    "-fx-text-fill: #FFFFFF; -fx-border-width: 1px; -fx-border-color: #555555; " +
                    "-fx-border-radius: 4px; -fx-background-radius: 4px;");

            Tooltip.install(cardPagosNode, tooltip);

        } catch (SQLException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al cargar datos métricos.");
        }
    }

    private void verificarCierreMensual() {

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT MAX(fecha) AS fecha FROM movimientos_inventario WHERE tipo = 'SALDO_INICIAL'");
             ResultSet rs = stmt.executeQuery()) {
            LocalDateTime ultima = null;
            if (rs.next()) {
                String fechaStr = rs.getString("fecha");
                if (fechaStr != null) {
                    ultima = LocalDateTime.parse(fechaStr);
                }
            }

            LocalDate proximoMes = LocalDate.now().plusMonths(1).withDayOfMonth(1);
            if (ultima == null || ultima.toLocalDate().isBefore(proximoMes)) {
                ReporteUtil.cierreMensual();
                AuditoriaUtil.registrar(
                        SessionManager.getUsuarioActual().getNombre(),
                        "CIERRE_MENSUAL",
                        "INVENTARIO",
                        null,
                        "Saldo inicial generado"
                );
            }
        } catch (SQLException e) {
            System.err.println("Error verificando cierre mensual: " + e.getMessage());
        }
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
    private void abrirProveedores(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/proveedores.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Gestión de Proveedores");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al abrir proveedores");
        }
    }

    @FXML
    private void abrirReportes(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/reportes.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Reportes");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al abrir reportes");
        }
    }
}
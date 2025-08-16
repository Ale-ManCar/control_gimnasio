package controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
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

    @FXML private GridPane metricsContainer;
    @FXML private ComboBox<String> cmbPeriodo;
    @FXML private ComboBox<String> cmbArea;
    @FXML private ListView<String> lstReportes;
    @FXML private ListView<String> lstVencimientos;
    @FXML private Label lblUsuarioRol;
    @FXML private Button btnUsuarios;
    @FXML private Button btnAuditoria;
    @FXML private Button btnProveedores;
    @FXML private Button btnComparador;
    @FXML private Button btnRespaldos;
    @FXML private Button btnConfiguracion;

    private MetricCardController ctrlClientesActivos;
    private MetricCardController ctrlClientesInactivos;
    private MetricCardController ctrlMembresias;
    private MetricCardController ctrlStockCritico;
    private MetricCardController ctrlTopVentas;

    private Consumer<EventBus.EventType> dashboardListener;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            verificarCierreMensual();

            cmbPeriodo.getItems().addAll("Hoy", "Esta semana", "Este mes");
            cmbPeriodo.setValue("Hoy");
            cmbArea.getItems().addAll("Todas las áreas", "Ventas", "Membresías");
            cmbArea.setValue("Todas las áreas");

            cmbPeriodo.valueProperty().addListener((obs, o, n) -> cargarDatosTarjetas());
            cmbArea.valueProperty().addListener((obs, o, n) -> cargarDatosTarjetas());

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

        FXMLLoader loaderActivos = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneActivos = loaderActivos.load();
        ctrlClientesActivos = loaderActivos.getController();
        ctrlClientesActivos.setTitulo("Clientes Activos");
        ctrlClientesActivos.setIcon("fas-user-check", "#2e7d32");
        metricsContainer.add(paneActivos, 0, 0);
        paneActivos.setOnMouseClicked(e -> abrirListaClientes());

        FXMLLoader loaderInactivos = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneInactivos = loaderInactivos.load();
        ctrlClientesInactivos = loaderInactivos.getController();
        ctrlClientesInactivos.setTitulo("Clientes Inactivos");
        ctrlClientesInactivos.setIcon("fas-user-slash", "#c62828");
        metricsContainer.add(paneInactivos, 1, 0);

        FXMLLoader loaderMembresias = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneMembresias = loaderMembresias.load();
        ctrlMembresias = loaderMembresias.getController();
        ctrlMembresias.setTitulo("Membresías Nuevas");
        ctrlMembresias.setIcon("fas-id-card", "#1565c0");
        metricsContainer.add(paneMembresias, 0, 1);

        FXMLLoader loaderStock = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneStock = loaderStock.load();
        ctrlStockCritico = loaderStock.getController();
        ctrlStockCritico.setTitulo("Stock Crítico");
        ctrlStockCritico.setIcon("fas-exclamation-triangle", "#f57c00");
        metricsContainer.add(paneStock, 1, 1);

        FXMLLoader loaderTop = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneTop = loaderTop.load();
        ctrlTopVentas = loaderTop.getController();
        ctrlTopVentas.setTitulo("Top de Ventas");
        ctrlTopVentas.setIcon("fas-chart-line", "#6a1b9a");
        metricsContainer.add(paneTop, 0, 2);
    }

    private void cargarDatosTarjetas() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy;
        LocalDate fin = hoy;

        String periodo = cmbPeriodo.getValue();
        if ("Esta semana".equals(periodo)) {
            inicio = hoy.minusDays(hoy.getDayOfWeek().getValue() - 1);
            fin = inicio.plusDays(6);
        } else if ("Este mes".equals(periodo)) {
            inicio = hoy.withDayOfMonth(1);
            fin = hoy.withDayOfMonth(hoy.lengthOfMonth());
        }

        String area = cmbArea.getValue();

        try {
            if (!"Ventas".equals(area)) {
                ctrlClientesActivos.setValor(String.valueOf(DatabaseUtil.contarClientesActivos(inicio, fin)));
                ctrlClientesInactivos.setValor(String.valueOf(DatabaseUtil.contarClientesInactivos(inicio, fin)));
                ctrlMembresias.setValor(String.valueOf(DatabaseUtil.contarMembresiasNuevas(inicio, fin)));
            } else {
                ctrlClientesActivos.setValor("-");
                ctrlClientesInactivos.setValor("-");
                ctrlMembresias.setValor("-");
            }

            if (!"Membresías".equals(area)) {
                int criticos = DatabaseUtil.contarProductosStockCritico();
                ctrlStockCritico.setValor(String.valueOf(criticos));
                String top = DatabaseUtil.obtenerTopProductoVendido(inicio, fin);
                ctrlTopVentas.setValor(top != null ? top : "N/A");
            } else {
                ctrlStockCritico.setValor("-");
                ctrlTopVentas.setValor("-");
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
        }
    }

    @FXML
    private void abrirAuditoria(ActionEvent event) {
        abrirVentana("/fxml/auditoria.fxml", "Auditoría");
    }

    @FXML
    private void abrirUsuarios(ActionEvent event) {
        abrirVentana("/fxml/usuarios.fxml", "Gestión de Usuarios");
    }

    @FXML
    private void abrirProveedores(ActionEvent event) {
        abrirVentana("/fxml/proveedores.fxml", "Gestión de Proveedores");
    }

    @FXML
    private void abrirReportes(ActionEvent event) {
        abrirVentana("/fxml/reportes.fxml", "Reportes");
    }

    private void abrirVentana(String recurso, String titulo) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(recurso));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle(titulo);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void abrirComparadorPrecios(ActionEvent event) {
        abrirVentana("/fxml/comparador_precios.fxml", "Comparador de Precios");
    }

    @FXML
    private void abrirRespaldos(ActionEvent event) {
        abrirVentana("/fxml/respaldos.fxml", "Respaldos");
    }

    @FXML
    private void abrirConfiguracion(ActionEvent event) {
        abrirVentana("/fxml/configuracion.fxml", "Configuración");
    }
}
package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
    @FXML private Button btnCerrarDia;

    private MetricCardController ctrlClientesActivos;
    private MetricCardController ctrlMembresias;
    private MetricCardController ctrlVencimientos;
    private MetricCardController ctrlIngresos;
    private MetricCardController ctrlEgresos;
    private MetricCardController ctrlBalance;
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

        FXMLLoader loaderMembresias = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneMembresias = loaderMembresias.load();
        ctrlMembresias = loaderMembresias.getController();
        ctrlMembresias.setTitulo("Membresías Nuevas");
        ctrlMembresias.setIcon("fas-id-card", "#1565c0");
        metricsContainer.add(paneMembresias, 1, 0);

        FXMLLoader loaderVencimientos = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneVencimientos = loaderVencimientos.load();
        ctrlVencimientos = loaderVencimientos.getController();
        ctrlVencimientos.setTitulo("Vencimientos");
        ctrlVencimientos.setIcon("fas-hourglass-half", "#fbc02d");
        metricsContainer.add(paneVencimientos, 2, 0);

        FXMLLoader loaderIngresos = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneIngresos = loaderIngresos.load();
        ctrlIngresos = loaderIngresos.getController();
        ctrlIngresos.setTitulo("Ingresos");
        ctrlIngresos.setIcon("fas-dollar-sign", "#2e7d32");
        metricsContainer.add(paneIngresos, 3, 0);

        FXMLLoader loaderEgresos = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneEgresos = loaderEgresos.load();
        ctrlEgresos = loaderEgresos.getController();
        ctrlEgresos.setTitulo("Egresos");
        ctrlEgresos.setIcon("fas-arrow-circle-down", "#c62828");
        metricsContainer.add(paneEgresos, 0, 1);

        FXMLLoader loaderBalance = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneBalance = loaderBalance.load();
        ctrlBalance = loaderBalance.getController();
        ctrlBalance.setTitulo("Balance");
        ctrlBalance.setIcon("fas-balance-scale", "#6a1b9a");
        metricsContainer.add(paneBalance, 1, 1);

        FXMLLoader loaderStock = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneStock = loaderStock.load();
        ctrlStockCritico = loaderStock.getController();
        ctrlStockCritico.setTitulo("Stock Crítico");
        ctrlStockCritico.setIcon("fas-exclamation-triangle", "#f57c00");
        metricsContainer.add(paneStock, 2, 1);

        FXMLLoader loaderTop = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneTop = loaderTop.load();
        ctrlTopVentas = loaderTop.getController();
        ctrlTopVentas.setTitulo("Top de Ventas");
        ctrlTopVentas.setIcon("fas-chart-line", "#1565c0");
        metricsContainer.add(paneTop, 3, 1);
    }

    private void cargarDatosTarjetas() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio;
        LocalDate fin;

        String periodo = cmbPeriodo.getValue();
        if ("Esta semana".equals(periodo)) {
            inicio = hoy.minusDays(hoy.getDayOfWeek().getValue() - 1);
            fin = inicio.plusDays(6);
        } else if ("Este mes".equals(periodo)) {
            inicio = hoy.withDayOfMonth(1);
            fin = hoy.withDayOfMonth(hoy.lengthOfMonth());
        } else {
            fin = hoy;
            inicio = hoy;
        }

        String area = cmbArea.getValue();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    int clientes = DatabaseUtil.contarClientesActivos(inicio, fin);
                    int membresias = DatabaseUtil.contarMembresiasNuevas(inicio, fin);
                    java.util.List<String> vencimientos = DatabaseUtil.listarVencimientosProximos();
                    double ingresos = DatabaseUtil.sumarIngresos(inicio, fin);
                    double egresos = DatabaseUtil.sumarEgresos(inicio, fin);
                    double balance = ingresos - egresos;
                    int criticos = DatabaseUtil.contarProductosStockCritico();
                    String top = DatabaseUtil.obtenerTopProductoVendido(inicio, fin);
                    java.util.List<String> reportes = DatabaseUtil.listarReportesUltimaSemana();

                    Platform.runLater(() -> {
                        if (!"Ventas".equals(area)) {
                            ctrlClientesActivos.setValor(String.valueOf(clientes));
                            ctrlMembresias.setValor(String.valueOf(membresias));
                            ctrlVencimientos.setValor(String.valueOf(vencimientos.size()));
                            lstVencimientos.setItems(FXCollections.observableArrayList(vencimientos));
                        } else {
                            ctrlClientesActivos.setValor("-");
                            ctrlMembresias.setValor("-");
                            ctrlVencimientos.setValor("-");
                            lstVencimientos.getItems().clear();
                        }

                        if (!"Membresías".equals(area)) {
                            ctrlIngresos.setValor(String.format("%.2f", ingresos));
                            ctrlEgresos.setValor(String.format("%.2f", egresos));
                            ctrlBalance.setValor(String.format("%.2f", balance));
                            ctrlStockCritico.setValor(String.valueOf(criticos));
                            ctrlTopVentas.setValor(top != null ? top : "N/A");
                            lstReportes.setItems(FXCollections.observableArrayList(reportes));
                        } else {
                            ctrlIngresos.setValor("-");
                            ctrlEgresos.setValor("-");
                            ctrlBalance.setValor("-");
                            ctrlStockCritico.setValor("-");
                            ctrlTopVentas.setValor("-");
                            lstReportes.getItems().clear();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
            new Thread(task).start();
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
        if (SessionManager.isAdmin()) {
            abrirVentana("/fxml/auditoria.fxml", "Auditoría");
        }
    }

    @FXML
    private void abrirUsuarios(ActionEvent event) {
        if (SessionManager.isAdmin()) {
            abrirVentana("/fxml/usuarios.fxml", "Gestión de Usuarios");
        }
    }

    @FXML
    private void abrirProveedores(ActionEvent event) {
        if (SessionManager.isAdmin()) {
            abrirVentana("/fxml/proveedores.fxml", "Gestión de Proveedores");
        }
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
        if (SessionManager.isAdmin()) {
            abrirVentana("/fxml/comparador.fxml", "Comparador de Precios");
        }
    }

    @FXML
    private void abrirRespaldos(ActionEvent event) {
        if (SessionManager.isAdmin()) {
            abrirVentana("/fxml/respaldos.fxml", "Respaldos");
        }
    }

    @FXML
    private void abrirConfiguracion(ActionEvent event) {
        if (SessionManager.isAdmin()) {
            abrirVentana("/fxml/configuracion.fxml", "Configuración");
        }
    }

    @FXML
    private void handleCerrarDia(ActionEvent event) {
        if (!SessionManager.isAdmin()) {
            System.out.println("Acceso denegado: solo un administrador puede cerrar el día");
            return;
        }

        LocalDate hoy = LocalDate.now();
        String usuario = SessionManager.getUsuarioActual().getNombre();

        try (Connection conn = DatabaseUtil.getConnection()) {
            String sqlPagos = "SELECT IFNULL(SUM(monto),0) FROM pagos WHERE date(fecha_pago)=?";
            String sqlVentas = "SELECT IFNULL(SUM(total),0) FROM ventas WHERE date(fecha)=?";
            String sqlEgresos = "SELECT IFNULL(SUM(monto),0) FROM egresos WHERE date(fecha)=?";

            double pagos;
            double ventas;
            double egresos;

            try (PreparedStatement ps = conn.prepareStatement(sqlPagos)) {
                ps.setString(1, hoy.toString());
                ResultSet rs = ps.executeQuery();
                pagos = rs.next() ? rs.getDouble(1) : 0.0;
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlVentas)) {
                ps.setString(1, hoy.toString());
                ResultSet rs = ps.executeQuery();
                ventas = rs.next() ? rs.getDouble(1) : 0.0;
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlEgresos)) {
                ps.setString(1, hoy.toString());
                ResultSet rs = ps.executeQuery();
                egresos = rs.next() ? rs.getDouble(1) : 0.0;
            }

            double ingresos = pagos + ventas;
            double balance = ingresos - egresos;

            String insertSql = "INSERT INTO cierres_diarios (fecha, ingresos, egresos, balance, usuario) VALUES (?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, hoy.toString());
                ps.setDouble(2, ingresos);
                ps.setDouble(3, egresos);
                ps.setDouble(4, balance);
                ps.setString(5, usuario);
                ps.executeUpdate();
            }

            AuditoriaUtil.registrar(usuario, "CIERRE_DIARIO", "CIERRE", null, hoy.toString());

            ReporteUtil.generarReporteDiario(hoy);
            if (hoy.getDayOfMonth() == hoy.lengthOfMonth()) {
                ReporteUtil.generarReporteMensual(hoy.getYear(), hoy.getMonthValue());
            }
            if (hoy.getDayOfYear() == hoy.lengthOfYear()) {
                ReporteUtil.generarReporteAnual(hoy.getYear());
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
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
        metricsContainer.add(paneActivos, 0, 0);
        paneActivos.setOnMouseClicked(e -> abrirListaClientes());

        FXMLLoader loaderInactivos = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneInactivos = loaderInactivos.load();
        ctrlClientesInactivos = loaderInactivos.getController();
        ctrlClientesInactivos.setTitulo("Clientes Inactivos");
        metricsContainer.add(paneInactivos, 1, 0);

        FXMLLoader loaderMembresias = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneMembresias = loaderMembresias.load();
        ctrlMembresias = loaderMembresias.getController();
        ctrlMembresias.setTitulo("Membresías Nuevas");
        metricsContainer.add(paneMembresias, 0, 1);

        FXMLLoader loaderStock = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneStock = loaderStock.load();
        ctrlStockCritico = loaderStock.getController();
        ctrlStockCritico.setTitulo("Stock Crítico");
        metricsContainer.add(paneStock, 1, 1);

        FXMLLoader loaderTop = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneTop = loaderTop.load();
        ctrlTopVentas = loaderTop.getController();
        ctrlTopVentas.setTitulo("Top de Ventas");
        metricsContainer.add(paneTop, 0, 2);
    }

    private void cargarDatosTarjetas() {
        try (Connection conn = DatabaseUtil.getConnection()) {
            String sqlActivos = "SELECT COUNT(*) AS total FROM clientes WHERE activo = 1";
            try (PreparedStatement ps = conn.prepareStatement(sqlActivos);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ctrlClientesActivos.setValor(rs.getString("total"));
                }
            }

            String sqlInactivos = "SELECT COUNT(*) AS total FROM clientes WHERE activo = 0";
            try (PreparedStatement ps = conn.prepareStatement(sqlInactivos);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ctrlClientesInactivos.setValor(rs.getString("total"));
                }
            }

            ctrlMembresias.setValor("0");
            ctrlStockCritico.setValor("0");
            ctrlTopVentas.setValor("0");

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
        }
    }

    @FXML
    private void abrirComparador(ActionEvent event) {
        // TODO implementar comparador de precios
    }

    @FXML
    private void abrirRespaldos(ActionEvent event) {
        // TODO implementar respaldos
    }

    @FXML
    private void abrirConfiguracion(ActionEvent event) {
        // TODO implementar configuracion
    }
}
package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import util.AlertScheduler;
import util.DatabaseUtil;
import util.ReporteUtil;
import util.SessionManager;
import models.CoachClientes;
import models.MetricItem;
import models.Role;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private AnchorPane cardActivos;
    @FXML private AnchorPane cardInactivos;
    @FXML private AnchorPane cardMembresias;
    @FXML private AnchorPane cardPorVencer;
    @FXML private AnchorPane cardActivosHoy;
    @FXML private Label lblMensaje;
    @FXML private ListView<String> lstAlertas;
    @FXML private TableView<MetricItem> tblMorosos;
    @FXML private TableColumn<MetricItem, String> colMorososDesc;
    @FXML private TableColumn<MetricItem, Integer> colMorososValor;
    @FXML private TableView<CoachClientes> tblCoaches;
    @FXML private TableColumn<CoachClientes, String> colCoach;
    @FXML private TableColumn<CoachClientes, Integer> colClientes;

    private MetricCardController ctrlActivos;
    private MetricCardController ctrlInactivos;
    private MetricCardController ctrlMembresias;
    private MetricCardController ctrlPorVencer;
    private MetricCardController ctrlActivosHoy;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (!SessionManager.tienePermiso(Role.ADMIN)) {
            lblMensaje.setText("Acceso denegado");
            return;
        }
        try {
            inicializarTarjetas();
            configurarTablas();
            cargarDatos();
            cargarAlertasPendientes();
            cargarMorosos();
            cargarCoaches();
        } catch (IOException e) {
            lblMensaje.setText("Error al cargar tarjetas");
        }
    }

    private void inicializarTarjetas() throws IOException {
        ctrlActivos = cargarTarjeta(cardActivos, "Clientes Activos");
        ctrlInactivos = cargarTarjeta(cardInactivos, "Clientes Inactivos");
        ctrlMembresias = cargarTarjeta(cardMembresias, "Membresías del Mes");
        ctrlPorVencer = cargarTarjeta(cardPorVencer, "Membresías a Vencer Semana");
        ctrlActivosHoy = cargarTarjeta(cardActivosHoy, "Clientes Activos Hoy");
    }

    private MetricCardController cargarTarjeta(AnchorPane contenedor, String titulo) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane pane = loader.load();
        MetricCardController controller = loader.getController();
        controller.setTitulo(titulo);
        pane.prefWidthProperty().bind(contenedor.widthProperty());
        pane.prefHeightProperty().bind(contenedor.heightProperty());
        contenedor.getChildren().add(pane);
        return controller;
    }

    private void cargarDatos() {
        try {
            Map<String, Integer> stats = DatabaseUtil.getAdminStats();
            ctrlActivos.setValor(String.valueOf(stats.getOrDefault("clientes_activos", 0)));
            ctrlInactivos.setValor(String.valueOf(stats.getOrDefault("clientes_inactivos", 0)));
            ctrlMembresias.setValor(String.valueOf(stats.getOrDefault("membresias_mes", 0)));
            ctrlPorVencer.setValor(String.valueOf(stats.getOrDefault("por_vencer", 0)));
            ctrlActivosHoy.setValor(String.valueOf(stats.getOrDefault("activos_hoy", 0)));
        } catch (SQLException e) {
            lblMensaje.setText("No se pudieron cargar las estadísticas");
        }
    }

    private void configurarTablas() {
        colMorososDesc.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colMorososValor.setCellValueFactory(new PropertyValueFactory<>("valor"));
        colCoach.setCellValueFactory(new PropertyValueFactory<>("coach"));
        colClientes.setCellValueFactory(new PropertyValueFactory<>("clientes"));
    }

    private void cargarMorosos() {
        try {
            int count = DatabaseUtil.contarClientesMorosos();
            ObservableList<MetricItem> data = FXCollections.observableArrayList(new MetricItem("Clientes Morosos", count));
            tblMorosos.setItems(data);
        } catch (SQLException e) {
            lblMensaje.setText("No se pudieron cargar clientes morosos");
        }
    }

    private void cargarCoaches() {
        try {
            ObservableList<CoachClientes> data = DatabaseUtil.listarCoachesConMasClientes();
            tblCoaches.setItems(data);
        } catch (SQLException e) {
            lblMensaje.setText("No se pudieron cargar los coaches");
        }
    }

    private void cargarAlertasPendientes() {
        lstAlertas.getItems().setAll(AlertScheduler.obtenerAlertasPendientes());
    }

    @FXML
    private void abrirUsuarios() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/lista_clientes.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Usuarios");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            lblMensaje.setText("Error al abrir usuarios");
        }
    }

    @FXML
    private void abrirInventario() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/inventario_equipos.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Inventario");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            lblMensaje.setText("Error al abrir inventario");
        }
    }

    @FXML
    private void abrirProveedores() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/comparador_precios.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Proveedores");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            lblMensaje.setText("Error al abrir proveedores");
        }
    }

    @FXML
    private void abrirAuditoria() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auditoria.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Auditoría");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            lblMensaje.setText("Error al abrir auditoría");
        }
    }

    @FXML
    private void abrirRespaldos() {
        lblMensaje.setText("Módulo de respaldos no disponible");
    }

    @FXML
    private void abrirConfiguracion() {
        lblMensaje.setText("Módulo de configuración no disponible");
    }

    @FXML
    private void generarActividadRecepcionista() {
        ReporteUtil.generarReporteActividadRecepcionista();
    }

    @FXML
    private void generarInventarioMensual() {
        LocalDate hoy = LocalDate.now();
        ReporteUtil.generarReporteInventario(hoy.getMonthValue(), hoy.getYear());
    }

    @FXML
    private void generarMembresiasPorVencer() {
        ReporteUtil.generarReporteMembresiasPorVencer();
    }

    @FXML
    private void generarMorososPDF() {
        ReporteUtil.generarReporteClientesMorososPDF();
    }

    @FXML
    private void generarMorososExcel() {
        ReporteUtil.generarReporteClientesMorososExcel();
    }

    @FXML
    private void generarCoachesTopPDF() {
        ReporteUtil.generarReporteCoachesConMasClientesPDF();
    }

    @FXML
    private void generarCoachesTopExcel() {
        ReporteUtil.generarReporteCoachesConMasClientesExcel();
    }

    @FXML
    private void generarReporteDashboardPDF() {
        ReporteUtil.generarReporteDashboardPDF();
    }

    @FXML
    private void generarReporteDashboardExcel() {
        ReporteUtil.generarReporteDashboardExcel();
    }
}
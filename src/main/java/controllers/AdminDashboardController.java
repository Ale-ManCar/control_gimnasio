package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Role;
import util.BackupUtil;
import util.DatabaseUtil;
import util.DashboardService;
import util.SessionManager;
import util.UserService;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private AnchorPane cardClientesActivos;
    @FXML private AnchorPane cardMorosos;
    @FXML private AnchorPane cardIngresos;
    @FXML private AnchorPane cardPorVencer;
    @FXML private AnchorPane cardCoaches;
    @FXML private Label lblMensaje;

    private MetricCardController ctrlClientesActivos;
    private MetricCardController ctrlMorosos;
    private MetricCardController ctrlIngresos;
    private MetricCardController ctrlPorVencer;
    private MetricCardController ctrlCoaches;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (!SessionManager.tienePermiso(Role.ADMIN)) {
            lblMensaje.setText("Acceso denegado");
            return;
        }
        try {
            inicializarTarjetas();
            cargarDatos();
        } catch (IOException e) {
            lblMensaje.setText("Error al cargar tarjetas");
        }
    }

    private void inicializarTarjetas() throws IOException {
        ctrlClientesActivos = DashboardService.cargarTarjeta(cardClientesActivos, "Clientes Activos");
        ctrlMorosos = DashboardService.cargarTarjeta(cardMorosos, "Morosos");
        ctrlIngresos = DashboardService.cargarTarjeta(cardIngresos, "Ingresos");
        ctrlPorVencer = DashboardService.cargarTarjeta(cardPorVencer, "Membresías por Vencer");
        ctrlCoaches = DashboardService.cargarTarjeta(cardCoaches, "Coaches");
        cardCoaches.setOnMouseClicked(e -> abrirCoaches());
    }

    private void cargarDatos() {
        try {
            DashboardService.AdminMetrics metrics = DashboardService.obtenerMetricasAdmin();
            ctrlClientesActivos.setValor(String.valueOf(metrics.getClientesActivos()));
            ctrlMorosos.setValor(String.valueOf(metrics.getMorosos()));
            ctrlIngresos.setValor(String.format("$ %.2f", metrics.getIngresos()));
            ctrlPorVencer.setValor(String.valueOf(metrics.getPorVencer()));
            ctrlCoaches.setValor(String.valueOf(DatabaseUtil.contarCoaches()));
        } catch (SQLException e) {
            lblMensaje.setText("No se pudieron cargar las estadísticas");
        }
    }

    @FXML
    private void abrirUsuarios() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/gestion_usuarios.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Gestión de usuarios");
            stage.setScene(new Scene(root));
            stage.show();
            UserService.registrarActividad(SessionManager.getCurrentUser(), "Abrir usuarios");
        } catch (IOException | SQLException e) {
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
            UserService.registrarActividad(SessionManager.getCurrentUser(), "Abrir inventario");
        } catch (IOException | SQLException e) {
            lblMensaje.setText("Error al abrir inventario");
        }
    }

    @FXML
    private void abrirProveedores() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/lista_proveedores.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Proveedores");
            stage.setScene(new Scene(root));
            stage.show();
            UserService.registrarActividad(SessionManager.getCurrentUser(), "Abrir proveedores");
        } catch (IOException | SQLException e) {
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
            UserService.registrarActividad(SessionManager.getCurrentUser(), "Abrir auditoría");
        } catch (IOException | SQLException e) {
            lblMensaje.setText("Error al abrir auditoría");
        }
    }

    @FXML
    private void abrirCoaches() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/lista_coaches.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Coaches");
            stage.setScene(new Scene(root));
            stage.show();
            UserService.registrarActividad(SessionManager.getCurrentUser(), "Abrir coaches");
        } catch (IOException | SQLException e) {
            lblMensaje.setText("Error al abrir coaches");
        }
    }

    @FXML
    private void abrirRespaldos() {
        try {
            Path dir = Path.of("backups");
            if (!Files.exists(dir)) {
                lblMensaje.setText("No hay respaldos disponibles");
                return;
            }
            List<Path> archivos = Files.walk(dir)
                    .filter(p -> p.getFileName().toString().endsWith(".db"))
                    .sorted()
                    .toList();
            if (archivos.isEmpty()) {
                lblMensaje.setText("No hay respaldos disponibles");
                return;
            }
            List<String> nombres = archivos.stream()
                    .map(p -> dir.relativize(p).toString())
                    .toList();
            ChoiceDialog<String> dialog = new ChoiceDialog<>(nombres.get(0), nombres);
            dialog.setTitle("Respaldos disponibles");
            dialog.setHeaderText("Seleccione un respaldo para restaurar");
            Optional<String> resultado = dialog.showAndWait();
            if (resultado.isPresent()) {
                Path seleccionado = dir.resolve(resultado.get());
                BackupUtil.restaurarBackup(seleccionado);
                lblMensaje.setText("Respaldo restaurado: " + resultado.get());
            }
        } catch (IOException e) {
            lblMensaje.setText("Error al listar respaldos");
        }
    }

    @FXML
    private void abrirConfiguracion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/config_scheduler.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Configuración de Tareas");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            lblMensaje.setText("No se pudo abrir configuración: " + e.getMessage());
        }
    }
}
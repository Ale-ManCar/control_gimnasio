package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import models.Role;
import util.BackupUtil;
import util.DashboardService;
import util.SessionManager;
import models.User;
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
    @FXML private AnchorPane cardIngresos;
    @FXML private AnchorPane cardCoaches;
    @FXML private AnchorPane cardCerrarSesion;
    @FXML private Label lblMensaje;
    @FXML private Label lblBienvenida;

    private MetricCardController ctrlClientesActivos;
    private MetricCardController ctrlIngresos;
    private MetricCardController ctrlCoaches;
    private MetricCardController ctrlCerrarSesion;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (!SessionManager.tienePermiso(Role.ADMIN)) {
            lblMensaje.setText("Acceso denegado");
            return;
        }

        actualizarMensajeBienvenida();
        try {
            inicializarTarjetas();
            cargarDatos();
        } catch (IOException e) {
            lblMensaje.setText("Error al cargar tarjetas");
        }
    }

    private void actualizarMensajeBienvenida() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && lblBienvenida != null) {
            lblBienvenida.setText("¡Bienvenido, " + currentUser.getUsername() + "!");
        }
    }

    private void inicializarTarjetas() throws IOException {
        ctrlClientesActivos = DashboardService.cargarTarjeta(cardClientesActivos, "Clientes Activos");
        ctrlClientesActivos.setIconLiteral("fas-users");
        ctrlClientesActivos.setAccent("metric-card--success");

        ctrlIngresos = DashboardService.cargarTarjeta(cardIngresos, "Ingresos");
        ctrlIngresos.setIconLiteral("fas-wallet");
        ctrlIngresos.setAccent("metric-card--info");
        ctrlIngresos.setOnClick(this::handleVerIngresosMensuales);

        ctrlCoaches = DashboardService.cargarTarjeta(cardCoaches, "Coaches");
        ctrlCoaches.setIconLiteral("fas-chalkboard-teacher");
        ctrlCoaches.setAccent("metric-card--purple");
        ctrlCoaches.setOnClick(e -> abrirCoaches());

        ctrlCerrarSesion = DashboardService.cargarTarjeta(cardCerrarSesion, "Cerrar sesión");
        ctrlCerrarSesion.setIconLiteral("fas-sign-out-alt");
        ctrlCerrarSesion.setAccent("metric-card--danger");
        ctrlCerrarSesion.setValor("Salir");
        ctrlCerrarSesion.setOnClick(this::handleCerrarSesion);
    }

    private void cargarDatos() {
        try {
            DashboardService.AdminMetrics metrics = DashboardService.obtenerMetricasAdmin();
            ctrlClientesActivos.setValor(String.valueOf(metrics.getClientesActivos()));
            ctrlIngresos.setValor(String.format("$ %.2f", metrics.getIngresos()));
            ctrlCoaches.setValor(String.valueOf(metrics.getCoaches()));
        } catch (SQLException e) {
            lblMensaje.setText("No se pudieron cargar las estadísticas");
        }
    }

    @FXML
    private void handleVerIngresosMensuales(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ingresos_mensuales.fxml"));
            Parent root = loader.load();
            IngresosMensualesController controller = loader.getController();
            if (controller == null) {
                lblMensaje.setText("No se pudo cargar ingresos mensuales");
                return;
            }
            Stage stage = new Stage();
            stage.setTitle("Ingresos Mensuales");
            stage.setScene(new Scene(root));
            stage.show();
            UserService.registrarActividad(SessionManager.getCurrentUser(), "Ver ingresos mensuales");
        } catch (IOException | SQLException e) {
            lblMensaje.setText("Error al abrir ingresos mensuales");
        }
    }

    @FXML
    private void handleCerrarSesion(MouseEvent event) {
        User usuarioActual = SessionManager.getCurrentUser();
        try {
            if (usuarioActual != null) {
                UserService.registrarActividad(usuarioActual, "Cerrar sesión");
            }
        } catch (SQLException e) {
            lblMensaje.setText("No se pudo registrar la actividad de cierre de sesión");
        }

        SessionManager.clear();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/selector_perfiles.fxml"));
            Parent root = loader.load();
            SelectorPerfilesController controller = loader.getController();
            Stage nuevaVentana = new Stage();
            nuevaVentana.setScene(new Scene(root, 700, 420));
            nuevaVentana.setTitle("Seleccionar perfil");
            nuevaVentana.setResizable(false);
            controller.setStage(nuevaVentana);
            nuevaVentana.show();

            Stage ventanaActual = (Stage) lblBienvenida.getScene().getWindow();
            ventanaActual.close();
        } catch (IOException e) {
            lblMensaje.setText("No se pudo volver al selector de perfiles");
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
    private void abrirInsumos() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/insumos_admin.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Administración de Insumos");
            stage.setScene(new Scene(root));
            stage.show();
            UserService.registrarActividad(SessionManager.getCurrentUser(), "Abrir insumos");
        } catch (IOException | SQLException e) {
            lblMensaje.setText("Error al abrir insumos");
        }
    }

    @FXML
    private void abrirEquipos() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/equipos_admin.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Administración de Equipos");
            stage.setScene(new Scene(root));
            stage.show();
            UserService.registrarActividad(SessionManager.getCurrentUser(), "Abrir equipos");
        } catch (IOException | SQLException e) {
            lblMensaje.setText("Error al abrir equipos");
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
}
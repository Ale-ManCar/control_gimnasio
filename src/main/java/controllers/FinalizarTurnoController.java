package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import models.Turno;
import util.DatabaseUtil;
import util.SessionManager;
import util.ReporteUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class FinalizarTurnoController implements Initializable {
    @FXML private Label lblStockInicial;
    @FXML private Label lblStockFinal;
    @FXML private Label lblIngresosVentas;
    @FXML private Label lblIngresosClientes;

    private Turno turno;
    private String stockFinal;
    private double ingresosVentas;
    private double ingresosClientes;
    private Stage dashboardStage;
    private static final DateTimeFormatter TURNO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            turno = DatabaseUtil.obtenerTurnoActivo(SessionManager.getCurrentUser().getId());
            if (turno != null) {
                stockFinal = DatabaseUtil.obtenerStockJson();
                ingresosVentas = DatabaseUtil.obtenerTotalVentasDesde(turno.getFecha_inicio());
                ingresosClientes = DatabaseUtil.obtenerTotalPagosDesde(turno.getFecha_inicio());
                lblStockInicial.setText(turno.getStock_inicial());
                lblStockFinal.setText(stockFinal);
                lblIngresosVentas.setText(String.format("%.2f", ingresosVentas));
                lblIngresosClientes.setText(String.format("%.2f", ingresosClientes));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDashboardStage(Stage stage) {
        this.dashboardStage = stage;
    }

    @FXML
    private void confirmar(ActionEvent event) {
        try {
            DatabaseUtil.finalizarTurno(turno.getId(), stockFinal, ingresosVentas, ingresosClientes);
            LocalDateTime inicioTurno = obtenerInicioTurno();
            ReporteUtil.generarResumenTurno(SessionManager.getCurrentUser().getId(), inicioTurno, LocalDateTime.now());
            enviarResumen();
            SessionManager.clear();
            Stage current = (Stage) lblStockInicial.getScene().getWindow();
            current.close();
            if (dashboardStage != null) {
                dashboardStage.close();
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/selector_perfiles.fxml"));
            Parent root = loader.load();
            SelectorPerfilesController controller = loader.getController();
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 700, 420));
            controller.setStage(stage);
            stage.setTitle("Seleccionar perfil");
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private LocalDateTime obtenerInicioTurno() {
        if (turno == null || turno.getFecha_inicio() == null) {
            return LocalDateTime.now();
        }
        String fechaInicio = turno.getFecha_inicio();
        try {
            if (fechaInicio.contains("T")) {
                return LocalDateTime.parse(fechaInicio);
            }
            return LocalDateTime.parse(fechaInicio, TURNO_FORMATTER);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private void enviarResumen() {
        String resumen = "Turno " + turno.getId() + "\n" +
                "Stock inicial: " + turno.getStock_inicial() + "\n" +
                "Stock final: " + stockFinal + "\n" +
                "Ingresos ventas: " + ingresosVentas + "\n" +
                "Ingresos clientes: " + ingresosClientes;
        System.out.println(resumen);
    }

    @FXML
    private void cancelar(ActionEvent event) {
        ((Stage) lblStockInicial.getScene().getWindow()).close();
    }
}
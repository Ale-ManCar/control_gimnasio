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
            enviarResumen();
            SessionManager.clear();
            Stage current = (Stage) lblStockInicial.getScene().getWindow();
            current.close();
            if (dashboardStage != null) {
                dashboardStage.close();
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
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
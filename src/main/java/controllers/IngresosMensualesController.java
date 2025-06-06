package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.event.ActionEvent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import models.PagoMensual;
import util.DatabaseUtil;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class IngresosMensualesController implements Initializable {

    @FXML
    private BarChart<String, Number> barChart;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cargarDatosGrafico();
    }

    private void cargarDatosGrafico() {
        XYChart.Series<String, Number> datos = new XYChart.Series<>();
        datos.setName("Ingresos");

        try {
            List<PagoMensual> ingresos = DatabaseUtil.getIngresosMensuales();

            for (PagoMensual ingreso : ingresos) {
                String mes = ingreso.getMes();
                double total = ingreso.getTotal();
                datos.getData().add(new XYChart.Data<>(mes, total));
            }

            barChart.getData().clear();
            barChart.getData().add(datos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleVolver(ActionEvent event) {
        Stage stage = (Stage) barChart.getScene().getWindow();
        stage.close();
    }
}
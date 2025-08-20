package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

import util.ReporteUtil;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ReportesController implements Initializable {

    @FXML private DatePicker dpFecha;
    @FXML private Spinner<Integer> spMes;
    @FXML private Spinner<Integer> spAnio;
    @FXML private ListView<String> lstArchivos;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LocalDate hoy = LocalDate.now();
        dpFecha.setValue(null);
        spMes.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 12, hoy.getMonthValue()));
        spAnio.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2000, 2100, hoy.getYear()));
        spMes.valueProperty().addListener((o, ov, nv) -> listarArchivos());
        spAnio.valueProperty().addListener((o, ov, nv) -> listarArchivos());
        listarArchivos();
    }

    @FXML
    private void generarPdf() {
        generarReporte();
    }

    @FXML
    private void generarCsv() {
        generarReporte();
    }

    private void generarReporte() {
        try {
            LocalDate fecha = dpFecha.getValue();
            int mes = spMes.getValue();
            int anio = spAnio.getValue();
            if (fecha != null) {
                ReporteUtil.generarReporteDiario(fecha);
            } else if (mes != null && mes > 0) {
                ReporteUtil.generarReporteMensual(anio, mes);
            } else {
                ReporteUtil.generarReporteAnual(anio);
            }
            mostrarMensaje("Reporte generado correctamente");
            listarArchivos();
        } catch (Exception e) {
            mostrarError("Error al generar reporte: " + e.getMessage());
        }
    }

    @FXML
    private void abrirCarpeta() {
        int mes = spMes.getValue();
        int anio = spAnio.getValue();
        Path dir = Paths.get("reports", String.format("%04d", anio), String.format("%02d", mes));
        try {
            if (Files.exists(dir)) {
                Desktop.getDesktop().open(dir.toFile());
            }
        } catch (IOException e) {
            mostrarError("No se pudo abrir la carpeta: " + e.getMessage());
        }
    }

    private void listarArchivos() {
        int mes = spMes.getValue();
        int anio = spAnio.getValue();
        Path dir = Paths.get("reports", String.format("%04d", anio), String.format("%02d", mes));
        try {
            if (Files.exists(dir)) {
                lstArchivos.getItems().setAll(Files.list(dir)
                        .map(p -> p.getFileName().toString())
                        .sorted()
                        .collect(Collectors.toList()));
            } else {
                lstArchivos.getItems().clear();
            }
        } catch (IOException e) {
            lstArchivos.getItems().clear();
        }
    }

    private void mostrarMensaje(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    private void mostrarError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import models.PagoDetalle;
import util.DatabaseUtil;
import util.ReporteUtil;

import java.sql.SQLException;
import java.time.LocalDate;

public class HistorialPagosController {

    @FXML private TextField txtClienteId;
    @FXML private DatePicker dpFechaInicio;
    @FXML private DatePicker dpFechaFin;
    @FXML private ComboBox<String> cbTipoMembresia;
    @FXML private TableView<PagoDetalle> tablaPagos;
    @FXML private TableColumn<PagoDetalle, LocalDate> colFecha;
    @FXML private TableColumn<PagoDetalle, String> colCliente;
    @FXML private TableColumn<PagoDetalle, String> colMembresia;
    @FXML private TableColumn<PagoDetalle, Double> colMonto;
    @FXML private Button btnBuscar;
    @FXML private Button btnExportar;

    private ObservableList<PagoDetalle> pagos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("cliente"));
        colMembresia.setCellValueFactory(new PropertyValueFactory<>("membresia"));
        colMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));
        tablaPagos.setItems(pagos);
    }

    @FXML
    private void handleBuscar(ActionEvent e) {
        Integer clienteId = null;
        if (!txtClienteId.getText().trim().isEmpty()) {
            try {
                clienteId = Integer.parseInt(txtClienteId.getText().trim());
            } catch (NumberFormatException ex) {
                // ignore invalid id
            }
        }
        LocalDate inicio = dpFechaInicio.getValue();
        LocalDate fin = dpFechaFin.getValue();
        String tipo = cbTipoMembresia.getValue();
        try {
            pagos.setAll(DatabaseUtil.buscarPagos(clienteId, inicio, fin, tipo));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleExportar(ActionEvent e) {
        Integer clienteId = null;
        if (!txtClienteId.getText().trim().isEmpty()) {
            try {
                clienteId = Integer.parseInt(txtClienteId.getText().trim());
            } catch (NumberFormatException ignored) {}
        }
        LocalDate inicio = dpFechaInicio.getValue();
        LocalDate fin = dpFechaFin.getValue();
        String tipo = cbTipoMembresia.getValue();
        ReporteUtil.generarReportePagosFiltrado(clienteId, inicio, fin, tipo);
    }
}
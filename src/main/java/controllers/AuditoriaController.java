package controllers;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import models.Auditoria;
import util.AuditoriaUtil;
import util.SessionManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AuditoriaController implements Initializable {

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private TextField txtUsuario;
    @FXML private TextField txtAccion;
    @FXML private TextField txtEntidad;
    @FXML private TableView<Auditoria> tablaAuditoria;
    @FXML private TableColumn<Auditoria, String> colUsuario;
    @FXML private TableColumn<Auditoria, String> colFecha;
    @FXML private TableColumn<Auditoria, String> colAccion;
    @FXML private TableColumn<Auditoria, String> colEntidad;
    @FXML private TableColumn<Auditoria, Integer> colIdEntidad;
    @FXML private TableColumn<Auditoria, String> colDetalle;
    @FXML private Button btnFiltrar;
    @FXML private Button btnExportarCSV;
    @FXML private Button btnExportarPDF;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!SessionManager.isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Permiso denegado");
            alert.showAndWait();
            ((javafx.stage.Stage) btnFiltrar.getScene().getWindow()).close();
            return;
        }
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        colAccion.setCellValueFactory(new PropertyValueFactory<>("accion"));
        colEntidad.setCellValueFactory(new PropertyValueFactory<>("entidad"));
        colIdEntidad.setCellValueFactory(new PropertyValueFactory<>("idEntidad"));
        colDetalle.setCellValueFactory(new PropertyValueFactory<>("detalle"));
        cargarAuditoria();
    }

    private void cargarAuditoria() {
        ObservableList<Auditoria> datos = AuditoriaUtil.buscar(dpDesde.getValue(), dpHasta.getValue(),
                txtUsuario.getText(), txtAccion.getText(), txtEntidad.getText());
        tablaAuditoria.setItems(datos);
    }

    @FXML
    private void handleFiltrar(ActionEvent e) {
        if (!SessionManager.isAdmin()) return;
        cargarAuditoria();
    }

    @FXML
    private void handleExportarCSV(ActionEvent e) {
        if (!SessionManager.isAdmin()) return;
        try {
            FileChooser fc = new FileChooser();
            fc.setInitialFileName("auditoria.csv");
            File f = fc.showSaveDialog(((Button) e.getSource()).getScene().getWindow());
            if (f != null) {
                AuditoriaUtil.exportarCSV(tablaAuditoria.getItems(), f);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleExportarPDF(ActionEvent e) {
        if (!SessionManager.isAdmin()) return;
        try {
            FileChooser fc = new FileChooser();
            fc.setInitialFileName("auditoria.pdf");
            File f = fc.showSaveDialog(((Button) e.getSource()).getScene().getWindow());
            if (f != null) {
                AuditoriaUtil.exportarPDF(tablaAuditoria.getItems(), f);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
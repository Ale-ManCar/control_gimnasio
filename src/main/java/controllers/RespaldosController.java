package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import util.AuditoriaUtil;
import util.BackupUtil;
import util.SessionManager;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class RespaldosController implements Initializable {

    @FXML private TableView<BackupUtil.RespaldoInfo> tblRespaldos;
    @FXML private TableColumn<BackupUtil.RespaldoInfo, String> colFecha;
    @FXML private TableColumn<BackupUtil.RespaldoInfo, String> colTamano;
    @FXML private TableColumn<BackupUtil.RespaldoInfo, String> colRuta;

    private final ObservableList<BackupUtil.RespaldoInfo> respaldos = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colFecha.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getFechaHora().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        colTamano.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                BackupUtil.formatearTamano(data.getValue().getTamano())));
        colRuta.setCellValueFactory(new PropertyValueFactory<>("ruta"));

        respaldos.setAll(BackupUtil.listarRespaldos());
        tblRespaldos.setItems(respaldos);
    }

    @FXML
    private void generarRespaldo() {
        try {
            String destino = BackupUtil.generarNombreRespaldo();
            BackupUtil.crearZip(destino);
            AuditoriaUtil.registrar(SessionManager.getUsuarioActual().getNombre(),
                    "RESPALDO_GENERADO", "RESPALDO", null, destino);
            respaldos.setAll(BackupUtil.listarRespaldos());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
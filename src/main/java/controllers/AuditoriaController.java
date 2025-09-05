package controllers;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import models.Auditoria;
import util.DatabaseUtil;
import util.SessionManager;
import models.Role;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AuditoriaController implements Initializable {

    @FXML private TableView<Auditoria> tablaAuditoria;
    @FXML private TableColumn<Auditoria, Integer> colId;
    @FXML private TableColumn<Auditoria, String> colUsuario;
    @FXML private TableColumn<Auditoria, String> colAccion;
    @FXML private TableColumn<Auditoria, String> colDetalle;
    @FXML private TableColumn<Auditoria, String> colFecha;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (!SessionManager.tienePermiso(Role.ADMIN)) {
            return;
        }
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colAccion.setCellValueFactory(new PropertyValueFactory<>("accion"));
        colDetalle.setCellValueFactory(new PropertyValueFactory<>("detalle"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        try {
            ObservableList<Auditoria> registros = DatabaseUtil.getAuditoria();
            tablaAuditoria.setItems(registros);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
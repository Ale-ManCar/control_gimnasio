package controllers;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import models.Auditoria;
import models.Role;
import models.User;
import util.AuditoriaUtil;
import util.ReporteUtil;
import util.SessionManager;
import util.UserService;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class AuditoriaController implements Initializable {

    @FXML private TableView<Auditoria> tablaAuditoria;
    @FXML private TableColumn<Auditoria, Integer> colId;
    @FXML private TableColumn<Auditoria, String> colUsuario;
    @FXML private TableColumn<Auditoria, String> colAccion;
    @FXML private TableColumn<Auditoria, String> colFecha;
    @FXML private ChoiceBox<User> cbUsuarios;
    @FXML private DatePicker dpFechaInicio;
    @FXML private DatePicker dpFechaFin;
    @FXML private TextField txtTipo;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (!SessionManager.tienePermiso(Role.ADMIN)) {
            return;
        }
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colAccion.setCellValueFactory(new PropertyValueFactory<>("accion"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        try {
            ObservableList<User> usuarios = UserService.listarUsuarios();
            User todos = new User(0, "Todos", "", Role.ADMIN);
            usuarios.add(0, todos);
            cbUsuarios.setItems(usuarios);
            cbUsuarios.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(User user) {
                    return user != null ? user.getUsername() : "";
                }

                @Override
                public User fromString(String string) {
                    return null;
                }
            });
            cbUsuarios.getSelectionModel().selectFirst();
            cbUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, old, user) -> aplicarFiltros());
            aplicarFiltros();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void aplicarFiltros() {
        User seleccionado = cbUsuarios.getSelectionModel().getSelectedItem();
        int usuarioId = seleccionado != null ? seleccionado.getId() : 0;
        LocalDate inicio = dpFechaInicio != null ? dpFechaInicio.getValue() : null;
        LocalDate fin = dpFechaFin != null ? dpFechaFin.getValue() : null;
        String tipo = txtTipo != null ? txtTipo.getText() : null;
        ObservableList<Auditoria> registros = AuditoriaUtil.filtrarAcciones(usuarioId, inicio, fin, tipo);
        tablaAuditoria.setItems(registros);
    }

    @FXML
    private void exportarReporte() {
        if (tablaAuditoria.getItems() != null) {
            ReporteUtil.generarReporteAuditoria(tablaAuditoria.getItems());
        }
    }
}
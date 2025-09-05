package controllers;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import models.AuditoriaUsuario;
import models.User;
import models.Role;
import util.DatabaseUtil;
import util.SessionManager;
import util.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AuditoriaController implements Initializable {

    @FXML private TableView<AuditoriaUsuario> tablaAuditoria;
    @FXML private TableColumn<AuditoriaUsuario, Integer> colId;
    @FXML private TableColumn<AuditoriaUsuario, String> colUsuario;
    @FXML private TableColumn<AuditoriaUsuario, String> colAccion;
    @FXML private TableColumn<AuditoriaUsuario, String> colFecha;
    @FXML private ChoiceBox<User> cbUsuarios;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (!SessionManager.tienePermiso(Role.ADMIN)) {
            return;
        }
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colAccion.setCellValueFactory(new PropertyValueFactory<>("accion"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));

        try {
            ObservableList<User> usuarios = UserService.listarUsuarios();
            User todos = new User(0, "Todos", "", Role.ADMIN);
            usuarios.add(0, todos);
            cbUsuarios.setItems(usuarios);
            cbUsuarios.setConverter(new javafx.util.StringConverter<User>() {
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
            cargarAcciones(0);
            cbUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, old, user) -> {
                if (user != null) {
                    cargarAcciones(user.getId());
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void cargarAcciones(int usuarioId) {
        try {
            ObservableList<AuditoriaUsuario> registros = DatabaseUtil.listarAccionesPorUsuario(usuarioId);
            tablaAuditoria.setItems(registros);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
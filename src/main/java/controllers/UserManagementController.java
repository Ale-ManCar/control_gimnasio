package controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import models.User;
import util.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class UserManagementController implements Initializable {

    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colLastLogin;
    @FXML private TableColumn<User, Integer> colAcciones;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colUsername.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("role"));
        colLastLogin.setCellValueFactory(cellData -> {
            if (cellData.getValue().getLastLogin() == null) {
                return new SimpleStringProperty("");
            }
            return new SimpleStringProperty(cellData.getValue().getLastLogin().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        });
        colAcciones.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("accionesRealizadas"));
        cargarUsuarios();
    }

    private void cargarUsuarios() {
        try {
            ObservableList<User> users = UserService.listarUsuarios();
            tableUsers.setItems(users);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
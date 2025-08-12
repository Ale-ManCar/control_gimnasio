package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import models.Coach;
import util.DatabaseUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ListaCoachesController {

    @FXML private TableView<Coach> tablaCoaches;
    @FXML private TableColumn<Coach, String> colNombre;
    @FXML private TableColumn<Coach, String> colArea;
    @FXML private TableColumn<Coach, Void> colAcciones;

    private final ObservableList<Coach> coaches = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colArea.setCellValueFactory(new PropertyValueFactory<>("area"));

        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnPerfil = new Button("Perfil");
            {
                btnPerfil.setOnAction(e -> {
                    Coach coach = getTableView().getItems().get(getIndex());
                    verPerfil(coach);
                });
                btnPerfil.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5px;");
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnPerfil);
            }
        });

        tablaCoaches.setItems(coaches);
        cargarCoaches();
    }

    private void cargarCoaches() {
        coaches.clear();
        String sql = "SELECT id, nombres, apellidos, area, telefono, foto_path FROM coaches";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                coaches.add(new Coach(
                        rs.getInt("id"),
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("area"),
                        rs.getString("telefono"),
                        rs.getString("foto_path")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void verPerfil(Coach coach) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/perfil_coach.fxml"));
            Parent root = loader.load();
            PerfilCoachController controller = loader.getController();
            controller.setCoach(coach);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Perfil del Coach");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void volverRegistro(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/registro_coach.fxml"));
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Registro de Coach");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
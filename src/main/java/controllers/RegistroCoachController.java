package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.Coach;
import util.DatabaseUtil;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RegistroCoachController {

    @FXML private TextField txtNombres;
    @FXML private TextField txtApellidos;
    @FXML private TextField txtTelefono;
    @FXML private ComboBox<String> cbArea;
    @FXML private ImageView imgFoto;
    @FXML private TableView<Coach> tablaCoaches;
    @FXML private TableColumn<Coach, String> colNombre;
    @FXML private TableColumn<Coach, String> colArea;
    @FXML private TableColumn<Coach, Void> colAcciones;

    private final ObservableList<Coach> coaches = FXCollections.observableArrayList();
    private String fotoPath = null;

    @FXML
    public void initialize() {
        cbArea.getItems().addAll("Maquinas", "Bailoterapia", "Crossfit");

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

    @FXML
    private void seleccionarFoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(txtNombres.getScene().getWindow());
        if (file != null) {
            fotoPath = file.getAbsolutePath();
            imgFoto.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    private void guardarCoach(ActionEvent event) {
        if (txtNombres.getText().isEmpty() || txtApellidos.getText().isEmpty() ||
                cbArea.getValue() == null) {
            mostrarAlerta("Debe completar todos los campos");
            return;
        }

        String sql = "INSERT INTO coaches (nombres, apellidos, telefono, area, foto_path) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, txtNombres.getText().trim());
            stmt.setString(2, txtApellidos.getText().trim());
            stmt.setString(3, txtTelefono.getText().trim());
            stmt.setString(4, cbArea.getValue());
            stmt.setString(5, fotoPath);
            stmt.executeUpdate();
            limpiarFormulario();
            cargarCoaches();
        } catch (SQLException e) {
            mostrarAlerta("No se pudo guardar el coach: " + e.getMessage());
        }
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

    private void limpiarFormulario() {
        txtNombres.clear();
        txtApellidos.clear();
        txtTelefono.clear();
        cbArea.setValue(null);
        imgFoto.setImage(null);
        fotoPath = null;
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @FXML
    private void volverDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Panel de Control");
        } catch (IOException e) {
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
}
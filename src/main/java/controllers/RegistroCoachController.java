package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import util.DatabaseUtil;
import models.Coach;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RegistroCoachController {

    @FXML private TextField txtNombres;
    @FXML private TextField txtApellidos;
    @FXML private TextField txtTelefono;
    @FXML private ComboBox<String> cbArea;
    @FXML private ImageView imgFoto;
    @FXML private Button btnGuardar;

    private String fotoPath = null;
    private Coach coachEditando = null;

    @FXML
    public void initialize() {
        cbArea.getItems().addAll("Maquinas", "Bailoterapia", "Crossfit");

        txtNombres.setTextFormatter(new TextFormatter<>(change -> {
            change.setText(change.getText().toUpperCase());
            return change;
        }));

        txtApellidos.setTextFormatter(new TextFormatter<>(change -> {
            change.setText(change.getText().toUpperCase());
            return change;
        }));

        txtTelefono.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0,10}") ? change : null));
    }

    public void setCoach(Coach coach) {
        this.coachEditando = coach;
        txtNombres.setText(coach.getNombres());
        txtApellidos.setText(coach.getApellidos());
        txtTelefono.setText(coach.getTelefono());
        cbArea.setValue(coach.getArea());
        fotoPath = coach.getFotoPath();
        if (fotoPath != null) {
            imgFoto.setImage(new Image(new File(fotoPath).toURI().toString()));
        }
        btnGuardar.setText("Actualizar");
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

        if (txtTelefono.getText().length() != 10) {
            mostrarAlerta("El número de teléfono debe tener exactamente 10 dígitos");
            return;
        }

        if (coachEditando == null) {
            String sql = "INSERT INTO coaches (nombres, apellidos, telefono, area, foto_path) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, txtNombres.getText().trim().toUpperCase());
                stmt.setString(2, txtApellidos.getText().trim().toUpperCase());
                stmt.setString(3, txtTelefono.getText().trim());
                stmt.setString(4, cbArea.getValue());
                stmt.setString(5, fotoPath);
                stmt.executeUpdate();
                limpiarFormulario();
            } catch (SQLException e) {
                mostrarAlerta("No se pudo guardar el coach: " + e.getMessage());
            }
        } else {
            String sql = "UPDATE coaches SET nombres = ?, apellidos = ?, telefono = ?, area = ?, foto_path = ? WHERE id = ?";
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, txtNombres.getText().trim().toUpperCase());
                stmt.setString(2, txtApellidos.getText().trim().toUpperCase());
                stmt.setString(3, txtTelefono.getText().trim());
                stmt.setString(4, cbArea.getValue());
                stmt.setString(5, fotoPath);
                stmt.setInt(6, coachEditando.getId());
                stmt.executeUpdate();
                Stage stage = (Stage) btnGuardar.getScene().getWindow();
                stage.close();
            } catch (SQLException e) {
                mostrarAlerta("No se pudo actualizar el coach: " + e.getMessage());
            }
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
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/recepcionista_dashboard.fxml"));
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Panel de Control");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void abrirListaCoaches(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/lista_coaches.fxml"));
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Lista de Coaches");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
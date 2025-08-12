package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import models.Cliente;
import models.Coach;
import util.DatabaseUtil;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class PerfilCoachController {

    @FXML private ImageView imgFoto;
    @FXML private Label lblNombre;
    @FXML private Label lblArea;
    @FXML private Label lblTelefono;
    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, String> colCliente;
    @FXML private TableColumn<Cliente, String> colTelefono;

    @FXML
    public void initialize() {
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
    }

    public void setCoach(Coach coach) {
        lblNombre.setText(coach.getNombreCompleto());
        lblArea.setText("Área: " + coach.getArea());
        lblTelefono.setText("Tel: " + coach.getTelefono());
        if (coach.getFotoPath() != null) {
            imgFoto.setImage(new Image(new File(coach.getFotoPath()).toURI().toString()));
        }
        cargarClientes(coach.getId());
    }

    private void cargarClientes(int coachId) {
        tablaClientes.getItems().clear();
        String sql = "SELECT nombres, apellidos, telefono, fecha_vencimiento FROM clientes WHERE coach_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, coachId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tablaClientes.getItems().add(new Cliente(
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("telefono"),
                        LocalDate.parse(rs.getString("fecha_vencimiento"))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCerrar() {
        Stage stage = (Stage) tablaClientes.getScene().getWindow();
        stage.close();
    }
}
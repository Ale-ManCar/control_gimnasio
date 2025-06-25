package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import models.Cliente;
import util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class ListaClientesController {

    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, String> colNombre;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colVencimiento;

    @FXML
    public void initialize() {
        configurarColumnas();
        cargarClientes();
    }

    private void configurarColumnas() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombres"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colVencimiento.setCellValueFactory(new PropertyValueFactory<>("fechaVencimiento"));
    }

    private void cargarClientes() {
        String sql = "SELECT nombres, telefono, fecha_vencimiento FROM clientes WHERE activo = 1";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                // Usar el constructor CORRECTO con 4 parámetros
                Cliente cliente = new Cliente(
                        rs.getString("nombres"),
                        rs.getString("telefono"),
                        "Tipo no disponible", // Valor por defecto para tipoMembresia
                        LocalDate.parse(rs.getString("fecha_vencimiento")) // Fecha como LocalDate
                );

                tablaClientes.getItems().add(cliente);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleVolver() {
        try {
            Stage stage = (Stage) tablaClientes.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
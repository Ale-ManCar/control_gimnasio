package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import models.Cliente;
import util.DatabaseUtil;

import java.sql.*;
import java.time.LocalDate;

public class RenovacionController {
    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, String> colNombre;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, LocalDate> colVence;

    @FXML private ComboBox<String> cbNuevaMembresia;
    @FXML private DatePicker dpFechaRenovacion;

    private final ObservableList<Cliente> clientes = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Configurar columnas
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombres"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colVence.setCellValueFactory(new PropertyValueFactory<>("fechaVencimiento"));

        // Configurar ComboBox
        cbNuevaMembresia.getItems().addAll("1 Mes", "3 Meses", "6 Meses", "1 Año");
        dpFechaRenovacion.setValue(LocalDate.now());

        // Cargar clientes próximos a vencer
        cargarClientesProximos();
    }

    private void cargarClientesProximos() {
        String sql = "SELECT nombres, telefono, fechaVencimiento FROM clientes " +
                "WHERE fechaVencimiento BETWEEN date('now') AND date('now', '+7 days')";

        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            clientes.clear();
            while (rs.next()) {
                clientes.add(new Cliente(
                        rs.getString("nombres"),
                        rs.getString("telefono"),
                        LocalDate.parse(rs.getString("fechaVencimiento")))
                );
            }
            tablaClientes.setItems(clientes);

        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudieron cargar los clientes");
        }
    }

    @FXML
    private void handleRenovar() {
        Cliente seleccionado = tablaClientes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Error", "Selecciona un cliente");
            return;
        }

        // Lógica de renovación aquí (se implementará en el siguiente paso)
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}

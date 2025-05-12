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
    @FXML private TableColumn<Cliente, String> colVence;
    @FXML private TableColumn<Cliente, String> colMembresia;

    @FXML private ComboBox<String> cbNuevaMembresia;
    @FXML private DatePicker dpFechaRenovacion;

    private final ObservableList<Cliente> clientes = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombres"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colVence.setCellValueFactory(new PropertyValueFactory<>("fechaVencimiento"));
        colMembresia.setCellValueFactory(new PropertyValueFactory<>("tipoMembresia"));

        cbNuevaMembresia.getItems().addAll("1 Mes", "3 Meses", "6 Meses", "1 Año");
        dpFechaRenovacion.setValue(LocalDate.now());

        cargarClientesProximos();
    }

    private void cargarClientesProximos() {
        String sql = "SELECT nombres, telefono, tipoMembresia, fechaVencimiento FROM clientes " +
                "WHERE fechaVencimiento BETWEEN date('now') AND date('now', '+7 days')";

        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            clientes.clear();
            while (rs.next()) {
                clientes.add(new Cliente(
                        rs.getString("nombres"),
                        rs.getString("telefono"),
                        rs.getString("tipoMembresia"),
                        LocalDate.parse(rs.getString("fechaVencimiento"))
                ));
            }
            tablaClientes.setItems(clientes);

        } catch (SQLException e) {
            mostrarAlerta("Error", "Error al cargar clientes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRenovar() {
        Cliente seleccionado = tablaClientes.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta("Error", "Selecciona un cliente de la tabla");
            return;
        }

        if (cbNuevaMembresia.getValue() == null || dpFechaRenovacion.getValue() == null) {
            mostrarAlerta("Error", "Selecciona tipo de membresía y fecha de renovación");
            return;
        }

        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "UPDATE clientes SET tipoMembresia = ?, fechaVencimiento = ? WHERE telefono = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);

            LocalDate nuevaFecha = calcularNuevaFecha(
                    dpFechaRenovacion.getValue(),
                    cbNuevaMembresia.getValue()
            );

            stmt.setString(1, cbNuevaMembresia.getValue());
            stmt.setString(2, nuevaFecha.toString());
            stmt.setString(3, seleccionado.getTelefono());

            int filasActualizadas = stmt.executeUpdate();

            if (filasActualizadas > 0) {
                mostrarAlerta("Éxito", "Membresía renovada hasta: " + nuevaFecha);
                cargarClientesProximos();
            } else {
                mostrarAlerta("Error", "No se pudo actualizar la membresía");
            }

        } catch (SQLException e) {
            mostrarAlerta("Error", "Error en base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private LocalDate calcularNuevaFecha(LocalDate fechaRenovacion, String membresia) {
        return switch (membresia) {
            case "1 Mes" -> fechaRenovacion.plusMonths(1);
            case "3 Meses" -> fechaRenovacion.plusMonths(3);
            case "6 Meses" -> fechaRenovacion.plusMonths(6);
            case "1 Año" -> fechaRenovacion.plusYears(1);
            default -> fechaRenovacion;
        };
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
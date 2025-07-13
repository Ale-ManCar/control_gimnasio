package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Cliente;
import util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class EditarClienteController {

    @FXML private TextField txtNombres;
    @FXML private TextField txtApellidos;
    @FXML private TextField txtTelefono;
    @FXML private Button btnGuardar;

    private Cliente cliente;

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
        txtNombres.setText(cliente.getNombres());
        txtApellidos.setText(cliente.getApellidos());
        txtTelefono.setText(cliente.getTelefono());

        // Validación de máximo 10 dígitos para teléfono
        txtTelefono.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtTelefono.setText(newValue.replaceAll("[^\\d]", ""));
            }
            if (newValue.length() > 10) {
                txtTelefono.setText(oldValue);
            }
        });
    }

    @FXML
    private void handleGuardar() {
        String nuevosNombres = txtNombres.getText().trim();
        String nuevosApellidos = txtApellidos.getText().trim();
        String nuevoTelefono = txtTelefono.getText().trim();

        if (nuevosNombres.isEmpty() || nuevosApellidos.isEmpty() || nuevoTelefono.isEmpty()) {
            mostrarAlerta("Error", "Todos los campos son obligatorios");
            return;
        }

        if (nuevoTelefono.length() != 10) {
            mostrarAlerta("Error", "El teléfono debe tener 10 dígitos");
            return;
        }

        actualizarClienteEnBD(nuevosNombres, nuevosApellidos, nuevoTelefono);
    }

    private void actualizarClienteEnBD(String nombres, String apellidos, String telefono) {
        String sql = "UPDATE clientes SET nombres = ?, apellidos = ?, telefono = ? WHERE telefono = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nombres);
            stmt.setString(2, apellidos);
            stmt.setString(3, telefono);
            stmt.setString(4, cliente.getTelefono()); // Teléfono original para buscar

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas > 0) {
                mostrarAlerta("Éxito", "Cliente editado exitosamente");
                cerrarVentana();
            } else {
                mostrarAlerta("Error", "No se pudo actualizar el cliente");
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "Error al actualizar en la base de datos");
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        stage.close();
    }
}
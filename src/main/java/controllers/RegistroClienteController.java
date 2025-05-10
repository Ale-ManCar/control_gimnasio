package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import util.DatabaseUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

public class RegistroClienteController {
    @FXML private TextField txtNombres, txtApellidos, txtTelefono;
    @FXML private DatePicker dpFechaInicio;
    @FXML private ComboBox<String> cbMembresia;
    @FXML private Button btnSiguiente;

    @FXML
    public void initialize() {
        // Verificar que el ComboBox no sea nulo
        if (cbMembresia != null) {
            // Limpiar cualquier dato previo
            cbMembresia.getItems().clear();

            // Agregar las opciones de membresía
            cbMembresia.getItems().addAll(
                    "1 Mes",
                    "3 Meses",
                    "6 Meses",
                    "1 Año"
            );

            // Seleccionar un valor por defecto
            cbMembresia.setValue("1 Mes");

            // Listener para debug
            cbMembresia.setOnAction(event -> {
                System.out.println("Membresía seleccionada: " + cbMembresia.getValue());
            });
        } else {
            System.err.println("Error: ComboBox cbMembresia es null");
        }

        // Validación del teléfono
        txtTelefono.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtTelefono.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
    }

    @FXML
    private void handleSiguiente() {
        // Validar campos obligatorios
        if (cbMembresia.getValue() == null || dpFechaInicio.getValue() == null) {
            mostrarAlerta("Error", "Debe completar todos los campos");
            return;
        }

        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "INSERT INTO clientes (nombres, apellidos, telefono, tipoMembresia, fechaInicio, fechaVencimiento) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);

            // Validar y asignar valores
            stmt.setString(1, validarCampo(txtNombres.getText(), "Nombres"));
            stmt.setString(2, validarCampo(txtApellidos.getText(), "Apellidos"));
            stmt.setString(3, validarCampo(txtTelefono.getText(), "Teléfono"));
            stmt.setString(4, cbMembresia.getValue());

            LocalDate fechaInicio = dpFechaInicio.getValue();
            stmt.setString(5, fechaInicio.toString());

            // Calcular fecha de vencimiento
            LocalDate fechaVencimiento = calcularVencimiento(fechaInicio, cbMembresia.getValue());
            stmt.setString(6, fechaVencimiento.toString());

            stmt.executeUpdate();
            mostrarAlerta("Éxito", "Cliente registrado correctamente");

            limpiarCampos();

        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo registrar el cliente: " + e.getMessage());
        }
    }

    private LocalDate calcularVencimiento(LocalDate fechaInicio, String membresia) {
        return switch (membresia) {
            case "1 Mes" -> fechaInicio.plusMonths(1);
            case "3 Meses" -> fechaInicio.plusMonths(3);
            case "6 Meses" -> fechaInicio.plusMonths(6);
            case "1 Año" -> fechaInicio.plusYears(1);
            default -> fechaInicio;
        };
    }

    private String validarCampo(String valor, String nombreCampo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException(nombreCampo + " es requerido");
        }
        return valor.trim();
    }

    private void limpiarCampos() {
        txtNombres.clear();
        txtApellidos.clear();
        txtTelefono.clear();
        dpFechaInicio.setValue(null);
        cbMembresia.setValue("1 Mes"); // Restablecer al valor por defecto
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
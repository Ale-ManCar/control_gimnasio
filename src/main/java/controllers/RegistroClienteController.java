package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import util.DatabaseUtil;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

public class RegistroClienteController {
    @FXML private TextField txtNombres, txtApellidos, txtTelefono;
    @FXML private DatePicker dpFechaInicio;
    @FXML private ComboBox<String> cbMembresia;
    @FXML private Button btnSiguiente;
    @FXML private Button btnIrARenovaciones; // Nuevo botón para navegación

    @FXML
    public void initialize() {
        // Configuración inicial del ComboBox
        if (cbMembresia != null) {
            cbMembresia.getItems().clear();
            cbMembresia.getItems().addAll("1 Mes", "3 Meses", "6 Meses", "1 Año");
            cbMembresia.setValue("1 Mes");
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
        // Validación de campos
        if (cbMembresia.getValue() == null || dpFechaInicio.getValue() == null) {
            mostrarAlerta("Error", "Debe completar todos los campos");
            return;
        }

        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "INSERT INTO clientes (nombres, apellidos, telefono, tipoMembresia, fechaInicio, fechaVencimiento) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setString(1, validarCampo(txtNombres.getText(), "Nombres"));
            stmt.setString(2, validarCampo(txtApellidos.getText(), "Apellidos"));
            stmt.setString(3, validarCampo(txtTelefono.getText(), "Teléfono"));
            stmt.setString(4, cbMembresia.getValue());
            stmt.setString(5, dpFechaInicio.getValue().toString());
            stmt.setString(6, calcularVencimiento(dpFechaInicio.getValue(), cbMembresia.getValue()).toString());

            stmt.executeUpdate();
            mostrarAlerta("Éxito", "Cliente registrado correctamente");
            limpiarCampos();

        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudo registrar el cliente: " + e.getMessage());
        }
    }

    @FXML
    private void handleIrARenovaciones() {
        try {
            // Cargar la pantalla de renovaciones
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/renovacion.fxml"));
            Stage stage = (Stage) btnIrARenovaciones.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Renovación de Membresías");
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo cargar la pantalla de renovaciones");
            e.printStackTrace();
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
        cbMembresia.setValue("1 Mes");
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
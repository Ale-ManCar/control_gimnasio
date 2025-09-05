package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import models.Egreso;
import util.DatabaseUtil;
import util.EventBus;
import util.AuditoriaUtil;
import util.SessionManager;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

public class RegistroEgresoController implements Initializable {

    @FXML private TextField txtMonto;
    @FXML private TextArea txtDescripcion;
    @FXML private ComboBox<String> cbCategoria;
    @FXML private Button btnRegistrar;
    @FXML private Button btnCancelar;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configurar categorías
        cbCategoria.getItems().addAll(
                "Alquiler",
                "Servicios",
                "Mantenimiento",
                "Insumos",
                "Salarios",
                "Marketing",
                "Otros"
        );
        cbCategoria.getSelectionModel().selectFirst();

        // Configurar conversión a mayúsculas
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getText();
            if (!text.isEmpty()) {
                change.setText(text.toUpperCase());
            }
            return change;
        };
        txtDescripcion.setTextFormatter(new TextFormatter<>(filter));

        // Configurar TextArea para salto de línea automático
        txtDescripcion.setWrapText(true);
        txtDescripcion.setPrefRowCount(4);

        // Estilizar botones
        btnRegistrar.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-padding: 10 20; -fx-font-size: 14px;");
        btnCancelar.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; "
                + "-fx-padding: 10 20; -fx-font-size: 14px;");
    }

    @FXML
    private void handleRegistrar() {
        try {
            // Validar campos
            if (txtMonto.getText().trim().isEmpty()) {
                mostrarError("Por favor ingrese el monto");
                return;
            }

            double monto;
            try {
                monto = Double.parseDouble(txtMonto.getText().trim());
            } catch (NumberFormatException ex) {
                mostrarError("El monto debe ser un número válido");
                return;
            }
            if (monto <= 0) {
                mostrarError("El monto debe ser mayor a cero");
                return;
            }

            if (txtDescripcion.getText().trim().isEmpty()) {
                mostrarError("Por favor ingrese una descripción");
                return;
            }

            if (cbCategoria.getValue() == null) {
                mostrarError("Seleccione una categoría");
                return;
            }

            // Crear y registrar egreso
            Egreso egreso = new Egreso();
            egreso.setDescripcion(txtDescripcion.getText().trim());
            egreso.setMonto(monto);
            egreso.setFecha(LocalDate.now());
            egreso.setCategoria(cbCategoria.getValue());

            DatabaseUtil.registrarEgreso(egreso);
            AuditoriaUtil.registrarAccion(
                    SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 0,
                    "Registro egreso",
                    egreso.getDescripcion()
            );
            EventBus.fireEvent(EventBus.EventType.EGRESO_REGISTRADO);
            limpiarFormulario();

        } catch (Exception e) {
            mostrarError("Error al registrar el egreso: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancelar() {
        cerrarVentana();
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    private void limpiarFormulario() {
        txtMonto.clear();
        txtDescripcion.clear();
        cbCategoria.getSelectionModel().selectFirst();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de Validación");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
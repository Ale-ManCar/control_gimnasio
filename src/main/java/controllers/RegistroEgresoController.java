package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.Egreso;
import util.DatabaseUtil;
import util.EventBus;
import util.AuditoriaUtil;
import util.SessionManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

public class RegistroEgresoController implements Initializable {

    @FXML private TextField txtMonto;
    @FXML private TextArea txtDescripcion;
    @FXML private ComboBox<String> cbCategoria;
    @FXML private Button btnRegistrar;
    @FXML private Button btnCancelar;
    @FXML private Button btnSubirPdf;
    @FXML private Label lblPdfSeleccionado;

    private static final Path DIRECTORIO_PDFS = Paths.get("/mnt/egresos_pdfs");
    private File archivoPdfSeleccionado;

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
        btnSubirPdf.setStyle(btnSubirPdf.getStyle() + "-fx-cursor: hand;");

        lblPdfSeleccionado.setText("Ningún archivo seleccionado");
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

            int egresoId = DatabaseUtil.registrarEgreso(egreso);

            if (egresoId <= 0) {
                mostrarError("No se pudo registrar el egreso");
                return;
            }

            egreso.setId(egresoId);

            if (archivoPdfSeleccionado != null) {
                guardarPdfParaEgreso(egreso);
            }

            AuditoriaUtil.registrarAccion(
                    SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 0,
                    "Registro egreso",
                    egreso.getDescripcion()
            );
            limpiarFormulario();

        } catch (Exception e) {
            mostrarError("Error al registrar el egreso: " + e.getMessage());
        }
    }

    @FXML
    private void handleSeleccionarPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Factura PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));
        File archivo = fileChooser.showOpenDialog(btnSubirPdf.getScene().getWindow());
        if (archivo != null) {
            if (!archivo.getName().toLowerCase().endsWith(".pdf")) {
                mostrarError("Solo se permiten archivos PDF");
                return;
            }
            archivoPdfSeleccionado = archivo;
            lblPdfSeleccionado.setText("Factura seleccionada: " + archivo.getName());
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
        archivoPdfSeleccionado = null;
        lblPdfSeleccionado.setText("Ningún archivo seleccionado");
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de Validación");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void guardarPdfParaEgreso(Egreso egreso) {
        try {
            Files.createDirectories(DIRECTORIO_PDFS);
            String nombreArchivo = String.format("egreso_%d_factura.pdf", egreso.getId());
            Path destino = DIRECTORIO_PDFS.resolve(nombreArchivo);
            Files.copy(archivoPdfSeleccionado.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
            egreso.setPdfPath(destino.toString());
            DatabaseUtil.actualizarRutaPdfEgreso(egreso.getId(), egreso.getPdfPath());
            EventBus.fireEvent(EventBus.EventType.EGRESO_REGISTRADO);
        } catch (IOException ex) {
            mostrarError("El egreso se registró pero no se pudo guardar la factura PDF: " + ex.getMessage());
        } catch (Exception ex) {
            mostrarError("No se pudo asociar la factura PDF: " + ex.getMessage());
        }
    }
}
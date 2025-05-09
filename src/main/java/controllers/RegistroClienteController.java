package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegistroClienteController {
    // Elementos FXML (vinculados al diseño)
    @FXML private TextField txtNombres;
    @FXML private TextField txtApellidos;
    @FXML private TextField txtTelefono;
    @FXML private DatePicker dpFechaInicio;
    @FXML private ComboBox<String> cbMembresia;
    @FXML private Button btnSiguiente;

    @FXML
    public void initialize() {
        // Configura el ComboBox de membresías
        cbMembresia.getItems().addAll("1 Mes", "3 Meses", "6 Meses", "1 Año");

        // Valida el teléfono (solo números)
        txtTelefono.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtTelefono.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
    }

    @FXML
    private void handleSiguiente() {
        // Lógica para avanzar al siguiente paso (lo implementaremos luego)
        System.out.println("Botón Siguiente clickeado");
    }
}
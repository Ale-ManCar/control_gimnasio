package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
    @FXML public Button btnGuardar;
    @FXML public Button btnCancelar;
    @FXML private Label lblErrorNombres;
    @FXML private Label lblErrorApellidos;
    @FXML private Label lblErrorTelefono;

    // Estilos para los botones
    private final String GUARDAR_BASE_STYLE = "-fx-background-color: linear-gradient(to right, #4A6CF7, #2E8BFF);"
            + "-fx-text-fill: white;"
            + "-fx-font-weight: bold;"
            + "-fx-font-size: 14px;"
            + "-fx-background-radius: 25;"
            + "-fx-padding: 10 30;";

    private final String CANCELAR_BASE_STYLE = "-fx-background-color: #6C757D;"
            + "-fx-text-fill: white;"
            + "-fx-font-weight: bold;"
            + "-fx-font-size: 14px;"
            + "-fx-background-radius: 25;"
            + "-fx-padding: 10 30;";

    private final String HOVER_EFFECT = "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 5, 0, 0, 1);";
    private final String ACTIVE_EFFECT = "-fx-effect: innershadow(three-pass-box, rgba(0,0,0,0.3), 3, 0, 0, 1);";

    private Cliente cliente;

    @FXML
    public void initialize() {
        // Configurar efectos para botón Guardar
        configurarBoton(btnGuardar, GUARDAR_BASE_STYLE);

        // Configurar efectos para botón Cancelar
        configurarBoton(btnCancelar, CANCELAR_BASE_STYLE);

        // Acción para botón Cancelar
        btnCancelar.setOnAction(e -> cerrarVentana());
    }

    private void configurarBoton(Button boton, String estiloBase) {
        boton.setStyle(estiloBase);

        boton.setOnMouseEntered(e ->
                boton.setStyle(estiloBase + HOVER_EFFECT));

        boton.setOnMouseExited(e ->
                boton.setStyle(estiloBase));

        boton.setOnMousePressed(e ->
                boton.setStyle(estiloBase + ACTIVE_EFFECT));

        boton.setOnMouseReleased(e ->
                boton.setStyle(estiloBase + HOVER_EFFECT));
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
        txtNombres.setText(cliente.getNombres());
        txtApellidos.setText(cliente.getApellidos());
        txtTelefono.setText(cliente.getTelefonoVisible());
        configurarValidaciones();
    }

    private void configurarValidaciones() {
        // Limpiar errores al editar
        txtNombres.textProperty().addListener((obs, oldVal, newVal) -> {
            lblErrorNombres.setText("");
            if (!newVal.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*")) {
                txtNombres.setText(oldVal);
            } else {
                // Mantener posición del cursor al convertir a mayúsculas
                int caretPosition = txtNombres.getCaretPosition();
                txtNombres.setText(newVal.toUpperCase());
                txtNombres.positionCaret(caretPosition);
            }
        });

        txtApellidos.textProperty().addListener((obs, oldVal, newVal) -> {
            lblErrorApellidos.setText("");
            if (!newVal.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*")) {
                txtApellidos.setText(oldVal);
            } else {
                // Mantener posición del cursor al convertir a mayúsculas
                int caretPosition = txtApellidos.getCaretPosition();
                txtApellidos.setText(newVal.toUpperCase());
                txtApellidos.positionCaret(caretPosition);
            }
        });

        txtTelefono.textProperty().addListener((obs, oldVal, newVal) -> {
            lblErrorTelefono.setText("");
            if (!newVal.matches("\\d*")) {
                txtTelefono.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (newVal.length() > 10) {
                txtTelefono.setText(oldVal);
            }
        });
    }

    // Asegúrate de que este método sea público y con anotación @FXML
    @FXML
    public void handleGuardar() {
        System.out.println("Botón Guardar presionado"); // Para depuración

        // Validar campos
        boolean valido = true;

        if (txtNombres.getText().trim().isEmpty()) {
            lblErrorNombres.setText("Campo obligatorio");
            valido = false;
        }

        if (txtApellidos.getText().trim().isEmpty()) {
            lblErrorApellidos.setText("Campo obligatorio");
            valido = false;
        }

        if (txtTelefono.getText().trim().isEmpty()) {
            lblErrorTelefono.setText("Campo obligatorio");
            valido = false;
        } else if (txtTelefono.getText().length() != 10) {
            lblErrorTelefono.setText("Debe tener 10 dígitos");
            valido = false;
        }

        if (!valido) return;

        String telefonoVisible = txtTelefono.getText().trim();
        String telefonoInterno = generarTelefonoInterno(telefonoVisible, cliente.getTelefono());

        actualizarClienteEnBD(
                txtNombres.getText().trim(),
                txtApellidos.getText().trim(),
                telefonoInterno,
                telefonoVisible
        );
    }

    private void actualizarClienteEnBD(String nombres, String apellidos, String telefonoInterno, String telefonoVisible) {
        String sql = "UPDATE clientes SET nombres = ?, apellidos = ?, telefono = ?, telefono_visible = ? WHERE telefono = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nombres);
            stmt.setString(2, apellidos);
            stmt.setString(3, telefonoInterno);
            stmt.setString(4, telefonoVisible);
            stmt.setString(5, cliente.getTelefono());

            int filasAfectadas = stmt.executeUpdate();
            System.out.println("Filas afectadas: " + filasAfectadas); // Para depuración

            if (filasAfectadas > 0) {
                cerrarVentana();
            } else {
                mostrarAlerta("Error", "No se pudo actualizar el cliente");
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "Error al actualizar en la base de datos: " + e.getMessage());
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);

        // Estilo para la alerta
        alert.getDialogPane().setStyle(
                "-fx-background-color: #ffffff;" +
                        "-fx-font-size: 14px;" +
                        "-fx-border-radius: 10px;" +
                        "-fx-background-radius: 10px;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);"
        );

        alert.showAndWait();
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        stage.close();
    }

    private String generarTelefonoInterno(String telefonoVisible, String telefonoActual) {
        String valor = telefonoVisible != null ? telefonoVisible.trim() : "";
        if (valor.equals("0000000000")) {
            if (telefonoActual != null && telefonoActual.startsWith("AUTO-")) {
                return telefonoActual;
            }
            return "AUTO-" + System.currentTimeMillis();
        }
        return valor;
    }
}


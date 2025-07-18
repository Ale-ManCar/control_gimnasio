package controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import util.DatabaseUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

public class RegistroClienteController {

    @FXML private TextField txtNombres, txtApellidos, txtTelefono, txtMontoPago;
    @FXML private DatePicker dpFechaInicio;
    @FXML private ComboBox<String> cbMembresia;
    @FXML private Button btnSiguiente;
    @FXML private Button btnIrARenovaciones;

    @FXML
    public void initialize() {
        if (cbMembresia != null) {
            cbMembresia.getItems().clear();
            cbMembresia.getItems().addAll("1 Mes", "3 Meses", "6 Meses", "1 Año");
            cbMembresia.setValue("1 Mes");
        }

        // CONVERSIÓN NOMBRE Y APELLIDOS EN MAYÚSCULAS
        txtNombres.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("[\\p{L} .'-]*")) {
                txtNombres.setText(oldVal);
            } else {
                txtNombres.setText(newVal.toUpperCase());
            }
        });

        txtApellidos.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("[\\p{L} .'-]*")) {
                txtApellidos.setText(oldVal);
            } else {
                txtApellidos.setText(newVal.toUpperCase());
            }
        });

        // TELÉFONO VALIDADO A 10 DÍGITOS
        txtTelefono.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*") || newVal.length() > 10) {
                txtTelefono.setText(oldVal);
            }
        });

        txtMontoPago.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d{0,2})?")) {
                txtMontoPago.setText(oldVal);
            }
        });
    }

    @FXML
    private void handleSiguiente() {
        if (cbMembresia.getValue() == null || dpFechaInicio.getValue() == null) {
            mostrarAlerta("Error", "Debe completar todos los campos");
            return;
        }

        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false); // Transacción

            // Insertar cliente
            String sqlCliente = "INSERT INTO clientes (nombres, apellidos, telefono, tipoMembresia, fechaInicio, fecha_vencimiento, monto_pago) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmtCliente = conn.prepareStatement(sqlCliente, PreparedStatement.RETURN_GENERATED_KEYS);

            stmtCliente.setString(1, validarCampo(txtNombres.getText(), "Nombres"));
            stmtCliente.setString(2, validarCampo(txtApellidos.getText(), "Apellidos"));
            stmtCliente.setString(3, validarCampo(txtTelefono.getText(), "Teléfono"));
            stmtCliente.setString(4, cbMembresia.getValue());
            stmtCliente.setString(5, dpFechaInicio.getValue().toString());

            LocalDate fechaVencimiento = calcularVencimiento(dpFechaInicio.getValue(), cbMembresia.getValue());
            stmtCliente.setString(6, fechaVencimiento.toString());

            String montoStr = validarCampo(txtMontoPago.getText(), "Monto de Pago");
            double monto = Double.parseDouble(montoStr);
            if (monto <= 0) {
                mostrarAlerta("Error", "El monto de pago debe ser mayor a 0");
                return;
            }
            stmtCliente.setDouble(7, monto);

            stmtCliente.executeUpdate();

            int clienteId = -1;
            var rs = stmtCliente.getGeneratedKeys();
            if (rs.next()) {
                clienteId = rs.getInt(1);
            }

            if (clienteId != -1) {
                String sqlPago = "INSERT INTO pagos (cliente_id, fecha_pago, fecha_vencimiento, monto) VALUES (?, ?, ?, ?)";
                PreparedStatement stmtPago = conn.prepareStatement(sqlPago);
                stmtPago.setInt(1, clienteId);
                stmtPago.setString(2, dpFechaInicio.getValue().toString());
                stmtPago.setString(3, fechaVencimiento.toString());
                stmtPago.setDouble(4, monto);
                stmtPago.executeUpdate();
            }

            conn.commit();

            // ✅ Mostrar UNA sola alerta de éxito con mensaje de redirección
            mostrarAlertaExito();

            // ✅ Programar retorno al dashboard después de 5 segundos
            programarRetornoAlDashboard();

        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudo registrar el cliente/pago: " + e.getMessage());
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "El monto de pago no es válido");
        }
    }

    private void programarRetornoAlDashboard() {
        new Thread(() -> {
            try {
                // Esperar 5 segundos
                Thread.sleep(5000);

                Platform.runLater(() -> {
                    try {
                        // Cargar el dashboard
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
                        Parent root = loader.load();

                        // Obtener la ventana actual
                        Stage stage = (Stage) btnSiguiente.getScene().getWindow();

                        // Crear nueva escena
                        Scene scene = new Scene(root);
                        stage.setScene(scene);
                        stage.setTitle("Panel de Control");
                        stage.show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        mostrarAlerta("Error", "No se pudo cargar el panel de control");
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @FXML
    private void handleIrARenovaciones() {
        try {
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
        txtMontoPago.clear();
        dpFechaInicio.setValue(null);
        cbMembresia.setValue("1 Mes");
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);

            // Configurar estilo
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle(
                    "-fx-background-color: #ffffff;" +
                            "-fx-font-size: 14px;" +
                            "-fx-border-radius: 10px;" +
                            "-fx-background-radius: 10px;"
            );

            alert.showAndWait();
        });
    }

    private void mostrarAlertaExito() {
        Platform.runLater(() -> {
            // Crear una alerta personalizada
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("¡Éxito!");
            alert.setHeaderText(null);
            alert.setContentText(
                    "✅ Cliente registrado correctamente\n\n" +
                            "Será redirigido al panel de control en 5 segundos..."
            );

            // Personalizar el diseño
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle(
                    "-fx-background-color: #e8f5e9;" +  // Verde claro de fondo
                            "-fx-font-size: 16px;" +
                            "-fx-border-radius: 15px;" +
                            "-fx-background-radius: 15px;" +
                            "-fx-effect: dropshadow(three-pass-box, rgba(46, 204, 113, 0.5), 15, 0, 0, 3);" +
                            "-fx-padding: 20px;"
            );

            // Estilo para el texto
            Label contentLabel = (Label) dialogPane.lookup(".content.label");
            contentLabel.setStyle(
                    "-fx-text-fill: #27ae60;" +  // Verde oscuro
                            "-fx-font-weight: bold;" +
                            "-fx-font-size: 13px;"
            );

            // Agregar ícono de éxito
            ImageView successIcon = new ImageView(new Image(
                    getClass().getResource("/images/success.png").toExternalForm()
            ));
            successIcon.setFitWidth(60);
            successIcon.setFitHeight(60);
            alert.setGraphic(successIcon);

            // Personalizar botón OK
            Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
            okButton.setStyle(
                    "-fx-background-color: #2ecc71;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-font-size: 14px;" +
                            "-fx-background-radius: 10px;" +
                            "-fx-padding: 8px 16px;"
            );

            // Animación de entrada
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), dialogPane);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            // Mostrar alerta
            alert.show();
            fadeIn.play();
        });
    }
}
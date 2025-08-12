package controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import util.DatabaseUtil;
import util.WhatsAppService;
import models.Cliente;
import models.Coach;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

public class RegistroClienteController {

    @FXML private TextField txtNombres, txtApellidos, txtTelefono, txtMontoPago;
    @FXML private DatePicker dpFechaInicio;
    @FXML private ComboBox<String> cbMembresia;
    @FXML private ComboBox<String> cbArea;
    @FXML private ComboBox<Coach> cbCoach;
    @FXML private Button btnSiguiente;
    @FXML private Button btnVolverAlDashboard;

    private final ObservableList<Coach> coaches = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (cbMembresia != null) {
            cbMembresia.getItems().clear();
            cbMembresia.getItems().addAll("1 Mes", "3 Meses", "6 Meses", "1 Año");
            cbMembresia.setValue("1 Mes");
        }

        if (cbArea != null) {
            cbArea.getItems().addAll("Maquinas", "Bailoterapia", "Crossfit");
            cbArea.valueProperty().addListener((obs, old, val) -> {
                if (val != null) cargarCoachesPorArea(val);
            });
        }

        if (cbCoach != null) {
            cbCoach.setItems(coaches);
            cbCoach.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Coach item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getNombreCompleto());
                }
            });
            cbCoach.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Coach item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getNombreCompleto());
                }
            });
        }

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
        if (cbMembresia.getValue() == null || dpFechaInicio.getValue() == null ||
                cbArea.getValue() == null || cbCoach.getValue() == null) {
            mostrarAlerta("Error", "Debe completar todos los campos");
            return;
        }

        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);

            String sqlCliente = "INSERT INTO clientes (nombres, apellidos, telefono, tipoMembresia, fechaInicio, fecha_vencimiento, monto_pago, area, coach_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            stmtCliente.setString(8, cbArea.getValue());
            stmtCliente.setInt(9, cbCoach.getValue().getId());

            stmtCliente.executeUpdate();

            int clienteId = -1;
            var rs = stmtCliente.getGeneratedKeys();
            if (rs.next()) {
                clienteId = rs.getInt(1);
            }

            if (clienteId != -1) {
                String sqlPago = "INSERT INTO pagos (cliente_id, fecha_pago, fecha_vencimiento, tipo_membresia, monto) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement stmtPago = conn.prepareStatement(sqlPago);
                stmtPago.setInt(1, clienteId);
                stmtPago.setString(2, dpFechaInicio.getValue().toString());
                stmtPago.setString(3, fechaVencimiento.toString());
                stmtPago.setString(4, cbMembresia.getValue());
                stmtPago.setDouble(5, monto);
                stmtPago.executeUpdate();
            }

            conn.commit();

            // ✅ Mostrar alerta de éxito
            mostrarAlertaExito();

            // ✅ Enviar alerta de registro
            Cliente nuevoCliente = new Cliente(
                    txtNombres.getText().trim(),
                    txtApellidos.getText().trim(),
                    txtTelefono.getText().trim(),
                    cbMembresia.getValue(),
                    fechaVencimiento
            );
            new Thread(() -> WhatsAppService.enviarAlertaRegistro(nuevoCliente)).start();

            // Programar retorno al dashboard
            programarRetornoAlDashboard();

        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudo registrar el cliente/pago: " + e.getMessage());
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "El monto de pago no es válido");
        } catch (Exception e) {
            mostrarAlerta("Error", "Error inesperado: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cargarCoachesPorArea(String area) {
        coaches.clear();
        String sql = "SELECT id, nombres, apellidos, area, telefono, foto_path FROM coaches WHERE area = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, area);
            var rs = stmt.executeQuery();
            while (rs.next()) {
                coaches.add(new Coach(
                        rs.getInt("id"),
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("area"),
                        rs.getString("telefono"),
                        rs.getString("foto_path")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void programarRetornoAlDashboard() {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
                        Parent root = loader.load();
                        Stage stage = (Stage) btnSiguiente.getScene().getWindow();
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
    private void handleVolverAlDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) btnVolverAlDashboard.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Panel de Control");
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo cargar el panel de control");
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

    private void mostrarAlerta(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
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
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("¡Éxito!");
            alert.setHeaderText(null);
            alert.setContentText(
                    "✅ Cliente registrado correctamente\n\n" +
                            "Será redirigido al panel de control en 5 segundos..."
            );

            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle(
                    "-fx-background-color: #e8f5e9;" +
                            "-fx-font-size: 16px;" +
                            "-fx-border-radius: 15px;" +
                            "-fx-background-radius: 15px;" +
                            "-fx-effect: dropshadow(three-pass-box, rgba(46, 204, 113, 0.5), 15, 0, 0, 3);" +
                            "-fx-padding: 20px;"
            );

            Label contentLabel = (Label) dialogPane.lookup(".content.label");
            contentLabel.setStyle(
                    "-fx-text-fill: #27ae60;" +
                            "-fx-font-weight: bold;" +
                            "-fx-font-size: 13px;"
            );

            ImageView successIcon = new ImageView(new Image(
                    getClass().getResource("/images/success.png").toExternalForm()
            ));
            successIcon.setFitWidth(60);
            successIcon.setFitHeight(60);
            alert.setGraphic(successIcon);

            Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
            okButton.setStyle(
                    "-fx-background-color: #2ecc71;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-font-size: 14px;" +
                            "-fx-background-radius: 10px;" +
                            "-fx-padding: 8px 16px;"
            );

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), dialogPane);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            alert.show();
            fadeIn.play();
        });
    }
}
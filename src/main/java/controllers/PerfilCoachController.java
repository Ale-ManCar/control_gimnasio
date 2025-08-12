package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import models.Cliente;
import models.Coach;
import util.DatabaseUtil;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class PerfilCoachController {

    @FXML private ImageView imgFoto;
    @FXML private Label lblNombre;
    @FXML private Label lblArea;
    @FXML private Label lblTelefono;
    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, String> colCliente;
    @FXML private TableColumn<Cliente, String> colTelefono;

    @FXML
    public void initialize() {
        configurarColumnas();
        configurarEstilosTabla();
        configurarFilas();
    }

    private void configurarColumnas() {
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));

        colCliente.setCellFactory(column -> new TableCell<Cliente, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: black; " +
                            "-fx-background-color: transparent;");
                }
            }
        });

        colTelefono.setCellFactory(column -> new TableCell<Cliente, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: black; " +
                            "-fx-background-color: transparent;");
                }
            }
        });
    }

    private void configurarEstilosTabla() {
        tablaClientes.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-background-color: #ffffff;" +
                        "-fx-border-radius: 10px;" +
                        "-fx-background-radius: 10px;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);"
        );
    }

    private void configurarFilas() {
        tablaClientes.setRowFactory(tv -> {
            TableRow<Cliente> row = new TableRow<Cliente>() {
                @Override
                protected void updateItem(Cliente cliente, boolean empty) {
                    super.updateItem(cliente, empty);
                    if (empty || cliente == null) {
                        setStyle("");
                        setTooltip(null);
                    } else {
                        Tooltip tooltip = new Tooltip(cliente.getTooltipText());
                        tooltip.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                        setTooltip(tooltip);

                        if (isSelected()) {
                            setStyle("-fx-background-color: #e6f2ff; " +
                                    "-fx-border-color: #e0e0e0; " +
                                    "-fx-border-width: 0 0 1px 0;");
                        } else if (isHover()) {
                            setStyle("-fx-background-color: #e6f2ff; " +
                                    "-fx-border-color: #e0e0e0; " +
                                    "-fx-border-width: 0 0 1px 0;");
                        } else {
                            setStyle("-fx-background-color: #ffffff; " +
                                    "-fx-border-color: #e0e0e0; " +
                                    "-fx-border-width: 0 0 1px 0;");
                        }
                    }
                }
            };

            row.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    row.setStyle("-fx-background-color: #e6f2ff; " +
                            "-fx-border-color: #e0e0e0; " +
                            "-fx-border-width: 0 0 1px 0;");
                } else {
                    if (row.isHover()) {
                        row.setStyle("-fx-background-color: #e6f2ff; " +
                                "-fx-border-color: #e0e0e0; " +
                                "-fx-border-width: 0 0 1px 0;");
                    } else {
                        row.setStyle("-fx-background-color: #ffffff; " +
                                "-fx-border-color: #e0e0e0; " +
                                "-fx-border-width: 0 0 1px 0;");
                    }
                }
            });

            row.hoverProperty().addListener((obs, oldVal, isHovering) -> {
                if (isHovering && !row.isSelected()) {
                    row.setStyle("-fx-background-color: #e6f2ff; " +
                            "-fx-border-color: #e0e0e0; " +
                            "-fx-border-width: 0 0 1px 0;");
                } else if (!row.isSelected()) {
                    row.setStyle("-fx-background-color: #ffffff; " +
                            "-fx-border-color: #e0e0e0; " +
                            "-fx-border-width: 0 0 1px 0;");
                }
            });

            return row;
        });
    }

    public void setCoach(Coach coach) {
        lblNombre.setText(coach.getNombreCompleto());
        lblArea.setText("Área: " + coach.getArea());
        lblTelefono.setText("Tel: " + coach.getTelefono());
        if (coach.getFotoPath() != null) {
            imgFoto.setImage(new Image(new File(coach.getFotoPath()).toURI().toString()));
        }
        cargarClientes(coach.getId());
    }

    private void cargarClientes(int coachId) {
        tablaClientes.getItems().clear();
        String sql = "SELECT nombres, apellidos, telefono, fecha_vencimiento FROM clientes WHERE coach_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, coachId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tablaClientes.getItems().add(new Cliente(
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("telefono"),
                        LocalDate.parse(rs.getString("fecha_vencimiento"))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCerrar() {
        Stage stage = (Stage) tablaClientes.getScene().getWindow();
        stage.close();
    }
}
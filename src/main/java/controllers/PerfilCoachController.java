package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
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
    @FXML private TextField txtBuscar;
    @FXML private Button btnLimpiar;

    private final ObservableList<Cliente> clientesOriginales = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarColumnas();
        configurarEstilosTabla();
        configurarFilas();
        configurarBusqueda();
        ajustarColumnas();
        ocultarScrollBars();
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

    private void ajustarColumnas() {
        tablaClientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colCliente.prefWidthProperty().bind(tablaClientes.widthProperty().multiply(0.6));
        colTelefono.prefWidthProperty().bind(tablaClientes.widthProperty().multiply(0.4));
    }

    private void ocultarScrollBars() {
        tablaClientes.skinProperty().addListener((obs, oldSkin, newSkin) ->
                tablaClientes.lookupAll(".scroll-bar").forEach(node -> {
                    node.setVisible(false);
                    node.setManaged(false);
                })
        );
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

    private void configurarBusqueda() {
        txtBuscar.setPromptText("Buscar cliente...");
        txtBuscar.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-padding: 8px 15px;" +
                        "-fx-background-radius: 20px;" +
                        "-fx-border-radius: 20px;" +
                        "-fx-border-color: #ced4da;" +
                        "-fx-background-color: #ffffff;"
        );

        btnLimpiar.setStyle(
                "-fx-background-color: #6C757D;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8px 15px;" +
                        "-fx-background-radius: 20px;" +
                        "-fx-border-radius: 20px;" +
                        "-fx-cursor: hand;"
        );
        btnLimpiar.setOnMouseEntered(e ->
                btnLimpiar.setStyle(
                        "-fx-background-color: #5a6268;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 8px 15px;" +
                                "-fx-background-radius: 20px;" +
                                "-fx-border-radius: 20px;" +
                                "-fx-cursor: hand;"
                )
        );
        btnLimpiar.setOnMouseExited(e ->
                btnLimpiar.setStyle(
                        "-fx-background-color: #6C757D;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 8px 15px;" +
                                "-fx-background-radius: 20px;" +
                                "-fx-border-radius: 20px;" +
                                "-fx-cursor: hand;"
                )
        );

        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            filtrarClientes();
        });
    }

    private void filtrarClientes() {
        String filtro = txtBuscar.getText().trim().toLowerCase();

        if (filtro.isEmpty()) {
            tablaClientes.setItems(clientesOriginales);
        } else {
            ObservableList<Cliente> filtrados = FXCollections.observableArrayList();
            for (Cliente cliente : clientesOriginales) {
                if (cliente.getNombreCompleto().toLowerCase().contains(filtro) ||
                        cliente.getTelefono().contains(filtro)) {
                    filtrados.add(cliente);
                }
            }
            tablaClientes.setItems(filtrados);
        }

        tablaClientes.refresh();
    }

    @FXML
    private void limpiarFiltro() {
        Cliente seleccionado = tablaClientes.getSelectionModel().getSelectedItem();
        int scrollPosition = tablaClientes.getSelectionModel().getSelectedIndex();

        txtBuscar.clear();
        tablaClientes.setItems(clientesOriginales);

        if (seleccionado != null) {
            tablaClientes.getSelectionModel().select(seleccionado);
        }
        tablaClientes.scrollTo(scrollPosition);

        tablaClientes.refresh();
    }

    public void setCoach(Coach coach) {
        lblNombre.setText(coach.getNombreCompleto());
        lblArea.setText("Área: " + coach.getArea());
        lblTelefono.setText("Tel: " + coach.getTelefono());
        if (coach.getFotoPath() != null) {
            imgFoto.setImage(new Image(new File(coach.getFotoPath()).toURI().toString()));
        }
        cargarClientes(coach.getId());
        clientesOriginales.setAll(tablaClientes.getItems());
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
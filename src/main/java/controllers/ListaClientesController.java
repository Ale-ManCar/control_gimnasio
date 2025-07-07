package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;
import models.Cliente;
import util.DatabaseUtil;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class ListaClientesController implements Initializable {

    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, String> colNombres;
    @FXML private TableColumn<Cliente, String> colApellidos;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colVencimiento;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarColumnas();
        cargarClientes();
        ajustarAnchoColumnas();
        centrarContenidoCeldas();
        centrarEncabezados();

        tablaClientes.setRowFactory(tv -> {
            TableRow<Cliente> row = new TableRow<>();
            row.setOnMouseEntered(event -> {
                if (!row.isEmpty()) {
                    Tooltip tooltip = new Tooltip(row.getItem().getTooltipText());
                    tooltip.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                    Tooltip.install(row, tooltip);
                }
            });
            row.setOnMouseExited(event -> {
                if (!row.isEmpty()) {
                    Tooltip.uninstall(row, null);
                }
            });
            return row;
        });
    }

    private void configurarColumnas() {
        colNombres.setCellValueFactory(new PropertyValueFactory<>("nombres"));
        colApellidos.setCellValueFactory(new PropertyValueFactory<>("apellidos"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colVencimiento.setCellValueFactory(new PropertyValueFactory<>("fecha_vencimiento"));
    }

    private void cargarClientes() {
        String sql = "SELECT nombres, apellidos, telefono, tipoMembresia, fecha_vencimiento FROM clientes WHERE activo = 1";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Cliente cliente = new Cliente(
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("telefono"),
                        rs.getString("tipoMembresia"),
                        LocalDate.parse(rs.getString("fecha_vencimiento"))
                );
                tablaClientes.getItems().add(cliente);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void ajustarAnchoColumnas() {
        tablaClientes.widthProperty().addListener((obs, oldVal, newVal) -> {
            double anchoTotal = tablaClientes.getWidth();
            double anchoColumna = anchoTotal / tablaClientes.getColumns().size();

            for (TableColumn<Cliente, ?> columna : tablaClientes.getColumns()) {
                columna.setPrefWidth(anchoColumna);
            }
        });
    }

    private void centrarContenidoCeldas() {
        centrarColumna(colNombres);
        centrarColumna(colApellidos);
        centrarColumna(colTelefono);
        centrarColumna(colVencimiento);
    }

    private <T> void centrarColumna(TableColumn<Cliente, T> columna) {
        columna.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Cliente, T> call(TableColumn<Cliente, T> param) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(T item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            setText(item.toString());
                            setStyle("-fx-alignment: CENTER;");
                        }
                    }
                };
            }
        });
    }

    private void centrarEncabezados() {
        tablaClientes.setStyle("-fx-font-size: 14px;");
        colNombres.setStyle("-fx-alignment: CENTER;");
        colApellidos.setStyle("-fx-alignment: CENTER;");
        colTelefono.setStyle("-fx-alignment: CENTER;");
        colVencimiento.setStyle("-fx-alignment: CENTER;");
    }

    @FXML
    private void handleVolver() {
        Stage stage = (Stage) tablaClientes.getScene().getWindow();
        stage.close();
    }
}
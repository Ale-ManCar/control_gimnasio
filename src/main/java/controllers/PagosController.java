package controllers;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import models.Pago;
import util.DatabaseUtil;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.function.Function;
import javafx.util.Callback;

public class PagosController implements Initializable {
    @FXML private TableView<Pago> tablaPagos;
    @FXML private TableColumn<Pago, Integer> colId;
    @FXML private TableColumn<Pago, String> colCliente;
    @FXML private TableColumn<Pago, String> colFecha;
    @FXML private TableColumn<Pago, String> colMembresia;
    @FXML private TableColumn<Pago, Double> colMonto;
    @FXML private TableColumn<Pago, String> colEstado;
    @FXML private TableColumn<Pago, Void> colAcciones;
    @FXML private Label lblMensaje;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configurarTabla();
        cargarPagos();
    }

    @FXML
    private void handleRefrescar() {
        cargarPagos();
    }

    @FXML
    private void handleCerrar() {
        tablaPagos.getScene().getWindow().hide();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new SimpleObjectPropertyFactory<>(Pago::getId));
        colCliente.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getClienteNombre()));
        colFecha.setCellValueFactory(cell -> {
            if (cell.getValue().getFechaPago() == null) {
                return new SimpleStringProperty("-");
            }
            return new SimpleStringProperty(cell.getValue().getFechaPago().format(formatter));
        });
        colMembresia.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTipoMembresia()));
        colMonto.setCellValueFactory(new SimpleObjectPropertyFactory<>(Pago::getMonto));
        colEstado.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEstado()));
        configurarColumnaAcciones();
    }

    private void configurarColumnaAcciones() {
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button boton = new Button("Anular");

            {
                boton.setStyle("-fx-background-color: #e63946; -fx-text-fill: white; -fx-font-weight: bold;");
                boton.setOnAction(e -> {
                    Pago pago = getTableView().getItems().get(getIndex());
                    confirmarAnulacion(pago);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(boton);
                    boton.setDisable(!"ACTIVO".equalsIgnoreCase(getTableRow().getItem().getEstado()));
                }
            }
        });
    }

    private void confirmarAnulacion(Pago pago) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setHeaderText(null);
        alerta.setContentText("¿Desea anular el pago " + pago.getId() + "?");
        alerta.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    PagoController.anularPago(pago.getId());
                    cargarPagos();
                } catch (SQLException e) {
                    mostrarError("No se pudo anular el pago: " + e.getMessage());
                }
            }
        });
    }

    private void cargarPagos() {
        try {
            ObservableList<Pago> pagos = DatabaseUtil.listarPagosActivos();
            tablaPagos.setItems(pagos);
            lblMensaje.setText(pagos.isEmpty() ? "No hay pagos registrados" : "");
        } catch (SQLException e) {
            mostrarError("No se pudieron cargar los pagos");
        }
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle("-fx-text-fill: #ff6b6b;");
    }

    private static class SimpleObjectPropertyFactory<T, R> implements Callback<TableColumn.CellDataFeatures<T, R>, ObservableValue<R>> {
        private final Function<T, R> extractor;

        SimpleObjectPropertyFactory(Function<T, R> extractor) {
            this.extractor = extractor;
        }

        @Override
        public ObservableValue<R> call(TableColumn.CellDataFeatures<T, R> param) {
            return new SimpleObjectProperty<>(extractor.apply(param.getValue()));
        }
    }
}
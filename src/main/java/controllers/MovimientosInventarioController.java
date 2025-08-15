package controllers;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import models.MovimientoInventario;
import models.Producto;
import util.DatabaseUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MovimientosInventarioController {

    @FXML private Label lblProducto;
    @FXML private TableView<MovimientoInventario> tablaMovimientos;
    @FXML private TableColumn<MovimientoInventario, LocalDateTime> colFecha;
    @FXML private TableColumn<MovimientoInventario, String> colTipo;
    @FXML private TableColumn<MovimientoInventario, Integer> colCantidad;
    @FXML private TableColumn<MovimientoInventario, Integer> colSaldo;
    @FXML private TableColumn<MovimientoInventario, String> colUsuario;
    @FXML private TableColumn<MovimientoInventario, String> colMotivo;

    private Producto producto;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private void initialize() {
        colFecha.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getFecha()));
        colTipo.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getTipo()));
        colCantidad.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getCantidad()).asObject());
        colSaldo.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getSaldo()).asObject());
        colUsuario.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getUsuario()));
        colMotivo.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getMotivo()));

        colFecha.setCellFactory(new Callback<>() {
            @Override
            public TableCell<MovimientoInventario, LocalDateTime> call(TableColumn<MovimientoInventario, LocalDateTime> param) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(LocalDateTime item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.format(formatter));
                    }
                };
            }
        });
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
        lblProducto.setText("Historial de Movimientos - " + producto.getNombre());
        cargarMovimientos();
    }

    private void cargarMovimientos() {
        try {
            ObservableList<MovimientoInventario> movimientos = DatabaseUtil.getMovimientosPorProducto(producto.getId());
            tablaMovimientos.setItems(movimientos);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
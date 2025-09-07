package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableRow;
import javafx.scene.control.cell.PropertyValueFactory;
import models.Equipo;
import models.ProveedorPrecio;
import util.DatabaseUtil;
import util.ReporteUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ComparadorPreciosController {

    @FXML private ComboBox<String> cboEquipos;
    @FXML private TableView<ProveedorPrecio> tblPrecios;
    @FXML private TableColumn<ProveedorPrecio, String> colProveedor;
    @FXML private TableColumn<ProveedorPrecio, Double> colPrecio;

    private final ObservableList<ProveedorPrecio> datos = FXCollections.observableArrayList();
    private final Map<String, Integer> equiposMap = new HashMap<>();

    @FXML
    public void initialize() {
        colProveedor.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        tblPrecios.setItems(datos);
        try {
            ObservableList<Equipo> equipos = DatabaseUtil.getEquipos();
            for (Equipo equipo : equipos) {
                cboEquipos.getItems().add(equipo.getNombre());
                equiposMap.put(equipo.getNombre(), equipo.getId());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        cboEquipos.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> cargarDatos());
    }

    private void cargarDatos() {
        String nombre = cboEquipos.getValue();
        datos.clear();
        if (nombre == null) {
            return;
        }
        Integer equipoId = equiposMap.get(nombre);
        if (equipoId == null) {
            return;
        }
        try {
            Map<String, Double> mapa = DatabaseUtil.getPreciosPorProveedor(equipoId);
            for (Map.Entry<String, Double> e : mapa.entrySet()) {
                datos.add(new ProveedorPrecio(e.getKey(), e.getValue()));
            }

            double mejorPrecio = DatabaseUtil.obtenerProveedoresConPrecioMasBajo(equipoId)
                    .stream()
                    .mapToDouble(ProveedorPrecio::getPrecio)
                    .findFirst()
                    .orElse(Double.MAX_VALUE);

            tblPrecios.setRowFactory(tv -> new TableRow<>() {
                @Override
                protected void updateItem(ProveedorPrecio item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setStyle("");
                    } else if (item.getPrecio() == mejorPrecio) {
                        setStyle("-fx-background-color: #b9f6ca;");
                    } else {
                        setStyle("");
                    }
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void exportarReporte() {
        String nombre = cboEquipos.getValue();
        if (nombre == null || datos.isEmpty()) {
            return;
        }
        ReporteUtil.generarReporteComparadorPrecios(nombre, new ArrayList<>(datos));
    }
}
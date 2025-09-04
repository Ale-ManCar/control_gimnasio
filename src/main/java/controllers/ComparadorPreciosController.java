package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import models.Equipo;
import util.DatabaseUtil;

import java.sql.SQLException;
import java.util.Map;

public class ComparadorPreciosController {

    public static class PrecioProveedor {
        private final String proveedor;
        private final double precio;

        public PrecioProveedor(String proveedor, double precio) {
            this.proveedor = proveedor;
            this.precio = precio;
        }

        public String getProveedor() {
            return proveedor;
        }

        public double getPrecio() {
            return precio;
        }
    }

    @FXML private ComboBox<Equipo> cbEquipos;
    @FXML private TableView<PrecioProveedor> tablaPrecios;
    @FXML private TableColumn<PrecioProveedor, String> colProveedor;
    @FXML private TableColumn<PrecioProveedor, Double> colPrecio;

    private ObservableList<PrecioProveedor> datos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colProveedor.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        tablaPrecios.setItems(datos);
        try {
            cbEquipos.setItems(DatabaseUtil.getEquipos());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        cbEquipos.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> cargarDatos());
    }

    private void cargarDatos() {
        Equipo equipo = cbEquipos.getValue();
        datos.clear();
        if (equipo == null) return;
        try {
            Map<String, Double> mapa = DatabaseUtil.getPreciosPorProveedor(equipo.getId());
            for (Map.Entry<String, Double> e : mapa.entrySet()) {
                datos.add(new PrecioProveedor(e.getKey(), e.getValue()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
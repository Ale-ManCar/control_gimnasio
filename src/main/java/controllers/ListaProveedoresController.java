package controllers;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import models.Proveedor;
import util.DatabaseUtil;

import java.sql.SQLException;

public class ListaProveedoresController {

    @FXML private TableView<Proveedor> tablaProveedores;
    @FXML private TableColumn<Proveedor, String> colNombre;
    @FXML private TableColumn<Proveedor, String> colContacto;
    @FXML private TableColumn<Proveedor, String> colTelefono;

    @FXML
    public void initialize() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colContacto.setCellValueFactory(new PropertyValueFactory<>("contacto"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        cargarProveedores();
    }

    private void cargarProveedores() {
        try {
            ObservableList<Proveedor> proveedores = DatabaseUtil.getProveedores();
            tablaProveedores.setItems(proveedores);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void refrescarTabla() {
        cargarProveedores();
    }
}
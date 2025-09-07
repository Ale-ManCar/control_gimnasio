package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import models.Compra;
import util.DatabaseUtil;

import java.sql.*;
import java.time.LocalDate;

/**
 * Controlador para la vista de aprobación de órdenes de compra generadas
 * automáticamente. Permite al administrador aprobar o rechazar órdenes
 * pendientes.
 */
public class OrdenesCompraController {

    @FXML
    private TableView<Compra> tablaOrdenes;

    private final ObservableList<Compra> ordenes = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        cargarOrdenesPendientes();
        if (tablaOrdenes != null) {
            tablaOrdenes.setItems(ordenes);
        }
    }

    private void cargarOrdenesPendientes() {
        ordenes.clear();
        String sql = "SELECT id, proveedor_id, fecha, total, ruta_pdf, estado FROM compras WHERE estado = 'PENDIENTE'";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Compra c = new Compra();
                c.setId(rs.getInt("id"));
                c.setProveedorId(rs.getInt("proveedor_id"));
                c.setFecha(LocalDate.parse(rs.getString("fecha")));
                c.setTotal(rs.getDouble("total"));
                c.setRutaPdf(rs.getString("ruta_pdf"));
                c.setEstado(rs.getString("estado"));
                ordenes.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void aprobarSeleccionada() {
        Compra c = tablaOrdenes.getSelectionModel().getSelectedItem();
        if (c != null) {
            actualizarEstado(c.getId(), "APROBADA");
            cargarOrdenesPendientes();
        }
    }

    @FXML
    private void rechazarSeleccionada() {
        Compra c = tablaOrdenes.getSelectionModel().getSelectedItem();
        if (c != null) {
            actualizarEstado(c.getId(), "RECHAZADA");
            cargarOrdenesPendientes();
        }
    }

    private void actualizarEstado(int id, String estado) {
        try {
            DatabaseUtil.executeUpdate("UPDATE compras SET estado = ? WHERE id = ?", estado, id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
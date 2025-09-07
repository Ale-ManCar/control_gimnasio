package util;

import models.Producto;

import java.sql.*;

/**
 * Servicio para generar órdenes de compra automáticas cuando un producto
 * alcanza un nivel de stock bajo. Las órdenes se almacenan en las tablas
 * {@code compras} y {@code compras_detalle} con estado "PENDIENTE" hasta
 * que un administrador las apruebe o rechace.
 */
public class OrdenCompraService {

    /**
     * Genera una orden de compra pendiente para el producto indicado si no
     * existe ya una orden pendiente.
     *
     * @param producto Producto que requiere reposición
     */
    public static void generarOrdenCompraAutomatica(Producto producto) {
        if (producto == null || producto.getId() == 0) {
            return;
        }

        String checkSql = "SELECT 1 FROM compras_detalle cd " +
                "JOIN compras c ON cd.compra_id = c.id " +
                "WHERE cd.equipo_id = ? AND c.estado = 'PENDIENTE'";
        String insertCompra = "INSERT INTO compras (proveedor_id, fecha, total, estado) VALUES (1, date('now'), ?, 'PENDIENTE')";
        String insertDetalle = "INSERT INTO compras_detalle (compra_id, equipo_id, cantidad, precio) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setInt(1, producto.getId());
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    // Ya existe una orden pendiente para este producto
                    return;
                }
            }

            conn.setAutoCommit(false);
            try (PreparedStatement compraStmt = conn.prepareStatement(insertCompra, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement detalleStmt = conn.prepareStatement(insertDetalle)) {

                int cantidad = producto.getUnidadesPorPaca() > 0 ? producto.getUnidadesPorPaca() : 1;
                double total = cantidad * producto.getPrecioCompra();

                compraStmt.setDouble(1, total);
                compraStmt.executeUpdate();

                int compraId = -1;
                try (ResultSet rs = compraStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        compraId = rs.getInt(1);
                    }
                }

                detalleStmt.setInt(1, compraId);
                detalleStmt.setInt(2, producto.getId());
                detalleStmt.setInt(3, cantidad);
                detalleStmt.setDouble(4, producto.getPrecioCompra());
                detalleStmt.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error generando orden de compra: " + e.getMessage());
        }
    }
}
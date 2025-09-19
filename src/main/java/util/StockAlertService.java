package util;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Servicio para consultar productos con stock bajo y generar alertas internas.
 */
public class StockAlertService {

    /**
     * Obtiene mensajes de alerta para todos los productos cuyo stock es menor o igual al umbral.
     */
    public static List<String> obtenerAlertasStock() {
        List<String> alertas = new ArrayList<>();
        String sql = "SELECT * FROM productos WHERE stock <= umbral";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                alertas.add(String.format("Producto '%s' con stock %d (umbral %d)",
                        rs.getString("nombre"), rs.getInt("stock"), rs.getInt("umbral")));

            }
        } catch (SQLException e) {
            System.err.println("Error al consultar inventario: " + e.getMessage());
        }
        return alertas;
    }

    /**
     * Consulta el inventario y envía las alertas generadas al consumidor indicado.
     */
    public static void enviarAlertas(Consumer<List<String>> consumer) {
        List<String> alertas = obtenerAlertasStock();
        if (!alertas.isEmpty()) {
            consumer.accept(alertas);
        }
    }
}
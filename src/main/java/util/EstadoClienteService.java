package util;

import models.Cliente;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EstadoClienteService {
    public static void iniciarActualizacionDiaria() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            actualizarEstadosClientes();
        }, 0, 1, TimeUnit.DAYS);
    }

    private static void actualizarEstadosClientes() {
        // Modificado: 15 días de gracia después del vencimiento
        String sql = "UPDATE clientes SET activo = 0 WHERE date(fecha_vencimiento, '+15 days') < date('now')";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error actualizando estados: " + e.getMessage());
        }
    }

    public static List<Cliente> obtenerClientesPorVencerEn(int dias) {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT nombres, apellidos, telefono, tipoMembresia, fecha_vencimiento " +
                "FROM clientes WHERE fecha_vencimiento = ? AND activo = 1 " +
                "AND telefono NOT IN (SELECT telefono_cliente FROM alertas_enviadas WHERE fecha_envio = CURRENT_DATE AND tipo_alerta = 'Vencimiento')";
        LocalDate objetivo = LocalDate.now().plusDays(dias);
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, objetivo.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Cliente c = new Cliente(
                            rs.getString("nombres"),
                            rs.getString("apellidos"),
                            rs.getString("telefono"),
                            LocalDate.parse(rs.getString("fecha_vencimiento"))
                    );
                    c.setTipoMembresia(rs.getString("tipoMembresia"));
                    clientes.add(c);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo clientes por vencer en " + dias + " días: " + e.getMessage());
        }
        return clientes;
    }
}
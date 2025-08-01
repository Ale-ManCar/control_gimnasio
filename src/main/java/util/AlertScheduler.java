package util;

import models.Cliente;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AlertScheduler implements Runnable {

    @Override
    public void run() {
        System.out.println("Ejecutando búsqueda de clientes para alertas...");
        List<Cliente> clientes = obtenerClientesParaAlertar();

        if (clientes.isEmpty()) {
            System.out.println("No hay clientes para notificar hoy.");
            return;
        }

        System.out.println("Clientes a notificar: " + clientes.size());

        List<Cliente> clientesValidos = new ArrayList<>();
        for (Cliente cliente : clientes) {
            if (clienteExisteYActivo(cliente.getTelefono())) {
                clientesValidos.add(cliente);
            } else {
                System.out.println("Cliente no existe o está inactivo: " + cliente.getTelefono());
            }
        }

        if (clientesValidos.isEmpty()) {
            System.out.println("Todos los clientes están inactivos o ya fueron notificados.");
            return;
        }

        boolean driverUsado = false;

        for (Cliente cliente : clientesValidos) {
            try {
                WhatsAppService.enviarAlerta(cliente);
                driverUsado = true;
                Thread.sleep(3000 + (int) (Math.random() * 3000)); // entre 3 y 6 segundos
            } catch (Exception e) {
                System.err.println("Error al procesar cliente " + cliente.getTelefono() + ": " + e.getMessage());
            }
        }

        if (driverUsado) {
            WhatsAppService.cerrarDriver();
        }
    }

    private boolean clienteExisteYActivo(String telefono) {
        String sql = "SELECT activo FROM clientes WHERE telefono = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, telefono);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean("activo");
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar cliente: " + telefono);
            return false;
        }
    }

    private List<Cliente> obtenerClientesParaAlertar() {
        List<Cliente> clientes = new ArrayList<>();
        LocalDate hoy = LocalDate.now();

        LocalDate fecha7Dias = hoy.plusDays(7);
        LocalDate fecha3Dias = hoy.plusDays(3);
        LocalDate fecha1Dia = hoy.plusDays(1);
        LocalDate fechaHoy = hoy;

        String sql = "SELECT nombres, apellidos, telefono, tipoMembresia, fecha_vencimiento " +
                "FROM clientes " +
                "WHERE fecha_vencimiento IN (?, ?, ?, ?) " +
                "AND activo = true " +
                "AND telefono NOT IN (" +
                "    SELECT telefono_cliente FROM alertas_enviadas " +
                "    WHERE fecha_envio = CURRENT_DATE" +
                ")";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fecha7Dias.toString());
            stmt.setString(2, fecha3Dias.toString());
            stmt.setString(3, fecha1Dia.toString());
            stmt.setString(4, fechaHoy.toString());

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Cliente cliente = new Cliente(
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("telefono"),
                        LocalDate.parse(rs.getString("fecha_vencimiento"))
                );
                cliente.setTipoMembresia(rs.getString("tipoMembresia"));
                clientes.add(cliente);
            }

        } catch (SQLException e) {
            System.err.println("Error al consultar clientes para alertas: " + e.getMessage());
            e.printStackTrace();
        }

        return clientes;
    }

    public static void main(String[] args) {
        new AlertScheduler().run();
    }
}
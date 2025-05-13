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

        for (Cliente cliente : clientes) {
            try {
                // Enviar alerta por WhatsApp
                WhatsAppService.enviarAlerta(cliente);

                // Registrar envío en base de datos
                guardarRegistroEnvio(cliente);

                System.out.println("Alerta enviada a: " + cliente.getNombres() +
                        " - Teléfono: " + cliente.getTelefono());

                // Pequeña pausa entre envíos para evitar bloqueos
                Thread.sleep(3000);

            } catch (Exception e) {
                System.err.println("Error al procesar cliente " + cliente.getTelefono() +
                        ": " + e.getMessage());
            }
        }
    }

    private List<Cliente> obtenerClientesParaAlertar() {
        List<Cliente> clientes = new ArrayList<>();
        LocalDate hoy = LocalDate.now();

        // Rangos de fechas para alertas (7, 3 y 1 día antes del vencimiento)
        LocalDate fecha7Dias = hoy.plusDays(7);
        LocalDate fecha3Dias = hoy.plusDays(3);
        LocalDate fecha1Dia = hoy.plusDays(1);

        String sql = "SELECT nombres, telefono, tipoMembresia, fechaVencimiento " +
                "FROM clientes " +
                "WHERE fechaVencimiento IN (?, ?, ?) " +
                "AND activo = true " +
                "AND telefono NOT IN (" +
                "    SELECT telefono_cliente FROM alertas_enviadas " +
                "    WHERE fecha_envio = CURRENT_DATE" +
                ")"; // Evitar notificaciones duplicadas

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fecha7Dias.toString());
            stmt.setString(2, fecha3Dias.toString());
            stmt.setString(3, fecha1Dia.toString());

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Cliente cliente = new Cliente(
                        rs.getString("nombres"),
                        rs.getString("telefono"),
                        rs.getString("tipoMembresia"),
                        LocalDate.parse(rs.getString("fechaVencimiento"))
                );
                clientes.add(cliente);
            }

        } catch (SQLException e) {
            System.err.println("Error al consultar clientes para alertas: " + e.getMessage());
            e.printStackTrace();
        }

        return clientes;
    }

    private void guardarRegistroEnvio(Cliente cliente) throws SQLException {
        String sql = "INSERT INTO alertas_enviadas " +
                "(telefono_cliente, fecha_envio, tipo_alerta) " +
                "VALUES (?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cliente.getTelefono());
            stmt.setDate(2, Date.valueOf(LocalDate.now()));

            // Determinar tipo de alerta según días restantes
            long diasRestantes = LocalDate.now().until(cliente.getFechaVencimientoDate()).getDays();
            String tipoAlerta = "Alerta a " + diasRestantes + " días";

            stmt.setString(3, tipoAlerta);
            stmt.executeUpdate();
        }
    }

    // Método para ejecución manual (opcional)
    public static void main(String[] args) {
        new AlertScheduler().run();
    }
}
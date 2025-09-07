package util;

import models.Cliente;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AlertScheduler implements Runnable {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * Inicia las tareas programadas para enviar avisos de vencimiento y recordatorios de pago.
     */
    public static void iniciar() {
        scheduler.scheduleAtFixedRate(() -> new AlertScheduler().run(), 0, 1, TimeUnit.DAYS);
        scheduler.scheduleAtFixedRate(AlertScheduler::procesarPagosPendientes, 0, 1, TimeUnit.DAYS);
    }

    @Override
    public void run() {
        System.out.println("Ejecutando búsqueda de clientes para alertas de vencimiento...");
        List<Cliente> clientes = obtenerClientesParaAlertar();

        if (!clientes.isEmpty()) {
            System.out.println("Clientes a notificar: " + clientes.size());

            List<Cliente> clientesValidos = clientes.stream()
                    .filter(c -> clienteExisteYActivo(c.getTelefono()))
                    .collect(Collectors.toList());

            if (!clientesValidos.isEmpty()) {
                boolean driverUsado = false;
                for (Cliente cliente : clientesValidos) {
                    try {
                        WhatsAppService.enviarAvisoVencimiento(cliente);
                        driverUsado = true;
                        Thread.sleep(3000 + (int) (Math.random() * 3000)); // entre 3 y 6 segundos
                    } catch (Exception e) {
                        System.err.println("Error al procesar cliente " + cliente.getTelefono() + ": " + e.getMessage());
                    }
                }
                if (driverUsado) {
                    WhatsAppService.cerrarDriver();
                }
            } else {
                System.out.println("Todos los clientes están inactivos o ya fueron notificados.");
            }
        } else {
            System.out.println("No hay clientes para notificar hoy.");
        }

        // Revisar inventario y notificar al administrador
        for (String alerta : obtenerAlertasInventario()) {
            notificarAdmin(alerta);
        }
    }

    /**
     * Procesa y envía recordatorios de pago a los clientes con membresías vencidas.
     */
    private static void procesarPagosPendientes() {
        System.out.println("Ejecutando búsqueda de clientes con pagos pendientes...");
        List<Cliente> clientes = obtenerClientesConPagosPendientes();

        if (!clientes.isEmpty()) {
            List<Cliente> clientesValidos = clientes.stream()
                    .filter(c -> clienteExisteYActivo(c.getTelefono()))
                    .collect(Collectors.toList());

            if (!clientesValidos.isEmpty()) {
                boolean driverUsado = false;
                for (Cliente cliente : clientesValidos) {
                    try {
                        WhatsAppService.enviarRecordatorioPago(cliente);
                        driverUsado = true;
                        Thread.sleep(3000 + (int) (Math.random() * 3000));
                    } catch (Exception e) {
                        System.err.println("Error al procesar cliente " + cliente.getTelefono() + ": " + e.getMessage());
                    }
                }
                if (driverUsado) {
                    WhatsAppService.cerrarDriver();
                }
            } else {
                System.out.println("Todos los clientes con pagos pendientes están inactivos o ya fueron notificados.");
            }
        } else {
            System.out.println("No hay clientes con pagos pendientes hoy.");
        }
    }

    private static boolean clienteExisteYActivo(String telefono) {
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
        int[] dias = {7, 3, 1, 0};
        for (int d : dias) {
            clientes.addAll(EstadoClienteService.obtenerClientesPorVencerEn(d));
        }
        return clientes;
    }

    /**
     * Obtiene clientes cuya membresía ya venció pero aún tienen la cuenta activa,
     * lo que indica que tienen un pago pendiente.
     */
    private static List<Cliente> obtenerClientesConPagosPendientes() {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT nombres, apellidos, telefono, tipoMembresia, fecha_vencimiento " +
                "FROM clientes WHERE fecha_vencimiento < date('now') AND activo = 1 " +
                "AND telefono NOT IN (SELECT telefono_cliente FROM alertas_enviadas WHERE fecha_envio = CURRENT_DATE)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
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
        } catch (SQLException e) {
            System.err.println("Error obteniendo clientes con pagos pendientes: " + e.getMessage());
        }
        return clientes;
    }

    private static List<String> obtenerAlertasInventario() {
        List<String> alertas = new ArrayList<>();
        String sqlEquipos = "SELECT nombre, stock, umbral FROM equipos WHERE stock <= umbral";
        String sqlProductos = "SELECT nombre, stock, umbral FROM productos WHERE stock <= umbral";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(sqlEquipos)) {
                while (rs.next()) {
                    alertas.add("Equipo '" + rs.getString("nombre") + "' con stock " +
                            rs.getInt("stock") + " (umbral " + rs.getInt("umbral") + ")");
                }
            }
            try (ResultSet rs = stmt.executeQuery(sqlProductos)) {
                while (rs.next()) {
                    alertas.add("Producto '" + rs.getString("nombre") + "' con stock " +
                            rs.getInt("stock") + " (umbral " + rs.getInt("umbral") + ")");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al consultar inventario: " + e.getMessage());
        }
        return alertas;
    }
    private static void notificarAdmin(String mensaje) {
        // En un futuro esto podría integrarse con un servicio de mensajería
        System.out.println("Notificación al administrador: " + mensaje);
    }

    public static List<String> obtenerAlertasPendientes() {
        List<String> alertas = new ArrayList<>();
        for (Cliente c : EstadoClienteService.obtenerClientesPorVencerEn(7)) {
            alertas.add("Membresía de " + c.getNombreCompleto() + " vence en 7 días");
        }
        alertas.addAll(obtenerAlertasInventario());
        return alertas;
    }

    public static void main(String[] args) {
        new AlertScheduler().run();
    }
}
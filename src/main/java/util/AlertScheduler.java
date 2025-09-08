package util;

import models.Cliente;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.text.ParseException;
import org.quartz.CronExpression;
import java.nio.file.*;
import java.io.IOException;

public class AlertScheduler implements Runnable {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Inicia la tareas programadas del sistema.
     */
    public static void iniciar() {
        // Avisos de vencimiento por defecto a las 9 AM
        programarAvisosVencimiento("0 0 9 * * ?");

        // Programar respaldo según configuración
        String frecuencia = obtenerFrecuenciaRespaldo();
        String cron = "0 0 2 * * ?"; // diario a las 2 AM
        if ("semanal".equalsIgnoreCase(frecuencia)) {
            cron = "0 0 2 ? * MON"; // lunes a las 2 AM
        }
        programarBackup(cron, frecuencia);
    }

    /**
     * Lee la configuración de frecuencia de respaldo desde CONFIGURACION.txt.
     */
    private static String obtenerFrecuenciaRespaldo() {
        Path config = Paths.get("CONFIGURACION.txt");
        if (Files.exists(config)) {
            try {
                return Files.lines(config)
                        .filter(l -> l.toLowerCase().contains("frecuencia de respaldo"))
                        .map(l -> l.substring(l.indexOf(':') + 1).trim())
                        .findFirst()
                        .orElse("diaria");
            } catch (IOException e) {
                System.err.println("No se pudo leer configuración de respaldo: " + e.getMessage());
            }
        }
        return "diaria";
    }

    private static void scheduleCron(String cronExpression, Runnable task) {
        try {
            CronExpression cron = new CronExpression(cronExpression);
            scheduleNext(cron, task);
        } catch (ParseException e) {
            System.err.println("Expresión cron inválida: " + cronExpression);
        }
    }

    private static void scheduleNext(CronExpression cron, Runnable task) {
        Date next = cron.getNextValidTimeAfter(new Date());
        if (next == null) {
            return;
        }
        long delay = next.getTime() - System.currentTimeMillis();
        scheduler.schedule(() -> {
            try {
                task.run();
            } finally {
                scheduleNext(cron, task);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    public static void programarBackup(String cronExpression) {
        scheduleCron(cronExpression, BackupUtil::crearBackup);
    }

    public static void programarBackup(String cronExpression, String tipo) {
        scheduleCron(cronExpression, () -> BackupUtil.crearBackup(tipo));
    }

    public static void programarAvisosVencimiento(String cronExpression) {
        scheduleCron(cronExpression, () -> new AlertScheduler().run());
    }

    public static void programarOrdenesCompra(String cronExpression) {
        scheduleCron(cronExpression, () -> StockAlertService.obtenerAlertasStock());
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
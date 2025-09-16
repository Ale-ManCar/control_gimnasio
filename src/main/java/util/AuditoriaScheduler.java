package util;

import javafx.collections.ObservableList;
import models.Role;
import models.User;
import org.quartz.CronExpression;
import util.ReporteUtil;
import util.UserService;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler para la generación automática de resúmenes de auditoría.
 */
public final class AuditoriaScheduler {

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "auditoria-scheduler");
        thread.setDaemon(true);
        return thread;
    });

    private static final String CRON_SEMANAL = "0 0 23 ? * SAT"; // sábados 23:00
    private static final String CRON_MENSUAL = "0 15 23 L * ?";  // último día del mes 23:15
    private static final String CRON_ANUAL = "0 30 23 31 12 ?"; // 31 de diciembre 23:30

    private AuditoriaScheduler() {
    }

    public static void iniciar() {
        programar(CRON_SEMANAL, AuditoriaScheduler::generarResumenesSemanales);
        programar(CRON_MENSUAL, AuditoriaScheduler::generarResumenesMensuales);
        programar(CRON_ANUAL, AuditoriaScheduler::generarResumenesAnuales);
    }

    public static Path generarResumenDiario(int usuarioId, LocalDateTime inicio, LocalDateTime fin) {
        return generarResumenDiario(usuarioId, inicio, fin, true);
    }

    public static Path generarResumenDiario(int usuarioId, LocalDateTime inicio, LocalDateTime fin, boolean mostrar) {
        return ReporteUtil.generarResumenTurno(usuarioId, inicio, fin, mostrar);
    }

    private static void generarResumenesSemanales() {
        LocalDate fin = LocalDate.now();
        LocalDate inicio = fin.minusDays(6);
        procesarRecepcionistas(usuario ->
                ReporteUtil.generarResumenSemanal(usuario.getId(), inicio.atStartOfDay(), fin.atTime(LocalTime.of(23, 59, 59)), false));
    }

    private static void generarResumenesMensuales() {
        LocalDate fin = LocalDate.now();
        LocalDate inicio = fin.withDayOfMonth(1);
        procesarRecepcionistas(usuario ->
                ReporteUtil.generarResumenMensual(usuario.getId(), inicio.atStartOfDay(), fin.atTime(LocalTime.of(23, 59, 59)), false));
    }

    private static void generarResumenesAnuales() {
        LocalDate fin = LocalDate.now();
        LocalDate inicio = fin.withDayOfYear(1);
        procesarRecepcionistas(usuario ->
                ReporteUtil.generarResumenAnual(usuario.getId(), inicio.atStartOfDay(), fin.atTime(LocalTime.of(23, 59, 59)), false));
    }

    private static void procesarRecepcionistas(java.util.function.Consumer<User> accion) {
        try {
            ObservableList<User> usuarios = UserService.listarUsuariosPorRol(Role.RECEPCIONISTA);
            for (User usuario : usuarios) {
                try {
                    accion.accept(usuario);
                } catch (Exception e) {
                    System.err.println("Error generando resumen para " + usuario.getUsername() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("No se pudieron obtener los recepcionistas: " + e.getMessage());
        }
    }

    private static void programar(String cronExpresion, Runnable tarea) {
        try {
            CronExpression cron = new CronExpression(cronExpresion);
            programarSiguiente(cron, tarea);
        } catch (Exception e) {
            System.err.println("Expresión cron inválida para auditoría: " + cronExpresion);
        }
    }

    private static void programarSiguiente(CronExpression cron, Runnable tarea) {
        Date siguiente = cron.getNextValidTimeAfter(new Date());
        if (siguiente == null) {
            return;
        }
        long delay = siguiente.getTime() - System.currentTimeMillis();
        EXECUTOR.schedule(() -> {
            try {
                tarea.run();
            } catch (Exception e) {
                System.err.println("Error ejecutando tarea de auditoría: " + e.getMessage());
                e.printStackTrace();
            } finally {
                programarSiguiente(cron, tarea);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
}
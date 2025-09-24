package util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class MantenimientoEquipoScheduler {
    private static ScheduledExecutorService scheduler;

    private MantenimientoEquipoScheduler() {
    }

    public static synchronized void iniciar() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "mantenimiento-equipos-scheduler");
                thread.setDaemon(true);
                return thread;
            }
        });
        scheduler.scheduleAtFixedRate(DatabaseUtil::actualizarMantenimientosProgramados, 0, 1, TimeUnit.DAYS);
    }

    public static synchronized void detener() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}
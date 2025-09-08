package util;

import java.time.Duration;
import java.time.LocalDateTime;

public class SessionTimeoutService implements Runnable {

    private static final long INACTIVITY_THRESHOLD_MINUTES = 30;

    @Override
    public void run() {
        if (SessionManager.getCurrentUser() == null) {
            return;
        }
        LocalDateTime last = SessionManager.getLastActivity();
        if (last == null) {
            SessionManager.actualizarActividad();
            return;
        }
        long inactive = Duration.between(last, LocalDateTime.now()).toMinutes();
        if (inactive >= INACTIVITY_THRESHOLD_MINUTES) {
            AuditoriaUtil.registrarAccion(SessionManager.getCurrentUser().getId(),
                    "Timeout", "Sesión cerrada por inactividad");
            SessionManager.clear();
        }
    }
}
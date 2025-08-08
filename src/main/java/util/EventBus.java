package util;

import java.util.HashSet;
import java.util.Set;

public class EventBus {
    private static final Set<Runnable> listeners = new HashSet<>();

    public static void registerListener(Runnable listener) {
        listeners.add(listener);
    }

    public static void unregisterListener(Runnable listener) {
        listeners.remove(listener);
    }

    public static void fireVentaRealizadaEvent() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    public static void fireEgresoRegistradoEvent() {
        fireVentaRealizadaEvent();
    }
}
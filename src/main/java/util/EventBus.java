package util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class EventBus {
    public enum EventType {
        VENTA_REALIZADA,
        EGRESO_REGISTRADO,
        DATOS_ACTUALIZADOS
    }

    // Mapa para almacenar los listeners por tipo de evento
    private static final Map<EventType, Set<Consumer<EventType>>> listenersMap = new HashMap<>();

    static {
        // Inicializar todos los tipos de evento
        for (EventType type : EventType.values()) {
            listenersMap.put(type, new HashSet<>());
        }
    }

    public static void registerListener(EventType eventType, Consumer<EventType> listener) {
        listenersMap.get(eventType).add(listener);
    }

    public static void unregisterListener(EventType eventType, Consumer<EventType> listener) {
        listenersMap.get(eventType).remove(listener);
    }

    public static void fireEvent(EventType eventType) {
        for (Consumer<EventType> listener : listenersMap.get(eventType)) {
            listener.accept(eventType);
        }
    }

    // Métodos legacy para compatibilidad
    public static void registerListener(Runnable listener) {
        registerListener(EventType.DATOS_ACTUALIZADOS, eventType -> listener.run());
    }

    public static void fireVentaRealizadaEvent() {
        fireEvent(EventType.VENTA_REALIZADA);
    }

    public static void fireEgresoRegistradoEvent() {
        fireEvent(EventType.EGRESO_REGISTRADO);
    }
}
package models;

/**
 * Tipos de resúmenes generados para los recepcionistas.
 */
public enum ResumenTipo {
    TODOS("Todos", null, null),
    DIARIO("Diario", "RESUMEN_TURNO", "Diario"),
    SEMANAL("Semanal", "RESUMEN_SEMANAL", "Semanal"),
    MENSUAL("Mensual", "RESUMEN_MENSUAL", "Mensual"),
    ANUAL("Anual", "RESUMEN_ANUAL", "Anual");

    private final String displayName;
    private final String accion;
    private final String folderName;

    ResumenTipo(String displayName, String accion, String folderName) {
        this.displayName = displayName;
        this.accion = accion;
        this.folderName = folderName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAccion() {
        return accion;
    }

    public String getFolderName() {
        return folderName;
    }

    public boolean matches(String action) {
        if (accion == null) {
            return true;
        }
        return accion.equalsIgnoreCase(action);
    }

    public static ResumenTipo fromAccion(String accion) {
        if (accion == null) {
            return null;
        }
        for (ResumenTipo tipo : values()) {
            if (tipo.accion != null && tipo.accion.equalsIgnoreCase(accion)) {
                return tipo;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
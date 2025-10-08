package util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Centraliza las rutas de archivos utilizadas por la aplicación para que los datos se
 * almacenen en una ubicación consistente y con permisos de escritura.
 */
public final class AppPaths {

    private static final String ENV_OVERRIDE = "CONTROL_GIMNASIO_HOME";
    private static final String LOGS_FOLDER_NAME = "logs";
    private static final String LOG_FILE_NAME = "app.log";

    private AppPaths() {
    }

    /**
     * Devuelve el directorio base donde la aplicación almacena datos persistentes.
     *
     * <p>Prioriza la variable de entorno {@code CONTROL_GIMNASIO_HOME}. Si no está definida,
     * se utiliza el directorio de trabajo actual para mantener compatibilidad con
     * instalaciones existentes.</p>
     */
    public static Path getAppHome() {
        String override = System.getenv(ENV_OVERRIDE);
        if (override != null && !override.isBlank()) {
            return Paths.get(override).toAbsolutePath();
        }

        // Utiliza el directorio de trabajo actual para mantener la base de datos en la
        // misma ubicación que las instalaciones anteriores.
        return Paths.get("").toAbsolutePath();
    }

    /**
     * Ruta del directorio que contiene la base de datos SQLite.
     */
    public static Path getDatabaseDirectory() {
        return getAppHome().resolve("database");
    }

    /**
     * Ruta completa del archivo de base de datos SQLite.
     */
    public static Path getDatabaseFile() {
        return getDatabaseDirectory().resolve("gimnasio.db");
    }

    /**
     * Directorio donde se almacenan los respaldos.
     */
    public static Path getBackupDirectory() {
        return getAppHome().resolve("backups");
    }

    /**
     * Directorio donde se almacenan los registros de la aplicación.
     */
    public static Path getLogsDirectory() {
        return getAppHome().resolve(LOGS_FOLDER_NAME);
    }

    /**
     * Ruta completa del archivo de bitácora principal.
     */
    public static Path getLogFile() {
        return getLogsDirectory().resolve(LOG_FILE_NAME);
    }

    /**
     * Garantiza que los directorios base existan antes de acceder a ellos.
     */
    public static void ensureAppDirectories() throws Exception {
        createIfMissing(getAppHome());
        createIfMissing(getDatabaseDirectory());
        createIfMissing(getBackupDirectory());
        createIfMissing(getLogsDirectory());
    }

    private static void createIfMissing(Path path) throws Exception {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }
}

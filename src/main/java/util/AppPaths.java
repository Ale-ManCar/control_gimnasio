package util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Centraliza las rutas de archivos utilizadas por la aplicación para que los datos se
 * almacenen en una ubicación consistente y con permisos de escritura.
 */
public final class AppPaths {

    private static final String ENV_OVERRIDE = "CONTROL_GIMNASIO_HOME";
    private static final String APP_FOLDER_NAME = "ControlGimnasio";

    private AppPaths() {
    }

    /**
     * Devuelve el directorio base donde la aplicación almacena datos persistentes.
     *
     * <p>Prioriza la variable de entorno {@code CONTROL_GIMNASIO_HOME}. Si no está definida,
     * utiliza una ruta específica para el sistema operativo:
     * <ul>
     *     <li>Windows: {@code %LOCALAPPDATA%\ControlGimnasio}</li>
     *     <li>Otros sistemas: {@code ~/.control_gimnasio}</li>
     * </ul>
     */
    public static Path getAppHome() {
        String override = System.getenv(ENV_OVERRIDE);
        if (override != null && !override.isBlank()) {
            return Paths.get(override).toAbsolutePath();
        }

        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isBlank()) {
                return Paths.get(localAppData, APP_FOLDER_NAME).toAbsolutePath();
            }
        }

        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".control_gimnasio").toAbsolutePath();
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
     * Garantiza que los directorios base existan antes de acceder a ellos.
     */
    public static void ensureAppDirectories() throws Exception {
        createIfMissing(getAppHome());
        createIfMissing(getDatabaseDirectory());
        createIfMissing(getBackupDirectory());
    }

    private static void createIfMissing(Path path) throws Exception {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }
}

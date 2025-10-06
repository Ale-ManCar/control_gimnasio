package util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Gestiona las rutas de archivos utilizados por la aplicación tanto en modo desarrollo
 * como cuando se ejecuta desde un instalador generado con jpackage.
 */
public final class AppPaths {
    private static final Path DATA_DIR = computeDataDir();
    private static final Path DATABASE_PATH = DATA_DIR.resolve(Paths.get("database", "gimnasio.db"));
    private static final Path BACKUP_DIR = DATA_DIR.resolve("backups");
    private static final Path CONFIG_FILE = DATA_DIR.resolve("CONFIGURACION.txt");
    private static final Path BUNDLED_RESOURCE_ROOT = locateBundledResources();

    private static volatile boolean initialized = false;

    private AppPaths() {
    }

    /**
     * Asegura que los directorios de datos existen y que los recursos iniciales
     * (base de datos y archivo de configuración) están disponibles en una
     * ubicación escribible para el usuario actual.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        try {
            Files.createDirectories(DATABASE_PATH.getParent());
            Files.createDirectories(BACKUP_DIR);
            copyIfMissing(BUNDLED_RESOURCE_ROOT.resolve(Paths.get("database", "gimnasio.db")), DATABASE_PATH);
            copyIfMissing(BUNDLED_RESOURCE_ROOT.resolve("CONFIGURACION.txt"), CONFIG_FILE);
            initialized = true;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudieron preparar los archivos necesarios: " + e.getMessage(), e);
        }
    }

    /**
     * Devuelve la ruta al directorio de datos escribible.
     */
    public static Path getDataDir() {
        ensureInitialized();
        return DATA_DIR;
    }

    /**
     * Devuelve la ruta absoluta a la base de datos SQLite en uso.
     */
    public static Path getDatabasePath() {
        ensureInitialized();
        return DATABASE_PATH;
    }

    /**
     * Devuelve la ruta al directorio de respaldos.
     */
    public static Path getBackupsDir() {
        ensureInitialized();
        return BACKUP_DIR;
    }

    /**
     * Devuelve la ruta al archivo de configuración del sistema.
     */
    public static Path getConfigFile() {
        ensureInitialized();
        return CONFIG_FILE;
    }

    private static void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }

    private static Path computeDataDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isBlank()) {
                return Paths.get(localAppData, "ControlGimnasio");
            }
        }
        return Paths.get(System.getProperty("user.home"), ".control_gimnasio");
    }

    private static void copyIfMissing(Path source, Path target) throws IOException {
        if (Files.exists(target)) {
            return;
        }
        if (!Files.exists(source)) {
            throw new IOException("No se encontró el recurso empaquetado: " + source);
        }
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private static Path locateBundledResources() {
        Path base = resolveCodeSourceBase();
        Path located = findResourceRoot(base);
        if (located != null) {
            return located;
        }
        Path workingDir = Paths.get("").toAbsolutePath();
        located = findResourceRoot(workingDir);
        if (located != null) {
            return located;
        }
        throw new IllegalStateException("No se encontró la carpeta 'database' ni el archivo CONFIGURACION.txt en el instalador.");
    }

    private static Path resolveCodeSourceBase() {
        try {
            Path codeSource = Paths.get(AppPaths.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(codeSource)) {
                return codeSource.getParent();
            }
            return codeSource;
        } catch (URISyntaxException e) {
            return Paths.get("").toAbsolutePath();
        }
    }

    private static Path findResourceRoot(Path start) {
        Path current = start;
        for (int i = 0; i < 6 && current != null; i++) {
            if (Files.isDirectory(current.resolve("database")) && Files.exists(current.resolve("CONFIGURACION.txt"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }
}

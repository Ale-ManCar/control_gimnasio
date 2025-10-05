package util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utilidad para crear respaldos de la base de datos.
 */
public class BackupUtil implements Runnable {
    private static final Path DB_PATH = AppPaths.getDatabasePath();
    private static final Path BACKUP_DIR = AppPaths.getBackupsDir();;

    @Override
    public void run() {
        crearBackup();
    }

    /**
     * Crea un respaldo diario por defecto.
     */
    public static void crearBackup() {
        crearBackup("diario");
    }

    /**
     * Copia la base de datos a la carpeta de respaldos indicada con sello de fecha.
     *
     * @param tipo puede ser "diario" o "semanal"
     */
    public static void crearBackup(String tipo) {
        try {
            Path dir = BACKUP_DIR.resolve(tipo);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            LocalDateTime ahora = LocalDateTime.now();
            String nombreArchivo;
            if ("diario".equalsIgnoreCase(tipo)) {
                String fecha = LocalDate.now()
                        .format(DateTimeFormatter.BASIC_ISO_DATE);
                nombreArchivo = "gimnasio_" + fecha + ".db";
            } else {
                String timestamp = ahora.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                nombreArchivo = "gimnasio_" + timestamp + ".db";
            }
            Path destino = dir.resolve(nombreArchivo);
            Files.copy(DB_PATH, destino, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Backup " + tipo + " creado: " + destino);
        } catch (IOException e) {
            System.err.println("Error creando backup " + tipo + ": " + e.getMessage());
        }
    }

    /**
     * Restaura la base de datos desde un archivo de respaldo.
     *
     * @param archivo ruta del respaldo a restaurar
     */
    public static void restaurarBackup(Path archivo) {
        try {
            Files.copy(archivo, DB_PATH, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Base de datos restaurada desde: " + archivo);
        } catch (IOException e) {
            System.err.println("Error restaurando backup: " + e.getMessage());
        }
    }
}
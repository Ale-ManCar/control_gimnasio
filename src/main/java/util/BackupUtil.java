package util;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utilidad para crear respaldos de la base de datos.
 */
public class BackupUtil implements Runnable {
    private static final Path DB_PATH = Paths.get("database", "gimnasio.db");
    private static final Path BACKUP_DIR = Paths.get("backups");

    @Override
    public void run() {
        crearBackup();
    }

    /**
     * Copia la base de datos a la carpeta de respaldos con sello de fecha.
     */
    public static void crearBackup() {
        try {
            if (!Files.exists(BACKUP_DIR)) {
                Files.createDirectories(BACKUP_DIR);
            }
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path destino = BACKUP_DIR.resolve("gimnasio_" + timestamp + ".db");
            Files.copy(DB_PATH, destino, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Backup creado: " + destino);
        } catch (IOException e) {
            System.err.println("Error creando backup: " + e.getMessage());
        }
    }
}
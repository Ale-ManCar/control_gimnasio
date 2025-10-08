package util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Registro simple de eventos y errores de la aplicación en un archivo local.
 */
public final class AppLogger {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AppLogger() {
    }

    public static void logInfo(String message) {
        write("INFO", message, null);
    }

    public static void logError(String message, Throwable error) {
        write("ERROR", message, error);
    }

    private static void write(String level, String message, Throwable error) {
        try {
            AppPaths.ensureAppDirectories();
            Path logFile = AppPaths.getLogFile();
            StringBuilder entry = new StringBuilder()
                    .append('[')
                    .append(TIMESTAMP.format(LocalDateTime.now()))
                    .append("] ")
                    .append(level)
                    .append(':')
                    .append(' ')
                    .append(message);
            if (error != null) {
                entry.append(System.lineSeparator())
                        .append(stackTrace(error));
            }
            entry.append(System.lineSeparator());

            Files.writeString(logFile, entry.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ioException) {
            System.err.println("No se pudo escribir en el log: " + ioException.getMessage());
        } catch (Exception e) {
            System.err.println("Error asegurando directorios de log: " + e.getMessage());
        }
    }

    private static String stackTrace(Throwable error) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            error.printStackTrace(pw);
        }
        return sw.toString();
    }
}

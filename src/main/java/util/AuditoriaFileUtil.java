package util;

import models.ResumenTipo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Utilidades para manejar la estructura de archivos de auditoría.
 */
public final class AuditoriaFileUtil {

    private static final Path BASE_DIR = Paths.get("Auditoria");
    private static final DateTimeFormatter DIA_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter MES_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter MES_NOMBRE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);

    private AuditoriaFileUtil() {
    }

    public static Path getBaseDirectory() {
        return BASE_DIR.toAbsolutePath();
    }

    public static Path ensureResumenPath(String username, int usuarioId, ResumenTipo tipo,
                                         LocalDateTime inicio, LocalDateTime fin) throws IOException {
        if (tipo == null || tipo == ResumenTipo.TODOS) {
            throw new IllegalArgumentException("Tipo de resumen no válido para la ruta");
        }
        String nombreUsuario = username == null || username.isBlank()
                ? "usuario_" + usuarioId
                : username;

        Path baseDir = getBaseDirectory();
        Files.createDirectories(baseDir);

        Path usuarioDir = baseDir.resolve(construirNombreUsuario(nombreUsuario, usuarioId));
        Files.createDirectories(usuarioDir);

        int anio = fin != null ? fin.toLocalDate().getYear() : inicio.toLocalDate().getYear();
        Path anioDir = usuarioDir.resolve(String.valueOf(anio));
        Files.createDirectories(anioDir);

        LocalDateTime referencia = fin != null ? fin : inicio;
        if (referencia == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin no pueden ser ambas nulas");
        }
        String nombreMes = referencia.format(MES_NOMBRE_FORMATTER);
        Path mesDir = anioDir.resolve(nombreMes);
        Files.createDirectories(mesDir);

        Path tipoDir = mesDir.resolve(tipo.getFolderName());
        Files.createDirectories(tipoDir);

        String nombreArchivo = construirNombreArchivo(tipo, inicio, fin);
        return tipoDir.resolve(nombreArchivo);
    }

    private static String construirNombreUsuario(String username, int usuarioId) {
        String normalizado = Normalizer.normalize(username, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String limpio = normalizado
                .replaceAll("[^a-zA-Z0-9-_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_", "")
                .replaceAll("_$", "");
        if (limpio.isBlank()) {
            limpio = "usuario";
        }
        return limpio.toLowerCase(Locale.getDefault()) + "_" + usuarioId;
    }

    private static String construirNombreArchivo(ResumenTipo tipo, LocalDateTime inicio, LocalDateTime fin) {
        LocalDate fechaFin = fin != null ? fin.toLocalDate() : inicio.toLocalDate();
        return switch (tipo) {
            case DIARIO -> "resumen_turno_" + fechaFin.format(DIA_FORMATTER) + ".pdf";
            case SEMANAL -> {
                LocalDate fechaInicio = inicio.toLocalDate();
                yield "resumen_semanal_" + fechaInicio.format(DIA_FORMATTER)
                        + "_" + fechaFin.format(DIA_FORMATTER) + ".pdf";
            }
            case MENSUAL -> {
                YearMonth mes = YearMonth.from(fechaFin);
                yield "resumen_mensual_" + mes.format(MES_FORMATTER) + ".pdf";
            }
            case ANUAL -> "resumen_anual_" + fechaFin.getYear() + ".pdf";
            case TODOS -> throw new IllegalArgumentException("No se puede construir archivo para TODOS");
        };
    }
}
package models;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Auditoria {
    private static final DateTimeFormatter SQLITE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final int id;
    private final String usuario;
    private final String accion;
    private final String detalle;
    private final String timestamp;

    private final ResumenTipo resumenTipo;
    private final Path archivo;
    private final LocalDateTime fecha;

    public Auditoria(int id, String usuario, String accion, String detalle, String timestamp) {
        this.id = id;
        this.usuario = usuario;
        this.accion = accion;
        this.detalle = detalle;
        this.timestamp = timestamp;
        this.resumenTipo = ResumenTipo.fromAccion(accion);
        this.archivo = parsePath(detalle);
        this.fecha = parseTimestamp(timestamp);
    }

    public int getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getAccion() {
        return accion;
    }

    public String getDetalle() {
        return detalle;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public ResumenTipo getResumenTipo() {
        return resumenTipo;
    }

    public Path getArchivo() {
        return archivo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public Integer getAnio() {
        return fecha != null ? fecha.getYear() : null;
    }

    public Month getMes() {
        return fecha != null ? fecha.getMonth() : null;
    }

    public String getNombreArchivo() {
        if (archivo != null) {
            return archivo.getFileName().toString();
        }
        return detalle != null ? detalle : "";
    }

    private static Path parsePath(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Paths.get(value);
        } catch (InvalidPathException ex) {
            return null;
        }
    }

    private static LocalDateTime parseTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, SQLITE_FORMATTER);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }
}
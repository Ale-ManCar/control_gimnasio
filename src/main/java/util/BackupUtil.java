package util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupUtil {

    public static class RespaldoInfo {
        private final LocalDateTime fechaHora;
        private final long tamano;
        private final String ruta;

        public RespaldoInfo(LocalDateTime fechaHora, long tamano, String ruta) {
            this.fechaHora = fechaHora;
            this.tamano = tamano;
            this.ruta = ruta;
        }

        public LocalDateTime getFechaHora() { return fechaHora; }
        public long getTamano() { return tamano; }
        public String getRuta() { return ruta; }
    }

    public static String crearZip(String destino) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(Paths.get(destino)))) {
            agregarArchivo(zos, Paths.get("database/gimnasio.db"), "database/gimnasio.db");
            agregarDirectorio(zos, Paths.get("facturas"), "facturas");
            agregarDirectorio(zos, Paths.get("reports"), "reports");
            agregarDirectorio(zos, Paths.get("adjuntos"), "adjuntos");
            agregarDirectorio(zos, Paths.get("fotos"), "fotos");
        }
        return destino;
    }

    private static void agregarArchivo(ZipOutputStream zos, Path path, String entryName) throws IOException {
        if (!Files.exists(path)) return;
        zos.putNextEntry(new ZipEntry(entryName));
        try (InputStream in = Files.newInputStream(path)) {
            in.transferTo(zos);
        }
        zos.closeEntry();
    }

    private static void agregarDirectorio(ZipOutputStream zos, Path dir, String base) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walk(dir).filter(Files::isRegularFile).forEach(p -> {
            String entry = base + "/" + dir.relativize(p).toString().replace("\\", "/");
            try {
                agregarArchivo(zos, p, entry);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static List<RespaldoInfo> listarRespaldos() {
        List<RespaldoInfo> lista = new ArrayList<>();
        Path dir = Paths.get("respaldos");
        if (Files.exists(dir)) {
            try {
                Files.walk(dir).filter(p -> p.toString().endsWith(".zip")).forEach(p -> {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                        LocalDateTime fecha = LocalDateTime.ofInstant(attrs.creationTime().toInstant(), ZoneId.systemDefault());
                        long size = Files.size(p);
                        lista.add(new RespaldoInfo(fecha, size, p.toString()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        lista.sort(Comparator.comparing(RespaldoInfo::getFechaHora).reversed());
        return lista;
    }

    public static String formatearTamano(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.2f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.2f MB", mb);
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }

    public static String generarNombreRespaldo() {
        LocalDateTime ahora = LocalDateTime.now();
        String dir = String.format("respaldos/%04d/%02d/", ahora.getYear(), ahora.getMonthValue());
        try {
            Files.createDirectories(Paths.get(dir));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String nombre = ahora.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return dir + "respaldo_" + nombre + ".zip";
    }
}
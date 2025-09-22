package util;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.view.JasperViewer;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import javafx.collections.ObservableList;
import models.Egreso;
import models.Equipo;
import models.CoachClientes;
import models.PagoDetalle;
import models.Auditoria;
import models.Pago;
import models.ResumenTipo;
import util.AuditoriaFileUtil;
import util.AuditoriaUtil;
import java.io.InputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReporteUtil {
    private static final DateTimeFormatter RESUMEN_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter SQLITE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern PAGO_ID_PATTERN = Pattern.compile("Pago\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    public static void generarReporteFinanciero(int mes, int anio) {
        try {
            InputStream reporteStream = ReporteUtil.class.getResourceAsStream("/reports/reporte_financiero.jrxml");
            if (reporteStream == null) {
                System.err.println("❌ No se encontró el archivo reporte_financiero.jrxml");
                return;
            }

            // Obtener datos para el mes y año específico
            double totalMembresias = DatabaseUtil.obtenerTotalPagosParaMes(mes, anio);
            double totalVentas = DatabaseUtil.obtenerTotalVentasParaMes(mes, anio);
            double totalEgresos = DatabaseUtil.obtenerTotalEgresosParaMes(mes, anio);
            double resultadoNeto = (totalMembresias + totalVentas) - totalEgresos;

            // Obtener egresos del mes específico
            List<Egreso> egresos = DatabaseUtil.getEgresosParaMes(mes, anio).stream()
                    .map(e -> {
                        Egreso eg = new Egreso();
                        eg.setId(e.getId());
                        eg.setDescripcion(e.getDescripcion());
                        eg.setMonto(e.getMonto());
                        eg.setFecha(e.getFecha());
                        eg.setCategoria(e.getCategoria());
                        return eg;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> parametros = new HashMap<>();
            parametros.put("totalMembresias", totalMembresias);
            parametros.put("totalVentas", totalVentas);
            parametros.put("totalEgresos", totalEgresos);
            parametros.put("resultadoNeto", resultadoNeto);
            parametros.put("mesReporte", mes);
            parametros.put("anioReporte", anio);

            JRDataSource dataSourceEgresos = new JRBeanCollectionDataSource(egresos);

            JasperReport jasperReport = JasperCompileManager.compileReport(reporteStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, dataSourceEgresos);

            if (jasperPrint.getPages() != null && !jasperPrint.getPages().isEmpty()) {
                JasperViewer.viewReport(jasperPrint, false);
                String nombreArchivo = String.format("reporte_financiero_%02d_%d.pdf", mes, anio);
                String pdfPath = System.getProperty("user.dir") + File.separator + nombreArchivo;
                JasperExportManager.exportReportToPdfFile(jasperPrint, pdfPath);
                System.out.println("✅ Reporte financiero generado en: " + pdfPath);
            }

        } catch (Exception e) {
            System.err.println("❌ Error generando reporte financiero: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para generar reporte del mes actual (mantiene compatibilidad)
    public static void generarReporteFinanciero() {
        LocalDate hoy = LocalDate.now();
        generarReporteFinanciero(hoy.getMonthValue(), hoy.getYear());
    }

    public static void generarReporteFinanciero(LocalDate fechaInicio, LocalDate fechaFin, Integer clienteId, String tipoMembresia) {
        try {
            InputStream reporteStream = ReporteUtil.class.getResourceAsStream("/reports/reporte_financiero.jrxml");
            if (reporteStream == null) {
                System.err.println("❌ No se encontró el archivo reporte_financiero.jrxml");
                return;
            }

            Map<String, Double> totales = DatabaseUtil.obtenerIngresosVsEgresos(fechaInicio, fechaFin, clienteId, tipoMembresia);
            double totalMembresias = totales.getOrDefault("membresias", 0.0);
            double totalVentas = totales.getOrDefault("ventas", 0.0);
            double totalEgresos = totales.getOrDefault("egresos", 0.0);
            double resultadoNeto = (totalMembresias + totalVentas) - totalEgresos;

            List<Egreso> egresos = DatabaseUtil.filtrarEgresos(fechaInicio, fechaFin, null);

            Map<String, Object> parametros = new HashMap<>();
            parametros.put("totalMembresias", totalMembresias);
            parametros.put("totalVentas", totalVentas);
            parametros.put("totalEgresos", totalEgresos);
            parametros.put("resultadoNeto", resultadoNeto);
            parametros.put("fechaInicio", fechaInicio.toString());
            parametros.put("fechaFin", fechaFin.toString());

            JRDataSource dataSourceEgresos = new JRBeanCollectionDataSource(egresos);
            JasperReport jasperReport = JasperCompileManager.compileReport(reporteStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, dataSourceEgresos);

            JasperViewer.viewReport(jasperPrint, false);
            String baseName = String.format("reporte_financiero_%s_%s", fechaInicio, fechaFin);
            String pdfPath = System.getProperty("user.dir") + File.separator + baseName + ".pdf";
            JasperExportManager.exportReportToPdfFile(jasperPrint, pdfPath);

            JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            String xlsPath = System.getProperty("user.dir") + File.separator + baseName + ".xlsx";
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(xlsPath));
            SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
            configuration.setDetectCellType(true);
            exporter.setConfiguration(configuration);
            exporter.exportReport();

            System.out.println("✅ Reporte financiero generado en: " + pdfPath + " y " + xlsPath);
        } catch (Exception e) {
            System.err.println("❌ Error generando reporte financiero: " + e.getMessage());
        }
    }

    public static void generarReportePagosFiltrado(Integer clienteId, LocalDate fechaInicio, LocalDate fechaFin, String tipoMembresia) {
        try {
            List<PagoDetalle> pagos = DatabaseUtil.buscarPagos(clienteId, fechaInicio, fechaFin, tipoMembresia);

            InputStream reporteStream = ReporteUtil.class.getResourceAsStream("/reports/reporte_financiero.jrxml");
            if (reporteStream == null) {
                System.err.println("❌ No se encontró el archivo reporte_financiero.jrxml");
                return;
            }

            Map<String, Object> parametros = new HashMap<>();
            parametros.put("fechaInicio", fechaInicio != null ? fechaInicio.toString() : "");
            parametros.put("fechaFin", fechaFin != null ? fechaFin.toString() : "");

            JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(pagos);
            JasperReport jasperReport = JasperCompileManager.compileReport(reporteStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, ds);

            JasperViewer.viewReport(jasperPrint, false);
            String baseName = String.format("reporte_pagos_filtrado_%s_%s",
                    fechaInicio != null ? fechaInicio : "", fechaFin != null ? fechaFin : "");
            String pdfPath = System.getProperty("user.dir") + File.separator + baseName + ".pdf";
            JasperExportManager.exportReportToPdfFile(jasperPrint, pdfPath);
            System.out.println("✅ Reporte de pagos generado en: " + pdfPath);
        } catch (Exception e) {
            System.err.println("❌ Error generando reporte de pagos: " + e.getMessage());
        }
    }

    public static void generarReporteEquipos(int diasAviso) {
        try {
            InputStream reporteStream = ReporteUtil.class.getResourceAsStream("/reports/equipos_estado.jrxml");
            if (reporteStream == null) {
                System.err.println("❌ No se encontró el archivo equipos_estado.jrxml");
                return;
            }

            List<Equipo> proximos = DatabaseUtil.obtenerEquiposConMantenimientoProximo(diasAviso);
            List<Equipo> vencidos = DatabaseUtil.obtenerEquiposConMantenimientoVencido();
            List<Equipo> criticos = DatabaseUtil.obtenerEquiposEnEstadoCritico();
            List<Equipo> malEstado = DatabaseUtil.obtenerEquiposEnMalEstado();

            Map<Integer, Equipo> combinado = new LinkedHashMap<>();
            for (Equipo equipo : vencidos) {
                combinado.put(equipo.getId(), equipo);
            }
            for (Equipo equipo : proximos) {
                combinado.putIfAbsent(equipo.getId(), equipo);
            }
            for (Equipo equipo : criticos) {
                combinado.putIfAbsent(equipo.getId(), equipo);
            }
            for (Equipo equipo : malEstado) {
                combinado.putIfAbsent(equipo.getId(), equipo);
            }

            Map<String, Object> parametros = new HashMap<>();
            parametros.put("diasAviso", diasAviso);
            parametros.put("totalProximos", proximos.size());
            parametros.put("totalVencidos", vencidos.size());
            parametros.put("totalCriticos", criticos.size());
            parametros.put("totalMalEstado", malEstado.size());

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(new ArrayList<>(combinado.values()));
            JasperReport jasperReport = JasperCompileManager.compileReport(reporteStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, dataSource);

            if (jasperPrint.getPages().isEmpty()) {
                System.out.println("⚠️ No se encontraron equipos para el reporte.");
                return;
            }

            JasperViewer.viewReport(jasperPrint, false);
            String nombreArchivo = String.format("equipos_estado_%s.pdf", LocalDate.now());
            String ruta = System.getProperty("user.dir") + File.separator + nombreArchivo;
            JasperExportManager.exportReportToPdfFile(jasperPrint, ruta);
            System.out.println("✅ Reporte de equipos generado en: " + ruta);
        } catch (Exception e) {
            System.err.println("❌ Error generando reporte de equipos: " + e.getMessage());
            throw new RuntimeException("Error generando reporte de equipos", e);
        }
    }

    public static void generarReporteActividadRecepcionista() {
        try {
            InputStream reporteStream = ReporteUtil.class.getResourceAsStream("/reports/actividad_recepcionista.jrxml");
            if (reporteStream == null) {
                System.err.println("❌ No se encontró el archivo actividad_recepcionista.jrxml");
                return;
            }

            String sql = "SELECT u.username AS recepcionista, " +
                    "COUNT(t.id) AS turnos, " +
                    "COALESCE(SUM(t.ingresos_ventas),0) AS ventas, " +
                    "COALESCE(SUM(t.ingresos_clientes),0) AS membresias " +
                    "FROM usuarios u LEFT JOIN turnos t ON u.id = t.usuario_id " +
                    "WHERE u.rol = 'RECEPCIONISTA' GROUP BY u.username";

            List<ActividadRecepcionista> data = new ArrayList<>();
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.add(new ActividadRecepcionista(
                            rs.getString("recepcionista"),
                            rs.getInt("turnos"),
                            rs.getDouble("ventas"),
                            rs.getDouble("membresias")));
                }
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(reporteStream);
            JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(data);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, new HashMap<>(), ds);
            JasperViewer.viewReport(jasperPrint, false);

            String pdfPath = System.getProperty("user.dir") + File.separator + "actividad_recepcionista.pdf";
            JasperExportManager.exportReportToPdfFile(jasperPrint, pdfPath);
            System.out.println("✅ Reporte de actividad generado en: " + pdfPath);
        } catch (Exception e) {
            System.err.println("❌ Error generando reporte de actividad: " + e.getMessage());
        }
    }

    public static Path generarResumenTurno(int usuarioId, LocalDateTime inicio, LocalDateTime fin) {
        return generarResumenTurno(usuarioId, null, inicio, fin, true, null);
    }

    public static Path generarResumenTurno(int usuarioId, LocalDateTime inicio, LocalDateTime fin, boolean mostrar) {
        return generarResumenTurno(usuarioId, null, inicio, fin, mostrar, null);
    }

    public static Path generarResumenTurno(int usuarioId, Integer turnoId, LocalDateTime inicio, LocalDateTime fin,
                                           boolean mostrar, Path destinoPersonalizado) {
        return generarResumenPeriodo(usuarioId, inicio, fin, "Resumen diario de turno", ResumenTipo.DIARIO,
                mostrar, destinoPersonalizado, turnoId);
    }

    public static Path generarResumenSemanal(int usuarioId, LocalDateTime inicio, LocalDateTime fin) {
        return generarResumenSemanalIngresos(usuarioId, inicio, fin, false);
    }

    public static Path generarResumenSemanal(int usuarioId, LocalDateTime inicio, LocalDateTime fin, boolean mostrar) {
        return generarResumenSemanalIngresos(usuarioId, inicio, fin, mostrar);
    }

    public static Path generarResumenSemanalIngresos(int usuarioId, LocalDateTime inicio, LocalDateTime fin, boolean mostrar) {
        return generarResumenTotalesPeriodo(usuarioId, inicio, fin, "Resumen semanal", ResumenTipo.SEMANAL, mostrar,
                "/reports/resumen_semanal.jrxml", null, null);
    }

    public static Path generarResumenMensual(int usuarioId, LocalDateTime inicio, LocalDateTime fin) {
        return generarResumenMensualIngresos(usuarioId, inicio, fin, false);
    }

    public static Path generarResumenMensual(int usuarioId, LocalDateTime inicio, LocalDateTime fin, boolean mostrar) {
        return generarResumenMensualIngresos(usuarioId, inicio, fin, mostrar);
    }

    public static Path generarResumenMensualIngresos(int usuarioId, LocalDateTime inicio, LocalDateTime fin, boolean mostrar) {
        return generarResumenTotalesPeriodo(usuarioId, inicio, fin, "Resumen mensual", ResumenTipo.MENSUAL, mostrar,
                "/reports/resumen_mensual.jrxml", null, null);
    }

    public static Path generarResumenAnual(int usuarioId, LocalDateTime inicio, LocalDateTime fin) {
        return generarResumenAnualIngresos(usuarioId, inicio, fin, false);
    }

    public static Path generarResumenAnual(int usuarioId, LocalDateTime inicio, LocalDateTime fin, boolean mostrar) {
        return generarResumenAnualIngresos(usuarioId, inicio, fin, mostrar);
    }

    public static Path generarResumenAnualIngresos(int usuarioId, LocalDateTime inicio, LocalDateTime fin, boolean mostrar) {
        return generarResumenTotalesPeriodo(usuarioId, inicio, fin, "Resumen anual", ResumenTipo.ANUAL, mostrar,
                "/reports/resumen_anual.jrxml", null, null);
    }

    private static Path generarResumenPeriodo(int usuarioId, LocalDateTime inicio, LocalDateTime fin,
                                              String titulo, ResumenTipo tipo, boolean mostrar,
                                              Path destinoPersonalizado, Integer turnoId) {
        if (inicio == null || fin == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
        }

        LocalDateTime start = inicio;
        LocalDateTime end = fin;
        if (end.isBefore(start)) {
            LocalDateTime temp = start;
            start = end;
            end = temp;
        }

        try {
            InputStream reporteStream = ReporteUtil.class.getResourceAsStream("/reports/resumen_turno.jrxml");
            if (reporteStream == null) {
                System.err.println("❌ No se encontró el archivo resumen_turno.jrxml");
                return null;
            }

            String username = usuarioId > 0 ? DatabaseUtil.obtenerNombreUsuarioPorId(usuarioId) : null;
            if (username == null || username.isBlank()) {
                username = "Recepcionista " + usuarioId;
            }

            List<Pago> clientesRegistrados = DatabaseUtil.listarClientesRegistrados(usuarioId, start, end);
            List<Pago> renovaciones = DatabaseUtil.listarMembresiasRenovadas(usuarioId, start, end);
            Map<String, Number> ingresosPagos = DatabaseUtil.obtenerIngresosPagos(usuarioId, start, end);
            List<Pago> pagosAnulados = DatabaseUtil.listarPagosAnulados(usuarioId, start, end);
            double totalVentas = DatabaseUtil.obtenerTotalVentasEntre(start, end);

            int cantidadPagos = ingresosPagos.getOrDefault("cantidad", 0).intValue();
            double totalMembresias = ingresosPagos.getOrDefault("total", 0).doubleValue();

            Map<Integer, LocalDateTime> tiemposAnulacion = obtenerTiemposAccion(usuarioId, start, end, "ANULAR_PAGO");

            Map<String, Object> parametros = new HashMap<>();
            parametros.put("REPORT_TITLE", construirTitulo(titulo, username));
            parametros.put("RANGO_TURNO", construirRangoPeriodo(start, end));
            parametros.put("CLIENTES_REGISTRADOS", construirListadoPagos(clientesRegistrados));
            parametros.put("MEMBRESIAS_RENOVADAS", construirListadoPagos(renovaciones));
            parametros.put("INGRESOS_PAGOS", construirIngresosTexto(cantidadPagos, totalMembresias));
            parametros.put("TOTAL_MEMBRESIAS", totalMembresias);
            parametros.put("TOTAL_VENTAS", totalVentas);
            parametros.put("PAGOS_ANULADOS", construirPagosAnulados(pagosAnulados, tiemposAnulacion));

            JasperReport jasperReport = JasperCompileManager.compileReport(reporteStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, new JREmptyDataSource());

            if (mostrar) {
                JasperViewer.viewReport(jasperPrint, false);
            }

            Path destino = destinoPersonalizado != null
                    ? AuditoriaFileUtil.ensureCustomDestination(destinoPersonalizado)
                    : AuditoriaFileUtil.ensureResumenPath(username, usuarioId, tipo, start, end, turnoId);
            JasperExportManager.exportReportToPdfFile(jasperPrint, destino.toString());

            if (usuarioId > 0 && tipo.getAccion() != null && destinoPersonalizado == null) {
                AuditoriaUtil.registrarAccion(usuarioId, tipo.getAccion(), destino.toString());
            }

            System.out.println("✅ " + titulo + " generado en: " + destino.toAbsolutePath());
            return destino;
        } catch (Exception e) {
            System.err.println("❌ Error generando " + titulo.toLowerCase() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static Path generarResumenTotalesPeriodo(int usuarioId, LocalDateTime inicio, LocalDateTime fin,
                                                     String titulo, ResumenTipo tipo, boolean mostrar,
                                                     String plantilla, Path destinoPersonalizado, Integer turnoId) {
        if (inicio == null || fin == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
        }

        LocalDateTime start = inicio;
        LocalDateTime end = fin;
        if (end.isBefore(start)) {
            LocalDateTime temp = start;
            start = end;
            end = temp;
        }

        try {
            InputStream reporteStream = ReporteUtil.class.getResourceAsStream(plantilla);
            if (reporteStream == null) {
                System.err.println("❌ No se encontró la plantilla " + plantilla);
                return null;
            }

            String username = usuarioId > 0 ? DatabaseUtil.obtenerNombreUsuarioPorId(usuarioId) : null;
            if (username == null || username.isBlank()) {
                username = "Recepcionista " + usuarioId;
            }

            Map<String, Number> ingresosPagos = DatabaseUtil.obtenerIngresosPagos(usuarioId, start, end);
            double totalMembresias = ingresosPagos.getOrDefault("total", 0).doubleValue();
            double totalVentas = DatabaseUtil.obtenerTotalVentasEntre(start, end);
            double totalGeneral = totalMembresias + totalVentas;

            Map<String, Object> parametros = new HashMap<>();
            parametros.put("REPORT_TITLE", construirTitulo(titulo, username));
            parametros.put("RANGO_TURNO", construirRangoPeriodo(start, end));
            parametros.put("TOTAL_MEMBRESIAS", totalMembresias);
            parametros.put("TOTAL_VENTAS", totalVentas);
            parametros.put("TOTAL_GENERAL", totalGeneral);

            JasperReport jasperReport = JasperCompileManager.compileReport(reporteStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, new JREmptyDataSource());

            if (mostrar) {
                JasperViewer.viewReport(jasperPrint, false);
            }

            Path destino = destinoPersonalizado != null
                    ? AuditoriaFileUtil.ensureCustomDestination(destinoPersonalizado)
                    : AuditoriaFileUtil.ensureResumenPath(username, usuarioId, tipo, start, end, turnoId);
            JasperExportManager.exportReportToPdfFile(jasperPrint, destino.toString());

            if (usuarioId > 0 && tipo.getAccion() != null && destinoPersonalizado == null) {
                AuditoriaUtil.registrarAccion(usuarioId, tipo.getAccion(), destino.toString());
            }

            System.out.println("✅ " + titulo + " generado en: " + destino.toAbsolutePath());
            return destino;
        } catch (Exception e) {
            System.err.println("❌ Error generando " + titulo.toLowerCase() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static void generarReporteMembresiasPorVencer() {
        try {
            InputStream reporteStream = ReporteUtil.class.getResourceAsStream("/reports/membresias_por_vencer.jrxml");
            if (reporteStream == null) {
                System.err.println("❌ No se encontró el archivo membresias_por_vencer.jrxml");
                return;
            }

            String sql = "SELECT nombres, apellidos, telefono, fecha_vencimiento AS fechaVencimiento " +
                    "FROM clientes WHERE activo = 1 " +
                    "AND date(fecha_vencimiento) BETWEEN date('now') AND date('now','+7 day') " +
                    "ORDER BY fecha_vencimiento";

            List<MembresiaPorVencer> lista = new ArrayList<>();
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new MembresiaPorVencer(
                            rs.getString("nombres"),
                            rs.getString("apellidos"),
                            rs.getString("telefono"),
                            LocalDate.parse(rs.getString("fechaVencimiento"))));
                }
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(reporteStream);
            JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(lista);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, new HashMap<>(), ds);
            JasperViewer.viewReport(jasperPrint, false);

            String pdfPath = System.getProperty("user.dir") + File.separator + "membresias_por_vencer.pdf";
            JasperExportManager.exportReportToPdfFile(jasperPrint, pdfPath);
            System.out.println("✅ Reporte de membresías por vencer generado en: " + pdfPath);
        } catch (Exception e) {
            System.err.println("❌ Error generando reporte de membresías: " + e.getMessage());
        }
    }

    public static void generarReporteClientesMorososPDF() {
        try {
            int total = DatabaseUtil.contarClientesMorosos();
            String contenido = "Clientes morosos: " + total + "\n";
            Path path = Path.of("clientes_morosos.pdf");
            Files.writeString(path, contenido);
            System.out.println("✅ Reporte de clientes morosos PDF generado en: " + path.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("❌ Error generando reporte de clientes morosos PDF: " + e.getMessage());
        }
    }

    public static void generarReporteClientesMorososExcel() {
        try {
            int total = DatabaseUtil.contarClientesMorosos();
            String contenido = "ClientesMorosos\n" + total + "\n";
            Path path = Path.of("clientes_morosos.csv");
            Files.writeString(path, contenido);
            System.out.println("✅ Reporte de clientes morosos Excel generado en: " + path.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("❌ Error generando reporte de clientes morosos Excel: " + e.getMessage());
        }
    }

    public static void generarReporteCoachesConMasClientesPDF() {
        try {
            List<CoachClientes> lista = DatabaseUtil.listarCoachesConMasClientes();
            StringBuilder contenido = new StringBuilder("Coach,Clientes\n");
            for (CoachClientes c : lista) {
                contenido.append(c.getCoach()).append(',').append(c.getClientes()).append("\n");
            }
            Path path = Path.of("coaches_con_mas_clientes.pdf");
            Files.writeString(path, contenido.toString());
            System.out.println("✅ Reporte de coaches con más clientes PDF generado en: " + path.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("❌ Error generando reporte de coaches PDF: " + e.getMessage());
        }
    }

    public static void generarReporteCoachesConMasClientesExcel() {
        try {
            List<CoachClientes> lista = DatabaseUtil.listarCoachesConMasClientes();
            StringBuilder contenido = new StringBuilder("Coach,Clientes\n");
            for (CoachClientes c : lista) {
                contenido.append(c.getCoach()).append(',').append(c.getClientes()).append("\n");
            }
            Path path = Path.of("coaches_con_mas_clientes.csv");
            Files.writeString(path, contenido.toString());
            System.out.println("✅ Reporte de coaches con más clientes Excel generado en: " + path.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("❌ Error generando reporte de coaches Excel: " + e.getMessage());
        }
    }

    public static void generarReporteDashboardPDF() {
        try {
            Map<String, Integer> stats = DatabaseUtil.getAdminStats();
            StringBuilder contenido = new StringBuilder();
            contenido.append("Reporte Dashboard\n\n");
            stats.forEach((k, v) -> contenido.append(k).append(": ").append(v).append("\n"));
            Path path = Path.of("reporte_dashboard.pdf");
            Files.writeString(path, contenido.toString());
            System.out.println("✅ Reporte de dashboard PDF generado en: " + path.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("❌ Error generando reporte de dashboard PDF: " + e.getMessage());
        }
    }

    public static void generarReporteDashboardExcel() {
        try {
            Map<String, Integer> stats = DatabaseUtil.getAdminStats();
            StringBuilder contenido = new StringBuilder();
            contenido.append("Metrica,Valor\n");
            stats.forEach((k, v) -> contenido.append(k).append(",").append(v).append("\n"));
            Path path = Path.of("reporte_dashboard.csv");
            Files.writeString(path, contenido.toString());
            System.out.println("✅ Reporte de dashboard Excel generado en: " + path.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("❌ Error generando reporte de dashboard Excel: " + e.getMessage());
        }
    }

    public static void generarReporteAuditoria(List<Auditoria> registros) {
        try {
            InputStream reporteStream = ReporteUtil.class.getResourceAsStream("/reports/auditoria.jrxml");
            if (reporteStream == null) {
                System.err.println("❌ No se encontró el archivo auditoria.jrxml");
                return;
            }

            JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(registros);
            JasperReport jasperReport = JasperCompileManager.compileReport(reporteStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, new HashMap<>(), ds);

            JasperViewer.viewReport(jasperPrint, false);
            String baseName = "reporte_auditoria";
            String pdfPath = System.getProperty("user.dir") + File.separator + baseName + ".pdf";
            JasperExportManager.exportReportToPdfFile(jasperPrint, pdfPath);

            JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            String xlsPath = System.getProperty("user.dir") + File.separator + baseName + ".xlsx";
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(xlsPath));
            SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
            configuration.setDetectCellType(true);
            exporter.setConfiguration(configuration);
            exporter.exportReport();

            System.out.println("✅ Reporte de auditoría generado en: " + pdfPath + " y " + xlsPath);
        } catch (Exception e) {
            System.err.println("❌ Error generando reporte de auditoría: " + e.getMessage());
        }
    }

    private static Map<Integer, LocalDateTime> obtenerTiemposAccion(int usuarioId, LocalDateTime inicio, LocalDateTime fin, String accion) {
        Map<Integer, LocalDateTime> tiempos = new HashMap<>();
        ObservableList<Auditoria> registros = AuditoriaUtil.filtrarAcciones(usuarioId, inicio.toLocalDate(), fin.toLocalDate(), accion);
        for (Auditoria registro : registros) {
            if (accion != null && !accion.equalsIgnoreCase(registro.getAccion())) {
                continue;
            }
            LocalDateTime timestamp = parseTimestamp(registro.getTimestamp());
            if (timestamp == null || timestamp.isBefore(inicio) || timestamp.isAfter(fin)) {
                continue;
            }
            Integer pagoId = extraerPagoId(registro.getDetalle());
            if (pagoId != null) {
                tiempos.put(pagoId, timestamp);
            }
        }
        return tiempos;
    }

    private static String construirRangoPeriodo(LocalDateTime inicio, LocalDateTime fin) {
        return String.format("Desde %s hasta %s", RESUMEN_FORMATTER.format(inicio), RESUMEN_FORMATTER.format(fin));
    }

    private static String construirTitulo(String titulo, String username) {
        if (username == null || username.isBlank()) {
            return titulo;
        }
        return titulo + " - " + username;
    }

    private static String construirListadoPagos(List<Pago> pagos) {
        if (pagos == null || pagos.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int indice = 1;
        DateTimeFormatter fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Pago pago : pagos) {
            String nombre = obtenerNombreClienteSeguro(pago.getClienteId());
            sb.append(indice++).append('.').append(' ').append(nombre);
            if (pago.getTipoMembresia() != null && !pago.getTipoMembresia().isBlank()) {
                sb.append(" - ").append(pago.getTipoMembresia());
            }
            if (pago.getFechaPago() != null) {
                sb.append(" (" + pago.getFechaPago().format(fechaFormatter) + ")");
            }
            sb.append(" - $").append(String.format("%.2f", pago.getMonto()));
            sb.append('\n');
        }
        return sb.toString().trim();
    }

    private static String construirIngresosTexto(int cantidad, double total) {
        return String.format("Pagos activos: %d%nTotal recibido: $%.2f", cantidad, total);
    }

    private static String construirPagosAnulados(List<Pago> pagos, Map<Integer, LocalDateTime> tiempos) {
        if (pagos == null || pagos.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Pago pago : pagos) {
            String nombre = obtenerNombreClienteSeguro(pago.getClienteId());
            sb.append("- ").append(nombre).append(" (#").append(pago.getId()).append(") ");
            sb.append(String.format("$%.2f", pago.getMonto()));
            LocalDateTime timestamp = tiempos.get(pago.getId());
            if (timestamp != null) {
                sb.append(" - ").append(RESUMEN_FORMATTER.format(timestamp));
            }
            sb.append('\n');
        }
        return sb.toString().trim();
    }

    private static Integer extraerPagoId(String detalle) {
        if (detalle == null) {
            return null;
        }
        Matcher matcher = PAGO_ID_PATTERN.matcher(detalle);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static LocalDateTime parseTimestamp(String timestamp) {
        if (timestamp == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(timestamp, SQLITE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    private static String obtenerNombreClienteSeguro(int clienteId) {
        try {
            String nombre = DatabaseUtil.obtenerNombreCompletoCliente(clienteId);
            if (nombre != null && !nombre.isBlank()) {
                return nombre;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Cliente #" + clienteId;
    }

    public record ActividadRecepcionista(String recepcionista, int turnos, double ventas, double membresias) {}
    public record MembresiaPorVencer(String nombres, String apellidos, String telefono, LocalDate fechaVencimiento) {}
}
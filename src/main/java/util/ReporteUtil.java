package util;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import models.Egreso;
import models.CoachClientes;
import models.PagoDetalle;
import models.ProveedorPrecio;
import models.Auditoria;
import java.io.InputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReporteUtil {
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

    public static void generarReporteInventario(int mes, int anio) {
        try {
            InputStream reporteStream = ReporteUtil.class.getResourceAsStream("/reports/inventario.jrxml");
            if (reporteStream == null) {
                System.err.println("❌ No se encontró el archivo inventario.jrxml");
                return;
            }

            String sql = "SELECT e.nombre AS producto, " +
                    "COALESCE(SUM(cd.cantidad),0) AS compras, " +
                    "e.stock AS stockFinal " +
                    "FROM equipos e " +
                    "LEFT JOIN compras_detalle cd ON e.id = cd.equipo_id " +
                    "LEFT JOIN compras c ON cd.compra_id = c.id " +
                    "AND strftime('%Y', c.fecha) = ? AND strftime('%m', c.fecha) = ? " +
                    "GROUP BY e.nombre, e.stock";

            List<ItemInventario> items = new ArrayList<>();
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, String.format("%04d", anio));
                ps.setString(2, String.format("%02d", mes));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int compras = rs.getInt("compras");
                        int stockFinal = rs.getInt("stockFinal");
                        int stockInicial = stockFinal - compras;
                        items.add(new ItemInventario(
                                rs.getString("producto"),
                                stockInicial,
                                compras,
                                0,
                                stockFinal));
                    }
                }
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(reporteStream);
            JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(items);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, new HashMap<>(), ds);
            JasperViewer.viewReport(jasperPrint, false);

            String nombreArchivo = String.format("inventario_%02d_%d.pdf", mes, anio);
            String pdfPath = System.getProperty("user.dir") + File.separator + nombreArchivo;
            JasperExportManager.exportReportToPdfFile(jasperPrint, pdfPath);
            System.out.println("✅ Reporte de inventario generado en: " + pdfPath);
        } catch (Exception e) {
            System.err.println("❌ Error generando reporte de inventario: " + e.getMessage());
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

    public static void generarReporteComparadorPrecios(String producto, List<ProveedorPrecio> datos) {
        try {
            String base = "comparador_precios_" + producto.replaceAll("[^a-zA-Z0-9]", "_");
            Path pdf = Path.of(base + ".pdf");
            Path xls = Path.of(base + ".xlsx");
            StringBuilder contenido = new StringBuilder("Proveedor\tPrecio\n");
            for (ProveedorPrecio p : datos) {
                contenido.append(p.getProveedor()).append('\t').append(p.getPrecio()).append('\n');
            }
            Files.writeString(pdf, contenido.toString());
            Files.writeString(xls, contenido.toString());
            System.out.println("✅ Reporte comparador de precios generado en: " + pdf.toAbsolutePath() + " y " + xls.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("❌ Error generando reporte comparador de precios: " + e.getMessage());
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

    public record ActividadRecepcionista(String recepcionista, int turnos, double ventas, double membresias) {}
    public record ItemInventario(String producto, int stockInicial, int compras, int bajas, int stockFinal) {}
    public record MembresiaPorVencer(String nombres, String apellidos, String telefono, LocalDate fechaVencimiento) {}
}
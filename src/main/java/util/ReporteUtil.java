package util;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;
import models.Egreso;
import models.Producto;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class ReporteUtil {
    public static void generarReporteFinanciero(int mes, int anio) {
        try {
            InputStream reporteStream = ReporteUtil.class.getResourceAsStream("/reports/reporte_financiero.jrxml");
            if (reporteStream == null) {
                System.err.println("❌ No se encontró el archivo reporte_financiero.jrxml");
                return;
            }

            // Obtener datos para el mes y año específico
            double[] acumulados = DatabaseUtil.obtenerIngresosEgresosAcumulados(mes, anio);
            double totalIngresos = acumulados[0];
            double totalEgresos = acumulados[1];
            double totalMembresias = DatabaseUtil.obtenerTotalPagosParaMes(mes, anio);
            double totalVentas = DatabaseUtil.obtenerTotalVentasParaMes(mes, anio);
            double resultadoNeto = totalIngresos - totalEgresos;

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
            parametros.put("totalIngresos", totalIngresos);
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
                AuditoriaUtil.registrar(
                        SessionManager.getUsuarioActual().getNombre(),
                        "REPORT",
                        "REPORTE_FINANCIERO",
                        null,
                        nombreArchivo
                );
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

    public static void actualizarIngresosEgresosMensuales() {
        try {
            LocalDate hoy = LocalDate.now();
            int mes = hoy.getMonthValue();
            int anio = hoy.getYear();
            double ingresos = DatabaseUtil.obtenerTotalPagosParaMes(mes, anio) +
                    DatabaseUtil.obtenerTotalVentasParaMes(mes, anio);
            double egresos = DatabaseUtil.obtenerTotalEgresosParaMes(mes, anio);
            DatabaseUtil.actualizarIngresosEgresosMensuales(mes, anio, ingresos, egresos);
        } catch (SQLException e) {
            System.err.println("❌ Error actualizando acumulados: " + e.getMessage());
        }
    }

    /**
     * Registra el saldo inicial de cada producto al inicio del nuevo mes.
     * Este movimiento sirve como punto de partida para el Kardex.
     */
    public static void cierreMensual(int mes, int anio) {
        try {
            List<Producto> productos = DatabaseUtil.getProductos();
            LocalDateTime fecha = LocalDate.of(anio, mes, 1)
                    .plusMonths(1)
                    .atStartOfDay();
            String usuario = SessionManager.getUsuarioActual() != null
                    ? SessionManager.getUsuarioActual().getNombre()
                    : "SISTEMA";

            for (Producto p : productos) {
                DatabaseUtil.insertMovimientoInventario(
                        p.getId(),
                        "SALDO_INICIAL",
                        p.getStock(),
                        "Saldo inicial",
                        usuario,
                        fecha,
                        p.getStock()
                );
            }

            AuditoriaUtil.registrar(
                    usuario,
                    "CIERRE_MENSUAL",
                    "INVENTARIO",
                    null,
                    String.format("Saldo inicial generado %02d/%d", mes, anio)
            );
        } catch (Exception e) {
            System.err.println("❌ Error durante el cierre mensual: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Variante que toma el mes y año actuales
    public static void cierreMensual() {
        LocalDate hoy = LocalDate.now();
        cierreMensual(hoy.getMonthValue(), hoy.getYear());
    }

    public static void generarReporteDiario(LocalDate fecha) {
        String base = String.format("reporte_dia_%02d", fecha.getDayOfMonth());
        generarReporte(fecha, fecha, base, "REPORTE_DIARIO");
        actualizarIngresosEgresosMensuales();
    }

    public static void generarReporteMensual(int anio, int mes) {
        LocalDate inicio = LocalDate.of(anio, mes, 1);
        LocalDate fin = inicio.withDayOfMonth(inicio.lengthOfMonth());
        generarReporte(inicio, fin, "reporte_mes", "REPORTE_MENSUAL");
    }

    public static void generarReporteAnual(int anio) {
        LocalDate inicio = LocalDate.of(anio, 1, 1);
        LocalDate fin = inicio.withMonth(12).withDayOfMonth(31);
        generarReporte(inicio, fin, "reporte_anual", "REPORTE_ANUAL");
    }

    private static void generarReporte(LocalDate inicio, LocalDate fin, String baseNombre, String accionAuditoria) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            String ini = inicio.toString();
            String fi = fin.toString();

            double pagos = 0.0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT IFNULL(SUM(monto),0) FROM pagos WHERE date(fecha_pago) BETWEEN ? AND ?")) {
                ps.setString(1, ini);
                ps.setString(2, fi);
                ResultSet rs = ps.executeQuery();
                pagos = rs.next() ? rs.getDouble(1) : 0.0;
            }

            double ventas = 0.0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT IFNULL(SUM(total),0) FROM ventas WHERE date(fecha) BETWEEN ? AND ?")) {
                ps.setString(1, ini);
                ps.setString(2, fi);
                ResultSet rs = ps.executeQuery();
                ventas = rs.next() ? rs.getDouble(1) : 0.0;
            }

            double egresos = 0.0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT IFNULL(SUM(monto),0) FROM egresos WHERE date(fecha) BETWEEN ? AND ?")) {
                ps.setString(1, ini);
                ps.setString(2, fi);
                ResultSet rs = ps.executeQuery();
                egresos = rs.next() ? rs.getDouble(1) : 0.0;
            }

            Map<String, Double> ventasCategoria = new HashMap<>();
            String sqlCat = "SELECT p.tipo, SUM(mi.cantidad * p.precio) AS total FROM movimientos_inventario mi " +
                    "JOIN productos p ON mi.producto_id = p.id WHERE mi.motivo = 'Venta' AND date(mi.fecha) BETWEEN ? AND ? GROUP BY p.tipo";
            try (PreparedStatement ps = conn.prepareStatement(sqlCat)) {
                ps.setString(1, ini);
                ps.setString(2, fi);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    ventasCategoria.put(rs.getString("tipo"), rs.getDouble("total"));
                }
            }

            List<String[]> ventasProducto = new ArrayList<>();
            String sqlProd = "SELECT p.nombre, p.tipo, SUM(mi.cantidad) AS unidades, SUM(mi.cantidad * p.precio) AS total " +
                    "FROM movimientos_inventario mi JOIN productos p ON mi.producto_id = p.id " +
                    "WHERE mi.motivo = 'Venta' AND date(mi.fecha) BETWEEN ? AND ? GROUP BY p.id";
            try (PreparedStatement ps = conn.prepareStatement(sqlProd)) {
                ps.setString(1, ini);
                ps.setString(2, fi);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    ventasProducto.add(new String[]{
                            rs.getString("nombre"),
                            rs.getString("tipo"),
                            String.valueOf(rs.getInt("unidades")),
                            String.valueOf(rs.getDouble("total"))
                    });
                }
            }

            double ingresos = pagos + ventas;
            double balance = ingresos - egresos;

            StringBuilder sb = new StringBuilder();
            sb.append("Ingresos,").append(ingresos).append('\n');
            sb.append("Egresos,").append(egresos).append('\n');
            sb.append("Balance,").append(balance).append('\n');
            sb.append('\n').append("Ventas por categoria\n");
            sb.append("Categoria,Total\n");
            for (Map.Entry<String, Double> e : ventasCategoria.entrySet()) {
                sb.append(e.getKey()).append(',').append(e.getValue()).append('\n');
            }
            sb.append('\n').append("Ventas por producto\n");
            sb.append("Producto,Categoria,Unidades,Total\n");
            for (String[] arr : ventasProducto) {
                sb.append(String.join(",", arr)).append('\n');
            }

            Path dir = Paths.get("reports", String.format("%04d", fin.getYear()), String.format("%02d", fin.getMonthValue()));
            Files.createDirectories(dir);
            Path csvPath = dir.resolve(baseNombre + ".csv");
            Files.writeString(csvPath, sb.toString(), StandardCharsets.UTF_8);
            Path pdfPath = dir.resolve(baseNombre + ".pdf");
            crearPdf(sb.toString(), pdfPath);

            AuditoriaUtil.registrar(SessionManager.getUsuarioActual().getNombre(), accionAuditoria, "REPORTE", null, pdfPath.toString());

        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static void crearPdf(String contenido, Path destino) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage pagina = new PDPage();
            doc.addPage(pagina);
            try (PDPageContentStream cs = new PDPageContentStream(doc, pagina)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.setLeading(14.5f);
                cs.newLineAtOffset(50, 700);
                for (String linea : contenido.split("\\n")) {
                    cs.showText(linea);
                    cs.newLine();
                }
                cs.endText();
            }
            doc.save(destino.toFile());
        }
    }
}
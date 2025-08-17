package util;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;
import models.Egreso;
import models.Producto;
import java.io.InputStream;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
}
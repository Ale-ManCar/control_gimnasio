package util;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;
import models.Egreso;
import java.io.InputStream;
import java.sql.Connection;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReporteUtil {
    public static void generarReporteFinanciero() {
        try {
            InputStream reporteStream = ReporteUtil.class.getResourceAsStream("/reports/reporte_financiero.jrxml");
            if (reporteStream == null) {
                System.err.println("❌ No se encontró el archivo reporte_financiero.jrxml");
                return;
            }

            double totalMembresias = DatabaseUtil.obtenerTotalPagosDelMesActual();
            double totalVentas = DatabaseUtil.obtenerTotalVentasDelMes();
            double totalEgresos = DatabaseUtil.obtenerTotalEgresosDelMes();
            double resultadoNeto = (totalMembresias + totalVentas) - totalEgresos;

            // Convertir fechas a formato String
            List<Egreso> egresos = DatabaseUtil.getEgresosDelMes().stream()
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

            JRDataSource dataSourceEgresos = new JRBeanCollectionDataSource(egresos);

            JasperReport jasperReport = JasperCompileManager.compileReport(reporteStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, dataSourceEgresos);

            if (jasperPrint.getPages() != null && !jasperPrint.getPages().isEmpty()) {
                JasperViewer.viewReport(jasperPrint, false);
                String pdfPath = System.getProperty("user.dir") + File.separator + "reporte_financiero.pdf";
                JasperExportManager.exportReportToPdfFile(jasperPrint, pdfPath);
                System.out.println("✅ Reporte financiero generado en: " + pdfPath);
            }

        } catch (Exception e) {
            System.err.println("❌ Error generando reporte financiero: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
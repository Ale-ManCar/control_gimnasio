package util;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.view.JasperViewer;

import java.io.InputStream;
import java.sql.Connection;
import java.io.File;

public class ReporteUtil {
    public static void generarReporteIngresos() {
        try {
            // Cargar archivo JRXML desde recursos
            InputStream reporteStream = ReporteUtil.class.getResourceAsStream("/reports/reporte_ingresos.jrxml");
            if (reporteStream == null) {
                throw new RuntimeException("No se encontró el archivo reporte_ingresos.jrxml en /reports/");
            }

            // Compilar JRXML
            JasperReport jasperReport = JasperCompileManager.compileReport(reporteStream);

            // Conexión a la base de datos
            Connection conn = DatabaseUtil.getConnection();

            // Llenar el reporte
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, null, conn);

            // Verificar si hay páginas (es decir, si hay datos)
            if (jasperPrint.getPages() == null || jasperPrint.getPages().isEmpty()) {
                System.out.println("⚠️ No hay datos disponibles para el reporte.");
            } else {
                // Mostrar vista previa
                JasperViewer.viewReport(jasperPrint, false);

                // Exportar a PDF
                String userDir = System.getProperty("user.dir");
                String pdfPath = userDir + File.separator + "src" + File.separator + "main" + File.separator + "resources" +
                        File.separator + "reports" + File.separator + "reporte_ingresos.pdf";

                JasperExportManager.exportReportToPdfFile(jasperPrint, pdfPath);
                System.out.println("✅ Reporte generado y guardado en: " + pdfPath);
            }

            conn.close();

        } catch (Exception e) {
            System.err.println("❌ Error al generar el reporte:");
            e.printStackTrace();
        }
    }
}
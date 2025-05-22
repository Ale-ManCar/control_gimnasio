package util;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.view.JasperViewer;

import java.io.InputStream;
import java.sql.Connection;
import java.io.File;
import java.io.IOException;

public class ReporteUtil {
    public static void generarReporteIngresos() {
        try {
            // Cargar archivo JRXML desde recursos (classpath)
            InputStream reporteStream = ReporteUtil.class.getResourceAsStream("/reports/reporte_ingresos.jrxml");
            if (reporteStream == null) {
                throw new RuntimeException("No se encontró el archivo reporte_ingresos.jrxml en /reports/");
            }

            // Compilar reporte a partir del InputStream
            JasperReport jasperReport = JasperCompileManager.compileReport(reporteStream);

            // Obtener conexión a base de datos (tu método)
            Connection conn = DatabaseUtil.getConnection();

            // Llenar reporte con datos
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, null, conn);

            // Mostrar reporte en ventana
            JasperViewer.viewReport(jasperPrint, false);

            // Definir ruta absoluta para guardar el PDF en la carpeta 'reports' dentro de recursos
            String userDir = System.getProperty("user.dir");
            String reportsDirPath = userDir + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "reports";

            // Crear la carpeta reports si no existe
            File reportsDir = new File(reportsDirPath);
            if (!reportsDir.exists()) {
                boolean created = reportsDir.mkdirs();
                if (!created) {
                    System.err.println("No se pudo crear la carpeta para reports en recursos");
                }
            }

            // Ruta completa para exportar el PDF
            String pdfPath = reportsDirPath + File.separator + "reporte_ingresos.pdf";

            // Exportar reporte a PDF en la carpeta correcta
            JasperExportManager.exportReportToPdfFile(jasperPrint, pdfPath);

            System.out.println("Reporte generado y exportado en: " + pdfPath);

            conn.close();

        } catch (Exception e) {
            System.err.println("❌ Error al generar el reporte:");
            e.printStackTrace();
        }
    }
}
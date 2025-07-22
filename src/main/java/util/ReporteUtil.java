package util;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.view.JasperViewer;
import java.io.InputStream;
import java.sql.Connection;
import java.io.File;

public class ReporteUtil {
    public static void generarReporteIngresos() {
        try {
            InputStream reporteStream = ReporteUtil.class.getResourceAsStream("/reports/reporte_ingresos.jrxml");
            if (reporteStream == null) {
                System.err.println("❌ No se encontró el archivo reporte_ingresos.jrxml");
                return;
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(reporteStream);
            Connection conn = DatabaseUtil.getConnection();
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, null, conn);

            if (jasperPrint.getPages() != null && !jasperPrint.getPages().isEmpty()) {
                JasperViewer.viewReport(jasperPrint, false);
                String pdfPath = System.getProperty("user.dir") + File.separator + "reporte_ingresos.pdf";
                JasperExportManager.exportReportToPdfFile(jasperPrint, pdfPath);
                System.out.println("✅ Reporte generado en: " + pdfPath);
            }

            conn.close();
        } catch (Exception e) {
            System.err.println("❌ Error generando reporte: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
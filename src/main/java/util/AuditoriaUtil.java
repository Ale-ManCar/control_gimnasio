package util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.Auditoria;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

public class AuditoriaUtil {

    public static void registrar(String usuario, String accion, String entidad, Integer idEntidad, String detalle) {
        String sql = "INSERT INTO auditoria (usuario, accion, entidad, entidad_id, detalle) VALUES (?,?,?,?,?)";
        try {
            DatabaseUtil.executeUpdate(sql, usuario, accion, entidad, idEntidad, detalle);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static ObservableList<Auditoria> buscar(LocalDate desde, LocalDate hasta, String usuario, String accion, String entidad) {
        ObservableList<Auditoria> lista = FXCollections.observableArrayList();
        StringBuilder sb = new StringBuilder("SELECT * FROM auditoria WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (desde != null) {
            sb.append(" AND fecha >= ?");
            params.add(desde.atStartOfDay().toString());
        }
        if (hasta != null) {
            sb.append(" AND fecha <= ?");
            params.add(hasta.plusDays(1).atStartOfDay().toString());
        }
        if (usuario != null && !usuario.isBlank()) {
            sb.append(" AND usuario LIKE ?");
            params.add('%' + usuario + '%');
        }
        if (accion != null && !accion.isBlank()) {
            sb.append(" AND UPPER(accion) = ?");
            params.add(accion.toUpperCase());
        }
        if (entidad != null && !entidad.isBlank()) {
            sb.append(" AND entidad LIKE ?");
            params.add('%' + entidad + '%');
        }
        sb.append(" ORDER BY fecha DESC");

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Auditoria a = new Auditoria();
                a.setId(rs.getInt("id"));
                a.setUsuario(rs.getString("usuario"));
                a.setFechaHora(rs.getTimestamp("fecha").toLocalDateTime());
                a.setAccion(rs.getString("accion"));
                a.setEntidad(rs.getString("entidad"));
                int idEnt = rs.getInt("entidad_id");
                if (!rs.wasNull()) {
                    a.setIdEntidad(idEnt);
                }
                a.setDetalle(rs.getString("detalle"));
                lista.add(a);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public static void exportarCSV(List<Auditoria> auditorias, File destino) throws IOException {
        try (FileWriter fw = new FileWriter(destino)) {
            fw.write("Usuario,FechaHora,Accion,Entidad,IdEntidad,Detalle\n");
            for (Auditoria a : auditorias) {
                fw.write(String.format("%s,%s,%s,%s,%s,%s\n",
                        a.getUsuario(),
                        a.getFechaHora(),
                        a.getAccion(),
                        a.getEntidad(),
                        a.getIdEntidad() == null ? "" : a.getIdEntidad(),
                        a.getDetalle().replace(',', ' ')));
            }
        }
    }

    public static void exportarPDF(List<Auditoria> auditorias, File destino) throws JRException {
        InputStream reporte = AuditoriaUtil.class.getResourceAsStream("/reports/auditoria.jrxml");
        if (reporte == null) {
            throw new JRException("No se encontró el reporte de auditoría");
        }
        JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(auditorias);
        JasperReport jr = JasperCompileManager.compileReport(reporte);
        JasperPrint jp = JasperFillManager.fillReport(jr, new HashMap<>(), ds);
        JasperExportManager.exportReportToPdfFile(jp, destino.getAbsolutePath());
    }
}
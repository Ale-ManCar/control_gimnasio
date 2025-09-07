package util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.Auditoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Utilidades para registrar y consultar acciones en la tabla de auditoría.
 */
public class AuditoriaUtil {
    private AuditoriaUtil() {}

    /**
     * Registra una acción realizada por un usuario.
     */
    public static void registrarAccion(int usuarioId, String accion, String detalle) {
        String sql = "INSERT INTO auditoria (usuario_id, accion, detalle, timestamp) VALUES (?, ?, ?, datetime('now'))";
        try {
            DatabaseUtil.executeUpdate(sql, usuarioId, accion, detalle);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Obtiene las acciones auditadas filtradas por los parámetros especificados.
     *
     * @param usuarioId   id del usuario (0 o null para todos)
     * @param fechaInicio fecha inicial del rango
     * @param fechaFin    fecha final del rango
     * @param tipo        tipo de acción
     * @return lista de registros de auditoría
     */
    public static ObservableList<Auditoria> filtrarAcciones(Integer usuarioId, LocalDate fechaInicio,
                                                            LocalDate fechaFin, String tipo) {
        ObservableList<Auditoria> registros = FXCollections.observableArrayList();
        StringBuilder sql = new StringBuilder(
                "SELECT a.id, COALESCE(u.username, '') AS usuario, a.accion, a.detalle, a.timestamp " +
                        "FROM auditoria a LEFT JOIN usuarios u ON a.usuario_id = u.id WHERE 1=1");

        if (usuarioId != null && usuarioId > 0) {
            sql.append(" AND a.usuario_id = ?");
        }
        if (fechaInicio != null) {
            sql.append(" AND date(a.timestamp) >= date(?)");
        }
        if (fechaFin != null) {
            sql.append(" AND date(a.timestamp) <= date(?)");
        }
        if (tipo != null && !tipo.isEmpty()) {
            sql.append(" AND a.accion = ?");
        }
        sql.append(" ORDER BY a.timestamp DESC");

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (usuarioId != null && usuarioId > 0) {
                stmt.setInt(idx++, usuarioId);
            }
            if (fechaInicio != null) {
                stmt.setString(idx++, fechaInicio.toString());
            }
            if (fechaFin != null) {
                stmt.setString(idx++, fechaFin.toString());
            }
            if (tipo != null && !tipo.isEmpty()) {
                stmt.setString(idx, tipo);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                registros.add(new Auditoria(
                        rs.getInt("id"),
                        rs.getString("usuario"),
                        rs.getString("accion"),
                        rs.getString("detalle"),
                        rs.getString("timestamp")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return registros;
    }
}
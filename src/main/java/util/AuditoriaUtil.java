package util;

import java.sql.SQLException;

public class AuditoriaUtil {
    private AuditoriaUtil() {}

    public static void registrarAccion(int usuarioId, String accion, String detalle) {
        String sql = "INSERT INTO auditoria (usuario_id, accion, detalle, timestamp) VALUES (?, ?, ?, datetime('now'))";
        try {
            DatabaseUtil.executeUpdate(sql, usuarioId, accion, detalle);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
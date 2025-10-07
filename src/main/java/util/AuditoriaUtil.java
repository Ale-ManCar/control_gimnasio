package util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.Auditoria;
import models.ResumenTipo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Utilidades para registrar y consultar acciones en la tabla de auditoría.
 */
public class AuditoriaUtil {
    private static final List<String> ACCIONES_RESUMEN = List.of(
            "RESUMEN_TURNO", "RESUMEN_SEMANAL", "RESUMEN_MENSUAL", "RESUMEN_ANUAL");

    private AuditoriaUtil() {}

    /**
     * Registra una acción realizada por un usuario.
     */
    public static void registrarAccion(int usuarioId, String accion, String detalle) {
        String sql = "INSERT INTO auditoria (usuario_id, accion, detalle, timestamp) VALUES (?, ?, ?, ?)";
        try {
            DatabaseUtil.executeUpdate(sql, usuarioId, accion, detalle, LocalDateTime.now());
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

    public static ObservableList<Auditoria> filtrarResumenes(Integer usuarioId, LocalDate fechaInicio,
                                                             LocalDate fechaFin, ResumenTipo tipo) {
        ObservableList<Auditoria> registros = FXCollections.observableArrayList();
        List<String> acciones = obtenerAccionesParaFiltro(tipo);
        if (acciones.isEmpty()) {
            return registros;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT a.id, COALESCE(u.username, '') AS usuario, a.accion, a.detalle, a.timestamp " +
                        "FROM auditoria a LEFT JOIN usuarios u ON a.usuario_id = u.id WHERE a.accion IN (");
        sql.append(String.join(",", acciones.stream().map(a -> "?").toList())).append(")");

        if (usuarioId != null && usuarioId > 0) {
            sql.append(" AND a.usuario_id = ?");
        }
        if (fechaInicio != null) {
            sql.append(" AND date(a.timestamp) >= date(?)");
        }
        if (fechaFin != null) {
            sql.append(" AND date(a.timestamp) <= date(?)");
        }
        sql.append(" ORDER BY a.timestamp DESC");

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            for (String accion : acciones) {
                stmt.setString(idx++, accion);
            }
            if (usuarioId != null && usuarioId > 0) {
                stmt.setInt(idx++, usuarioId);
            }
            if (fechaInicio != null) {
                stmt.setString(idx++, fechaInicio.toString());
            }
            if (fechaFin != null) {
                stmt.setString(idx++, fechaFin.toString());
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

    public static ObservableList<Integer> listarAniosResumenes(Integer usuarioId) {
        ObservableList<Integer> anios = FXCollections.observableArrayList();
        StringBuilder sql = new StringBuilder("SELECT DISTINCT strftime('%Y', timestamp) AS anio FROM auditoria " +
                "WHERE accion IN (");
        sql.append(String.join(",", ACCIONES_RESUMEN.stream().map(a -> "?").toList())).append(")");
        if (usuarioId != null && usuarioId > 0) {
            sql.append(" AND usuario_id = ?");
        }
        sql.append(" ORDER BY anio DESC");

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            for (String accion : ACCIONES_RESUMEN) {
                stmt.setString(idx++, accion);
            }
            if (usuarioId != null && usuarioId > 0) {
                stmt.setInt(idx++, usuarioId);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String anio = rs.getString("anio");
                if (anio != null && !anio.isBlank()) {
                    try {
                        anios.add(Integer.parseInt(anio));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return anios;
    }

    public static ObservableList<Month> listarMesesResumenes(Integer usuarioId, int anio) {
        ObservableList<Month> meses = FXCollections.observableArrayList();
        StringBuilder sql = new StringBuilder("SELECT DISTINCT strftime('%m', timestamp) AS mes FROM auditoria WHERE accion IN (");
        sql.append(String.join(",", ACCIONES_RESUMEN.stream().map(a -> "?").toList())).append(")");
        sql.append(" AND strftime('%Y', timestamp) = ?");
        if (usuarioId != null && usuarioId > 0) {
            sql.append(" AND usuario_id = ?");
        }
        sql.append(" ORDER BY mes DESC");

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            for (String accion : ACCIONES_RESUMEN) {
                stmt.setString(idx++, accion);
            }
            stmt.setString(idx++, String.format("%04d", anio));
            if (usuarioId != null && usuarioId > 0) {
                stmt.setInt(idx++, usuarioId);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String mes = rs.getString("mes");
                if (mes != null && !mes.isBlank()) {
                    try {
                        meses.add(Month.of(Integer.parseInt(mes)));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        meses.sort(Comparator.reverseOrder());
        return meses;
    }

    private static List<String> obtenerAccionesParaFiltro(ResumenTipo tipo) {
        if (tipo == null || tipo == ResumenTipo.TODOS) {
            return ACCIONES_RESUMEN;
        }
        if (tipo.getAccion() == null || tipo.getAccion().isBlank()) {
            return new ArrayList<>();
        }
        return List.of(tipo.getAccion());
    }
}
package util;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.PagoMensual;

public class DatabaseUtil {
    private static final String URL = "jdbc:sqlite:database/gimnasio.db";
    private static final int BUSY_TIMEOUT_MS = 60000; // 60 segundos de timeout

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA synchronous = NORMAL");
            stmt.execute("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MS);
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    public static synchronized void initDatabase() {
        String sqlClientes = "CREATE TABLE IF NOT EXISTS clientes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombres TEXT NOT NULL," +
                "apellidos TEXT NOT NULL," +
                "telefono TEXT NOT NULL UNIQUE," +
                "tipoMembresia TEXT NOT NULL," +
                "fechaInicio TEXT NOT NULL," +
                "fechaVencimiento TEXT NOT NULL," +
                "activo BOOLEAN DEFAULT TRUE)";

        String sqlAlertas = "CREATE TABLE IF NOT EXISTS alertas_enviadas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "telefono_cliente TEXT NOT NULL," +
                "fecha_envio TEXT NOT NULL," +
                "tipo_alerta TEXT NOT NULL," +
                "FOREIGN KEY (telefono_cliente) REFERENCES clientes(telefono))";

        String sqlConfig = "CREATE TABLE IF NOT EXISTS config (" +
                "id INTEGER PRIMARY KEY," +
                "nombre_gimnasio TEXT DEFAULT 'Mi Gimnasio'," +
                "mensaje_whatsapp TEXT DEFAULT '¡Hola [NOMBRE]! Tu membresía en [GIMNASIO] vence en [DIAS] días')";

        String sqlPagos = "CREATE TABLE IF NOT EXISTS pagos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "cliente_id INTEGER NOT NULL," +
                "fecha_pago TEXT NOT NULL," +
                "fecha_vencimiento TEXT," +
                "monto REAL NOT NULL," +
                "FOREIGN KEY (cliente_id) REFERENCES clientes(id))";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false);
            stmt.execute(sqlClientes);
            stmt.execute(sqlAlertas);
            stmt.execute(sqlConfig);
            stmt.execute(sqlPagos);
            stmt.execute("INSERT OR IGNORE INTO config (id) VALUES (1)");
            conn.commit();

            System.out.println("Base de datos inicializada correctamente");
        } catch (SQLException e) {
            System.err.println("Error al inicializar la base de datos:");
            e.printStackTrace();
        }
    }

    public static int executeUpdate(String sql, Object... params) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            stmt = conn.prepareStatement(sql);

            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof java.time.LocalDate) {
                    stmt.setString(i + 1, params[i].toString());
                } else {
                    stmt.setObject(i + 1, params[i]);
                }
            }

            int result = stmt.executeUpdate();
            conn.commit();
            return result;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error al hacer rollback: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    public static void testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("✅ Conexión exitosa a: " + URL);
        } catch (SQLException e) {
            System.err.println("❌ Error de conexión: " + e.getMessage());
        }
    }

    public static Map<String, Integer> getEstadisticas() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String sql = """
        SELECT 
            (SELECT COUNT(*) FROM clientes WHERE activo = 1) AS clientes_activos,
            (SELECT COUNT(*) FROM pagos WHERE date(fecha_pago) = CURRENT_DATE) AS pagos_hoy,
            (SELECT COUNT(*) FROM clientes WHERE fechaVencimiento BETWEEN CURRENT_DATE AND date('now', '+7 days')) AS por_vencer
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                stats.put("clientes_activos", rs.getInt(1));
                stats.put("pagos_hoy", rs.getInt(2));
                stats.put("por_vencer", rs.getInt(3));
            }
        }
        return stats;
    }

    public static ObservableList<PagoMensual> getIngresosMensuales() throws SQLException {
        ObservableList<PagoMensual> data = FXCollections.observableArrayList();
        String sql = """
        SELECT strftime('%Y-%m', fecha_pago) AS mes, 
               SUM(monto) AS total 
        FROM pagos 
        GROUP BY mes 
        ORDER BY mes DESC 
        LIMIT 6
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String mes = rs.getString("mes");
                double total = rs.getDouble("total");
                data.add(new PagoMensual(mes, total));
            }
        }
        return data;
    }
}
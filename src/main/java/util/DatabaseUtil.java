package util;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import models.Auditoria;
import models.AuditoriaUsuario;
import models.Egreso;
import models.EgresoDetalle;
import models.PagoDetalle;
import models.PagoMensual;
import models.Pago;
import models.Producto;
import models.InventarioMovimiento;
import models.User;
import models.Role;
import models.Turno;
import models.Proveedor;
import models.CoachClientes;

public class DatabaseUtil {
    private static final String URL = "jdbc:sqlite:database/gimnasio.db";
    private static final int BUSY_TIMEOUT_MS = 60000;
    private static final DateTimeFormatter SQLITE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern PAGO_ID_PATTERN = Pattern.compile("Pago\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

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
                "fecha_vencimiento TEXT NOT NULL," +
                "monto_pago REAL NOT NULL," +
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
                "mensaje_whatsapp TEXT DEFAULT '¡Hola *[NOMBRE] [APELLIDO]*! Tu membresía en *[GIMNASIO]* vence en *[DIAS]* días'," +
                "mensaje_registro TEXT DEFAULT '¡Bienvenido *[NOMBRE] [APELLIDO]* a *[GIMNASIO]*! Tu membresía de *[MEMBRESIA]* es válida hasta *[FECHA].* ¡Gracias por unirte!'," +
                "mensaje_renovacion TEXT DEFAULT '¡Hola *[NOMBRE] [APELLIDO]!* Tu membresía en *[GIMNASIO*] ha sido renovada por *[MEMBRESIA]*. Nueva fecha de vencimiento: *[FECHA].* ¡Disfruta de nuestros servicios!')";

        String sqlPagos = "CREATE TABLE IF NOT EXISTS pagos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "cliente_id INTEGER NOT NULL," +
                "fecha_pago TEXT NOT NULL," +
                "fecha_vencimiento TEXT," +
                "tipo_membresia TEXT," +
                "monto REAL NOT NULL," +
                "estado TEXT NOT NULL DEFAULT 'ACTIVO'," +
                "FOREIGN KEY (cliente_id) REFERENCES clientes(id))";

        String sqlCoaches = "CREATE TABLE IF NOT EXISTS coaches (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombres TEXT NOT NULL," +
                "apellidos TEXT NOT NULL," +
                "telefono TEXT," +
                "area TEXT NOT NULL," +
                "foto_path TEXT)";

        String sqlUsuarios = "CREATE TABLE IF NOT EXISTS usuarios (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT NOT NULL UNIQUE," +
                "password TEXT NOT NULL," +
                "rol TEXT NOT NULL," +
                "last_login TEXT," +
                "acciones_realizadas INTEGER DEFAULT 0)";

        String sqlProductos = "CREATE TABLE IF NOT EXISTS productos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT NOT NULL," +
                "stock INTEGER NOT NULL," +
                "stock_inicial INTEGER DEFAULT 0," +
                "umbral INTEGER DEFAULT 0," +
                "precio REAL NOT NULL," +
                "tipo TEXT NOT NULL," +
                "precio_compra REAL NOT NULL," +
                "unidades_por_paca INTEGER," +
                "peso_total REAL," +
                "peso_por_scoop REAL)";

        String sqlInventarioHistorial = "CREATE TABLE IF NOT EXISTS inventario_historial (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "producto_id INTEGER NOT NULL," +
                "tipo TEXT NOT NULL," +
                "cantidad INTEGER NOT NULL," +
                "fecha TEXT NOT NULL DEFAULT (datetime('now'))," +
                "FOREIGN KEY (producto_id) REFERENCES productos(id))";

        String sqlVentas = "CREATE TABLE IF NOT EXISTS ventas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "fecha TEXT NOT NULL DEFAULT (date('now'))," +
                "total REAL NOT NULL)";

        String sqlEgresos = "CREATE TABLE IF NOT EXISTS egresos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "descripcion TEXT NOT NULL," +
                "monto REAL NOT NULL," +
                "fecha TEXT NOT NULL," +
                "categoria TEXT NOT NULL)";

        String sqlTurnos = "CREATE TABLE IF NOT EXISTS turnos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "usuario_id INTEGER NOT NULL," +
                "fecha_inicio TEXT NOT NULL," +
                "fecha_fin TEXT," +
                "stock_inicial TEXT," +
                "stock_final TEXT," +
                "ingresos_ventas REAL DEFAULT 0," +
                "ingresos_clientes REAL DEFAULT 0," +
                "resumen_generado TEXT," +
                "FOREIGN KEY (usuario_id) REFERENCES usuarios(id))";

        String sqlAuditoria = "CREATE TABLE IF NOT EXISTS auditoria (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "usuario_id INTEGER," +
                "accion TEXT NOT NULL," +
                "detalle TEXT," +
                "timestamp TEXT DEFAULT (datetime('now'))," +
                "FOREIGN KEY (usuario_id) REFERENCES usuarios(id))";

        String sqlAuditoriaUsuarios = "CREATE TABLE IF NOT EXISTS auditoria_usuarios (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "usuario_id INTEGER," +
                "accion TEXT NOT NULL," +
                "fecha TEXT DEFAULT (datetime('now'))," +
                "FOREIGN KEY (usuario_id) REFERENCES usuarios(id))";

        String sqlProveedores = "CREATE TABLE IF NOT EXISTS proveedores (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT NOT NULL UNIQUE," +
                "contacto TEXT," +
                "telefono TEXT)";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false);
            stmt.execute(sqlClientes);
            stmt.execute(sqlAlertas);
            stmt.execute(sqlConfig);
            stmt.execute(sqlPagos);
            try { stmt.execute("ALTER TABLE pagos ADD COLUMN estado TEXT NOT NULL DEFAULT 'ACTIVO'"); } catch (SQLException ignored) {}
            try { stmt.execute("UPDATE pagos SET estado = 'ACTIVO' WHERE estado IS NULL"); } catch (SQLException ignored) {}
            stmt.execute(sqlProductos);
            stmt.execute(sqlInventarioHistorial);
            stmt.execute(sqlVentas);
            stmt.execute(sqlEgresos);
            stmt.execute(sqlCoaches);
            stmt.execute(sqlUsuarios);
            stmt.execute(sqlTurnos);
            try { stmt.execute("ALTER TABLE turnos ADD COLUMN resumen_generado TEXT"); } catch (SQLException ignored) {}
            stmt.execute(sqlAuditoria);
            stmt.execute(sqlAuditoriaUsuarios);
            stmt.execute(sqlProveedores);
            try { stmt.execute("ALTER TABLE clientes ADD COLUMN area TEXT"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE clientes ADD COLUMN coach_id INTEGER REFERENCES coaches(id)"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE productos ADD COLUMN stock_inicial INTEGER DEFAULT 0"); } catch (SQLException ignored) {}
            try { stmt.execute("UPDATE productos SET stock_inicial = stock_objetivo WHERE stock_inicial = 0 AND stock_objetivo IS NOT NULL"); } catch (SQLException ignored) {}
            try { stmt.execute("UPDATE productos SET stock_inicial = stock WHERE stock_inicial = 0"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE productos ADD COLUMN umbral INTEGER DEFAULT 0"); } catch (SQLException ignored) {}
            stmt.execute("INSERT OR IGNORE INTO config (id) VALUES (1)");
            try { stmt.execute("ALTER TABLE usuarios ADD COLUMN last_login TEXT"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE usuarios ADD COLUMN acciones_realizadas INTEGER DEFAULT 0"); } catch (SQLException ignored) {}
            stmt.execute("INSERT OR IGNORE INTO proveedores (id, nombre, contacto, telefono) VALUES (1, 'Proveedor 1', '', ''), (2, 'Proveedor 2', '', '')");
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

    public static double obtenerTotalPagosParaMes(int mes, int anio) {
        double total = 0.0;
        String sql = "SELECT SUM(monto) AS total FROM pagos " +
                "WHERE strftime('%Y', fecha_pago) = ? " +
                "AND strftime('%m', fecha_pago) = ? " +
                "AND estado = 'ACTIVO'";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, String.format("%04d", anio));
            pstmt.setString(2, String.format("%02d", mes));
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                total = rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }

    public static double obtenerTotalVentasParaMes(int mes, int anio) {
        double total = 0.0;
        String sql = "SELECT SUM(total) AS total FROM ventas " +
                "WHERE strftime('%Y', fecha) = ? " +
                "AND strftime('%m', fecha) = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, String.format("%04d", anio));
            pstmt.setString(2, String.format("%02d", mes));
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                total = rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }

    public static double obtenerTotalEgresosParaMes(int mes, int anio) {
        double total = 0.0;
        String sql = "SELECT SUM(monto) AS total FROM egresos " +
                "WHERE strftime('%Y', fecha) = ? " +
                "AND strftime('%m', fecha) = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, String.format("%04d", anio));
            pstmt.setString(2, String.format("%02d", mes));
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                total = rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }

    public static ObservableList<Egreso> getEgresosParaMes(int mes, int anio) {
        ObservableList<Egreso> egresos = FXCollections.observableArrayList();
        String sql = "SELECT id, descripcion, monto, fecha, categoria FROM egresos " +
                "WHERE strftime('%Y', fecha) = ? " +
                "AND strftime('%m', fecha) = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.format("%04d", anio));
            stmt.setString(2, String.format("%02d", mes));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Egreso e = new Egreso();
                e.setId(rs.getInt("id"));
                e.setDescripcion(rs.getString("descripcion"));
                e.setMonto(rs.getDouble("monto"));
                e.setFecha(LocalDate.parse(rs.getString("fecha")));
                e.setCategoria(rs.getString("categoria"));
                egresos.add(e);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return egresos;
    }

    public static double obtenerTotalPagosDelMesActual() {
        LocalDate hoy = LocalDate.now();
        return obtenerTotalPagosParaMes(hoy.getMonthValue(), hoy.getYear());
    }

    public static double obtenerTotalVentasDelMes() {
        LocalDate hoy = LocalDate.now();
        return obtenerTotalVentasParaMes(hoy.getMonthValue(), hoy.getYear());
    }

    public static double obtenerTotalEgresosDelMes() {
        LocalDate hoy = LocalDate.now();
        return obtenerTotalEgresosParaMes(hoy.getMonthValue(), hoy.getYear());
    }

    public static ObservableList<Egreso> getEgresosDelMes() {
        LocalDate hoy = LocalDate.now();
        return getEgresosParaMes(hoy.getMonthValue(), hoy.getYear());
    }

    public static void registrarEgreso(Egreso egreso) throws SQLException {
        String sql = "INSERT INTO egresos (descripcion, monto, fecha, categoria) VALUES (?, ?, ?, ?)";
        executeUpdate(sql,
                egreso.getDescripcion(),
                egreso.getMonto(),
                egreso.getFecha(),
                egreso.getCategoria());

        EventBus.fireEvent(EventBus.EventType.EGRESO_REGISTRADO);
    }

    /**
     * @deprecated Use {@link #registrarEgreso(Egreso)} instead
     */
    @Deprecated
    public static void insertarEgreso(Egreso egreso) throws SQLException {
        registrarEgreso(egreso);
    }

    public static ObservableList<Egreso> listarEgresos() throws SQLException {
        ObservableList<Egreso> egresos = FXCollections.observableArrayList();
        String sql = "SELECT id, descripcion, monto, fecha, categoria FROM egresos ORDER BY fecha DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Egreso e = new Egreso();
                e.setId(rs.getInt("id"));
                e.setDescripcion(rs.getString("descripcion"));
                e.setMonto(rs.getDouble("monto"));
                e.setFecha(LocalDate.parse(rs.getString("fecha")));
                e.setCategoria(rs.getString("categoria"));
                egresos.add(e);
            }
        }
        return egresos;
    }

    public static ObservableList<Egreso> filtrarEgresos(LocalDate fechaInicio, LocalDate fechaFin, String categoria) throws SQLException {
        ObservableList<Egreso> egresos = FXCollections.observableArrayList();
        StringBuilder sql = new StringBuilder("SELECT id, descripcion, monto, fecha, categoria FROM egresos WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (fechaInicio != null && fechaFin != null) {
            sql.append(" AND date(fecha) BETWEEN ? AND ?");
            params.add(fechaInicio.toString());
            params.add(fechaFin.toString());
        } else if (fechaInicio != null) {
            sql.append(" AND date(fecha) >= ?");
            params.add(fechaInicio.toString());
        } else if (fechaFin != null) {
            sql.append(" AND date(fecha) <= ?");
            params.add(fechaFin.toString());
        }

        if (categoria != null && !categoria.isEmpty() && !"TODOS".equalsIgnoreCase(categoria)) {
            sql.append(" AND categoria = ?");
            params.add(categoria);
        }

        sql.append(" ORDER BY fecha DESC");

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Egreso e = new Egreso();
                    e.setId(rs.getInt("id"));
                    e.setDescripcion(rs.getString("descripcion"));
                    e.setMonto(rs.getDouble("monto"));
                    e.setFecha(LocalDate.parse(rs.getString("fecha")));
                    e.setCategoria(rs.getString("categoria"));
                    egresos.add(e);
                }
            }
        }
        return egresos;
    }

    public static Map<String, Double> obtenerIngresosVsEgresos(LocalDate fechaInicio, LocalDate fechaFin, Integer clienteId, String tipoMembresia) throws SQLException {
        Map<String, Double> resultado = new HashMap<>();

        // Ingresos por membresías
        StringBuilder sqlPagos = new StringBuilder("SELECT COALESCE(SUM(monto),0) FROM pagos WHERE date(fecha_pago) BETWEEN ? AND ? AND estado = 'ACTIVO'");
        List<Object> paramsPagos = new ArrayList<>();
        paramsPagos.add(fechaInicio.toString());
        paramsPagos.add(fechaFin.toString());
        if (clienteId != null) {
            sqlPagos.append(" AND cliente_id = ?");
            paramsPagos.add(clienteId);
        }
        if (tipoMembresia != null && !tipoMembresia.isEmpty()) {
            sqlPagos.append(" AND tipo_membresia = ?");
            paramsPagos.add(tipoMembresia);
        }

        double totalPagos = 0.0;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlPagos.toString())) {
            for (int i = 0; i < paramsPagos.size(); i++) {
                stmt.setObject(i + 1, paramsPagos.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalPagos = rs.getDouble(1);
                }
            }
        }

        // Ingresos por ventas
        double totalVentas = 0.0;
        String sqlVentas = "SELECT COALESCE(SUM(total),0) FROM ventas WHERE date(fecha) BETWEEN ? AND ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlVentas)) {
            stmt.setString(1, fechaInicio.toString());
            stmt.setString(2, fechaFin.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalVentas = rs.getDouble(1);
                }
            }
        }

        // Egresos
        double totalEgresos = 0.0;
        String sqlEgresos = "SELECT COALESCE(SUM(monto),0) FROM egresos WHERE date(fecha) BETWEEN ? AND ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlEgresos)) {
            stmt.setString(1, fechaInicio.toString());
            stmt.setString(2, fechaFin.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalEgresos = rs.getDouble(1);
                }
            }
        }

        resultado.put("membresias", totalPagos);
        resultado.put("ventas", totalVentas);
        resultado.put("egresos", totalEgresos);
        return resultado;
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
            (SELECT COUNT(*) FROM pagos WHERE date(fecha_pago) = CURRENT_DATE AND estado = 'ACTIVO') AS pagos_hoy,
            (SELECT COUNT(*) FROM clientes WHERE fecha_vencimiento BETWEEN CURRENT_DATE AND date('now', '+7 days')) AS por_vencer
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

    public static Map<String, Integer> getAdminStats() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String sql = """
        SELECT
            (SELECT COUNT(*) FROM clientes WHERE activo = 1) AS clientes_activos,
            (SELECT COUNT(*) FROM clientes WHERE activo = 0) AS clientes_inactivos,
            (SELECT COUNT(*) FROM pagos WHERE strftime('%Y-%m', fecha_pago) = strftime('%Y-%m','now') AND estado = 'ACTIVO') AS membresias_mes,
            (SELECT COUNT(*) FROM clientes WHERE fecha_vencimiento BETWEEN date('now') AND date('now', '+7 day')) AS por_vencer,
            (SELECT COUNT(*) FROM clientes WHERE fecha_vencimiento < date('now') AND activo = 1) AS clientes_morosos,
            (SELECT COALESCE(MAX(cantidad),0) FROM (SELECT COUNT(*) AS cantidad FROM clientes WHERE coach_id IS NOT NULL GROUP BY coach_id)) AS coaches_top,
            (SELECT COUNT(*) FROM clientes WHERE activo = 1 AND date(fecha_inicio) <= date('now') AND date(fecha_vencimiento) >= date('now')) AS activos_hoy
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                stats.put("clientes_activos", rs.getInt("clientes_activos"));
                stats.put("clientes_inactivos", rs.getInt("clientes_inactivos"));
                stats.put("membresias_mes", rs.getInt("membresias_mes"));
                stats.put("por_vencer", rs.getInt("por_vencer"));
                stats.put("clientes_morosos", rs.getInt("clientes_morosos"));
                stats.put("coaches_top", rs.getInt("coaches_top"));
                stats.put("activos_hoy", rs.getInt("activos_hoy"));
            }
        }
        return stats;
    }

    public static ObservableList<PagoMensual> getIngresosMensuales(int año) throws SQLException {
        ObservableList<PagoMensual> data = FXCollections.observableArrayList();

        String sql = "SELECT mes, SUM(total) AS total FROM ("
                + "SELECT strftime('%Y-%m', fecha_pago) AS mes, monto AS total FROM pagos WHERE estado = 'ACTIVO' "
                + "UNION ALL "
                + "SELECT strftime('%Y-%m', fecha) AS mes, total FROM ventas"
                + ") "
                + "WHERE substr(mes, 1, 4) = ? "
                + "GROUP BY mes "
                + "ORDER BY mes";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(año));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String mes = rs.getString("mes");
                double total = rs.getDouble("total");
                data.add(new PagoMensual(mes, total));
            }
        }
        return data;
    }

    public static ObservableList<PieChart.Data> getDistribucionMembresias(int año) throws SQLException {
        ObservableList<PieChart.Data> datos = FXCollections.observableArrayList();
        String sql = "SELECT tipo_membresia, SUM(monto) AS total "
                + "FROM pagos "
                + "WHERE strftime('%Y', fecha_pago) = ? "
                + "AND tipo_membresia IS NOT NULL "
                + "AND estado = 'ACTIVO' "
                + "GROUP BY tipo_membresia";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(año));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                datos.add(new PieChart.Data(
                        rs.getString("tipo_membresia"),
                        rs.getDouble("total")
                ));
            }
        }
        return datos;
    }

    public static ObservableList<PagoDetalle> buscarPagos(Integer clienteId, LocalDate fechaInicio, LocalDate fechaFin, String tipoMembresia) throws SQLException {
        ObservableList<PagoDetalle> pagos = FXCollections.observableArrayList();
        StringBuilder sql = new StringBuilder("SELECT pagos.fecha_pago AS fecha, "
                + "clientes.nombres || ' ' || clientes.apellidos AS cliente, "
                + "clientes.id AS cliente_id, "
                + "pagos.tipo_membresia AS membresia, pagos.monto "
                + "FROM pagos JOIN clientes ON pagos.cliente_id = clientes.id WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (clienteId != null) {
            sql.append(" AND clientes.id = ?");
            params.add(clienteId);
        }

        if (fechaInicio != null && fechaFin != null) {
            sql.append(" AND date(pagos.fecha_pago) BETWEEN ? AND ?");
            params.add(fechaInicio.toString());
            params.add(fechaFin.toString());
        } else if (fechaInicio != null) {
            sql.append(" AND date(pagos.fecha_pago) >= ?");
            params.add(fechaInicio.toString());
        } else if (fechaFin != null) {
            sql.append(" AND date(pagos.fecha_pago) <= ?");
            params.add(fechaFin.toString());
        }

        if (tipoMembresia != null && !tipoMembresia.isEmpty()) {
            sql.append(" AND pagos.tipo_membresia = ?");
            params.add(tipoMembresia);
        }

        sql.append(" AND pagos.estado = 'ACTIVO'");
        sql.append(" ORDER BY pagos.fecha_pago DESC");

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    pagos.add(new PagoDetalle(
                            LocalDate.parse(rs.getString("fecha")),
                            rs.getString("cliente"),
                            rs.getInt("cliente_id"),
                            rs.getString("membresia"),
                            rs.getDouble("monto")
                    ));
                }
            }
        }
        return pagos;
    }

    public static ObservableList<Pago> listarPagosActivos() throws SQLException {
        ObservableList<Pago> pagos = FXCollections.observableArrayList();
        String sql = "SELECT p.id, p.cliente_id, p.fecha_pago, p.fecha_vencimiento, p.tipo_membresia, p.monto, p.estado, "
                + "COALESCE(c.nombres, '') AS nombres, COALESCE(c.apellidos, '') AS apellidos "
                + "FROM pagos p LEFT JOIN clientes c ON p.cliente_id = c.id "
                + "WHERE p.estado = 'ACTIVO' "
                + "ORDER BY datetime(p.fecha_pago) DESC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Pago pago = new Pago();
                pago.setId(rs.getInt("id"));
                pago.setClienteId(rs.getInt("cliente_id"));
                pago.setFechaPago(parseFecha(rs.getString("fecha_pago")));
                pago.setFechaVencimiento(parseFecha(rs.getString("fecha_vencimiento")));
                pago.setTipoMembresia(rs.getString("tipo_membresia"));
                pago.setMonto(rs.getDouble("monto"));
                pago.setEstado(rs.getString("estado"));
                String nombres = rs.getString("nombres");
                String apellidos = rs.getString("apellidos");
                String nombreCompleto = (nombres + " " + apellidos).trim();
                pago.setClienteNombre(nombreCompleto.isEmpty() ? "" : nombreCompleto);
                pagos.add(pago);
            }
        }
        return pagos;
    }

    public static List<PagoDetalle> getDetallesPagos(int año) throws SQLException {
        List<PagoDetalle> detalles = new ArrayList<>();
        String sql = "SELECT pagos.fecha_pago AS fecha, "
                + "clientes.nombres || ' ' || clientes.apellidos AS cliente, "
                + "clientes.id AS cliente_id, "
                + "pagos.tipo_membresia AS membresia, pagos.monto "
                + "FROM pagos pagos "
                + "JOIN clientes clientes ON pagos.cliente_id = clientes.id "
                + "WHERE strftime('%Y', pagos.fecha_pago) = ? "
                + "AND pagos.estado = 'ACTIVO'";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(año));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                detalles.add(new PagoDetalle(
                        LocalDate.parse(rs.getString("fecha")),
                        rs.getString("cliente"),
                        rs.getInt("cliente_id"),
                        rs.getString("membresia"),
                        rs.getDouble("monto")
                ));
            }
        }
        return detalles;
    }

    public static String obtenerTipoMembresiaActual(int clienteId) {
        String sql = "SELECT tipoMembresia FROM clientes WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, clienteId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("tipoMembresia");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Desconocido";
    }

    public static void insertarProducto(Producto producto) throws SQLException {
        String sql = "INSERT INTO productos (nombre, stock, stock_inicial, umbral, precio, tipo, precio_compra, unidades_por_paca, peso_total, peso_por_scoop) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        executeUpdate(sql,
                producto.getNombre(),
                producto.getStock(),
                producto.getStockInicial(),
                producto.getUmbral(),
                producto.getPrecio(),
                producto.getTipo(),
                producto.getPrecioCompra(),
                producto.getUnidadesPorPaca(),
                producto.getPesoTotal(),
                producto.getPesoScoop()
        );
    }

    public static ObservableList<Producto> getProductos() throws SQLException {
        ObservableList<Producto> productos = FXCollections.observableArrayList();
        String sql = "SELECT id, nombre, stock, stock_inicial, umbral, precio, tipo, precio_compra, unidades_por_paca, peso_total, peso_por_scoop FROM productos";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Producto p = new Producto();
                p.setId(rs.getInt("id"));
                p.setNombre(rs.getString("nombre").toUpperCase(Locale.ROOT));
                p.setStock(rs.getInt("stock"));
                p.setStockInicial(rs.getInt("stock_inicial"));
                p.setUmbral(rs.getInt("umbral"));
                p.setPrecio(rs.getDouble("precio"));
                p.setTipo(rs.getString("tipo"));
                p.setPrecioCompra(rs.getDouble("precio_compra"));
                p.setUnidadesPorPaca(rs.getInt("unidades_por_paca"));
                p.setPesoTotal(rs.getDouble("peso_total"));
                p.setPesoScoop(rs.getDouble("peso_por_scoop"));
                productos.add(p);
            }
        }
        return productos;
    }

    public static void actualizarStockProducto(int id, int cantidadVendida) throws SQLException {
        String sql = "UPDATE productos SET stock = stock - ? WHERE id = ?";
        executeUpdate(sql, cantidadVendida, id);
    }

    public static void actualizarProducto(int id, double nuevoPrecio, int nuevoUmbral) throws SQLException {
        String sql = "UPDATE productos SET precio = ?, umbral = ? WHERE id = ?";
        executeUpdate(sql, nuevoPrecio, nuevoUmbral, id);
    }

    public static void registrarEntradaProducto(int productoId, int cantidad) throws SQLException {
        String updateSql = "UPDATE productos SET stock = stock + ? WHERE id = ?";
        String insertSql = "INSERT INTO inventario_historial (producto_id, tipo, cantidad, fecha) VALUES (?, 'ENTRADA', ?, datetime('now'))";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql);
                 PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                psUpdate.setInt(1, cantidad);
                psUpdate.setInt(2, productoId);
                psUpdate.executeUpdate();

                psInsert.setInt(1, productoId);
                psInsert.setInt(2, cantidad);
                psInsert.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public static void registrarSalidaProducto(int productoId, int cantidad) throws SQLException {
        String updateSql = "UPDATE productos SET stock = stock - ? WHERE id = ?";
        String insertSql = "INSERT INTO inventario_historial (producto_id, tipo, cantidad, fecha) VALUES (?, 'SALIDA', ?, datetime('now'))";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql);
                 PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                psUpdate.setInt(1, cantidad);
                psUpdate.setInt(2, productoId);
                psUpdate.executeUpdate();

                psInsert.setInt(1, productoId);
                psInsert.setInt(2, cantidad);
                psInsert.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public static ObservableList<InventarioMovimiento> obtenerHistorialInventario() throws SQLException {
        ObservableList<InventarioMovimiento> historial = FXCollections.observableArrayList();
        String sql = "SELECT h.id, p.nombre AS producto, h.tipo, h.cantidad, h.fecha FROM inventario_historial h " +
                "JOIN productos p ON h.producto_id = p.id ORDER BY h.fecha DESC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                InventarioMovimiento mov = new InventarioMovimiento();
                mov.setId(rs.getInt("id"));
                mov.setProducto(rs.getString("producto"));
                mov.setTipo(rs.getString("tipo"));
                mov.setCantidad(rs.getInt("cantidad"));
                mov.setFecha(LocalDateTime.parse(rs.getString("fecha").replace(" ", "T")));
                historial.add(mov);
            }
        }
        return historial;
    }

    public static void registrarVenta(double totalVenta) throws SQLException {
        String sql = "INSERT INTO ventas (fecha, total) VALUES (date('now'), ?)";
        executeUpdate(sql, totalVenta);
        EventBus.fireVentaRealizadaEvent();
    }

    // ===================== EQUIPOS Y PROVEEDORES =====================

    public static ObservableList<Proveedor> getProveedores() throws SQLException {
        ObservableList<Proveedor> proveedores = FXCollections.observableArrayList();
        String sql = "SELECT id, nombre, contacto, telefono FROM proveedores";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Proveedor p = new Proveedor();
                p.setId(rs.getInt("id"));
                p.setNombre(rs.getString("nombre"));
                p.setContacto(rs.getString("contacto"));
                p.setTelefono(rs.getString("telefono"));
                proveedores.add(p);
            }
        }
        return proveedores;
    }

    public static int insertarProveedor(Proveedor proveedor) throws SQLException {
        String sql = "INSERT INTO proveedores (nombre, contacto, telefono) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, proveedor.getNombre());
            stmt.setString(2, proveedor.getContacto());
            stmt.setString(3, proveedor.getTelefono());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public static ObservableList<PagoMensual> getEgresosMensuales(int año) throws SQLException {
        ObservableList<PagoMensual> data = FXCollections.observableArrayList();
        String sql = "SELECT strftime('%Y-%m', fecha) AS mes, SUM(monto) AS total " +
                "FROM egresos " +
                "WHERE strftime('%Y', fecha) = ? " +
                "GROUP BY mes " +
                "ORDER BY mes";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(año));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String mes = rs.getString("mes");
                double total = rs.getDouble("total");
                data.add(new PagoMensual(mes, total));
            }
        }
        return data;
    }

    public static double getTotalEgresosAnual(int año) throws SQLException {
        double total = 0.0;
        String sql = "SELECT SUM(monto) AS total FROM egresos WHERE strftime('%Y', fecha) = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(año));
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                total = rs.getDouble("total");
            }
        }
        return total;
    }

    public static ObservableList<EgresoDetalle> getDetallesEgresos(int año) throws SQLException {
        ObservableList<EgresoDetalle> detalles = FXCollections.observableArrayList();
        String sql = "SELECT descripcion, categoria, fecha, monto " +
                "FROM egresos " +
                "WHERE strftime('%Y', fecha) = ? " +
                "ORDER BY fecha DESC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(año));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                detalles.add(new EgresoDetalle(
                        LocalDate.parse(rs.getString("fecha")),
                        rs.getString("descripcion"),
                        rs.getString("categoria"),
                        rs.getDouble("monto")
                ));
            }
        }
        return detalles;
    }

    public static User obtenerUsuario(String username, String password) {
        String sql = "SELECT id, username, password, rol, last_login, acciones_realizadas FROM usuarios WHERE username = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Role role = Role.fromString(rs.getString("rol"));
                LocalDateTime lastLogin = null;
                String last = rs.getString("last_login");
                if (last != null) {
                    lastLogin = LocalDateTime.parse(last);
                }
                int acciones = rs.getInt("acciones_realizadas");
                return new User(rs.getInt("id"), rs.getString("username"), rs.getString("password"), role, lastLogin, acciones);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void crearUsuario(User user) throws SQLException {
        String sql = "INSERT INTO usuarios(username, password, rol, last_login, acciones_realizadas) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getRole().name());
            stmt.setString(4, user.getLastLogin() != null ? user.getLastLogin().toString() : null);
            stmt.setInt(5, user.getAccionesRealizadas());
            stmt.executeUpdate();
        }
    }

    public static void actualizarUsuario(User user) throws SQLException {
        String sql = "UPDATE usuarios SET username = ?, password = ?, rol = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getRole().name());
            stmt.setInt(4, user.getId());
            stmt.executeUpdate();
        }
    }

    public static void eliminarUsuario(int id) throws SQLException {
        String sql = "DELETE FROM usuarios WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public static ObservableList<User> listarUsuarios() throws SQLException {
        ObservableList<User> usuarios = FXCollections.observableArrayList();
        String sql = "SELECT id, username, password, rol, last_login, acciones_realizadas FROM usuarios";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Role role = Role.fromString(rs.getString("rol"));
                LocalDateTime lastLogin = null;
                String last = rs.getString("last_login");
                if (last != null) {
                    lastLogin = LocalDateTime.parse(last);
                }
                int acciones = rs.getInt("acciones_realizadas");
                usuarios.add(new User(rs.getInt("id"), rs.getString("username"), rs.getString("password"), role, lastLogin, acciones));
            }
        }
        return usuarios;
    }

    public static ObservableList<User> listarUsuariosPorRol(Role role) throws SQLException {
        ObservableList<User> usuarios = FXCollections.observableArrayList();
        String sql = "SELECT id, username, password, rol, last_login, acciones_realizadas FROM usuarios WHERE rol = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime lastLogin = null;
                    String last = rs.getString("last_login");
                    if (last != null) {
                        lastLogin = LocalDateTime.parse(last);
                    }
                    usuarios.add(new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            Role.fromString(rs.getString("rol")),
                            lastLogin,
                            rs.getInt("acciones_realizadas")));
                }
            }
        }
        return usuarios;
    }

    public static String obtenerNombreUsuarioPorId(int userId) throws SQLException {
        String sql = "SELECT username FROM usuarios WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        }
        return null;
    }

    public static void actualizarLastLogin(int userId) throws SQLException {
        String sql = "UPDATE usuarios SET last_login = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, LocalDateTime.now().toString());
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    public static void registrarAccion(int userId, String accion) throws SQLException {
        String insert = "INSERT INTO auditoria_usuarios(usuario_id, accion) VALUES(?, ?)";
        String update = "UPDATE usuarios SET acciones_realizadas = acciones_realizadas + 1 WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement psInsert = conn.prepareStatement(insert);
             PreparedStatement psUpdate = conn.prepareStatement(update)) {
            conn.setAutoCommit(false);
            psInsert.setInt(1, userId);
            psInsert.setString(2, accion);
            psInsert.executeUpdate();
            psUpdate.setInt(1, userId);
            psUpdate.executeUpdate();
            conn.commit();
        }
    }

    public static ObservableList<AuditoriaUsuario> listarAccionesPorUsuario(int usuarioId) throws SQLException {
        ObservableList<AuditoriaUsuario> acciones = FXCollections.observableArrayList();
        String sql = "SELECT au.id, u.username AS usuario, au.accion, au.fecha " +
                "FROM auditoria_usuarios au LEFT JOIN usuarios u ON au.usuario_id = u.id" +
                (usuarioId > 0 ? " WHERE au.usuario_id = ?" : "") +
                " ORDER BY au.fecha DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (usuarioId > 0) {
                stmt.setInt(1, usuarioId);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                acciones.add(new AuditoriaUsuario(
                        rs.getInt("id"),
                        rs.getString("usuario"),
                        rs.getString("accion"),
                        rs.getString("fecha")
                ));
            }
        }
        return acciones;
    }

    public static String obtenerStockJson() {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT nombre, stock FROM productos";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                if (sb.length() > 0) sb.append(";");
                sb.append(rs.getString("nombre")).append(":").append(rs.getInt("stock"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static int iniciarTurno(int usuarioId) throws SQLException {
        String stock = obtenerStockJson();
        String sql = "INSERT INTO turnos (usuario_id, fecha_inicio, stock_inicial) VALUES (?, datetime('now'), ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, usuarioId);
            stmt.setString(2, stock);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }

    public static Turno obtenerTurnoActivo(int usuarioId) {
        String sql = "SELECT * FROM turnos WHERE usuario_id = ? AND fecha_fin IS NULL";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapTurno(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Turno obtenerTurnoPorId(int turnoId) {
        String sql = "SELECT * FROM turnos WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, turnoId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapTurno(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Turno obtenerUltimoTurnoFinalizado(int usuarioId) {
        String sql = "SELECT * FROM turnos WHERE usuario_id = ? AND fecha_fin IS NOT NULL " +
                "ORDER BY datetime(fecha_fin) DESC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapTurno(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void reabrirTurno(int turnoId) throws SQLException {
        String sql = "UPDATE turnos SET fecha_fin = NULL, stock_final = NULL, ingresos_ventas = 0, " +
                "ingresos_clientes = 0 WHERE id = ?";
        executeUpdate(sql, turnoId);
    }

    public static void marcarResumenGenerado(int turnoId, String ruta) throws SQLException {
        String sql = "UPDATE turnos SET resumen_generado = ? WHERE id = ?";
        executeUpdate(sql, ruta, turnoId);
    }

    private static Turno mapTurno(ResultSet rs) throws SQLException {
        Turno turno = new Turno();
        turno.setId(rs.getInt("id"));
        turno.setUsuario_id(rs.getInt("usuario_id"));
        turno.setFecha_inicio(rs.getString("fecha_inicio"));
        turno.setFecha_fin(rs.getString("fecha_fin"));
        turno.setStock_inicial(rs.getString("stock_inicial"));
        turno.setStock_final(rs.getString("stock_final"));
        turno.setIngresos_ventas(rs.getDouble("ingresos_ventas"));
        turno.setIngresos_clientes(rs.getDouble("ingresos_clientes"));
        turno.setResumenGenerado(rs.getString("resumen_generado"));
        return turno;
    }

    public static double obtenerTotalVentasDesde(String fechaInicio) {
        double total = 0.0;
        String sql = "SELECT SUM(total) AS total FROM ventas WHERE date(fecha) >= date(?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fechaInicio);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                total = rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }

    public static double obtenerTotalPagosDesde(String fechaInicio) {
        double total = 0.0;
        String sql = "SELECT SUM(monto) AS total FROM pagos WHERE date(fecha_pago) >= date(?) AND estado = 'ACTIVO'";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fechaInicio);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                total = rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }

    public static void anularPago(int pagoId) throws SQLException {
        executeUpdate("UPDATE pagos SET estado = 'ANULADO' WHERE id = ?", pagoId);
    }

    public static List<Pago> listarClientesRegistrados(int usuarioId, LocalDateTime inicio, LocalDateTime fin) throws SQLException {
        try (Connection conn = getConnection()) {
            List<Pago> pagos = obtenerPagosPorAccion(conn, usuarioId, inicio, fin, "CREAR_PAGO", true);
            List<Pago> registros = new ArrayList<>();
            for (Pago pago : pagos) {
                if (pago != null && esPrimerPago(conn, pago.getClienteId(), pago.getId())) {
                    registros.add(pago);
                }
            }
            return registros;
        }
    }

    public static List<Pago> listarMembresiasRenovadas(int usuarioId, LocalDateTime inicio, LocalDateTime fin) throws SQLException {
        try (Connection conn = getConnection()) {
            List<Pago> pagos = obtenerPagosPorAccion(conn, usuarioId, inicio, fin, "CREAR_PAGO", true);
            List<Pago> renovaciones = new ArrayList<>();
            for (Pago pago : pagos) {
                if (pago != null && !esPrimerPago(conn, pago.getClienteId(), pago.getId())) {
                    renovaciones.add(pago);
                }
            }
            return renovaciones;
        }
    }

    public static Map<String, Number> obtenerIngresosPagos(int usuarioId, LocalDateTime inicio, LocalDateTime fin) throws SQLException {
        Map<String, Number> resumen = new HashMap<>();
        int cantidad = 0;
        double total = 0.0;
        try (Connection conn = getConnection()) {
            List<Pago> pagos = obtenerPagosPorAccion(conn, usuarioId, inicio, fin, "CREAR_PAGO", true);
            for (Pago pago : pagos) {
                if (pago != null) {
                    cantidad++;
                    total += pago.getMonto();
                }
            }
        }
        resumen.put("cantidad", cantidad);
        resumen.put("total", total);
        return resumen;
    }

    public static int getTotalUsuarios() throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public static List<Pago> listarPagosAnulados(int usuarioId, LocalDateTime inicio, LocalDateTime fin) throws SQLException {
        try (Connection conn = getConnection()) {
            List<Pago> pagos = obtenerPagosPorAccion(conn, usuarioId, inicio, fin, "ANULAR_PAGO", false);
            List<Pago> anulados = new ArrayList<>();
            for (Pago pago : pagos) {
                if (pago != null && "ANULADO".equalsIgnoreCase(pago.getEstado())) {
                    anulados.add(pago);
                }
            }
            return anulados;
        }
    }

    public static double obtenerTotalVentasEntre(LocalDateTime inicio, LocalDateTime fin) throws SQLException {
        String sql = "SELECT SUM(total) AS total FROM ventas WHERE date(fecha) BETWEEN date(?) AND date(?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, inicio.toLocalDate().toString());
            stmt.setString(2, fin.toLocalDate().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        }
        return 0.0;
    }

    public static String obtenerNombreCompletoCliente(int clienteId) throws SQLException {
        String sql = "SELECT nombres, apellidos FROM clientes WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clienteId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String nombres = Optional.ofNullable(rs.getString("nombres")).orElse("").trim();
                    String apellidos = Optional.ofNullable(rs.getString("apellidos")).orElse("").trim();
                    String completo = (nombres + " " + apellidos).trim();
                    return completo.isEmpty() ? null : completo;
                }
            }
        }
        return null;
    }

    public static int contarClientesMorosos() throws SQLException {
        String sql = "SELECT COUNT(*) FROM clientes WHERE fecha_vencimiento < date('now') AND activo = 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public static int contarCoaches() throws SQLException {
        String sql = "SELECT COUNT(*) FROM coaches";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public static ObservableList<CoachClientes> listarCoachesConMasClientes() throws SQLException {
        ObservableList<CoachClientes> lista = FXCollections.observableArrayList();
        String sql = "SELECT c.nombres || ' ' || c.apellidos AS coach, COUNT(cl.id) AS clientes " +
                "FROM coaches c LEFT JOIN clientes cl ON c.id = cl.coach_id " +
                "GROUP BY c.id ORDER BY clientes DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new CoachClientes(rs.getString("coach"), rs.getInt("clientes")));
            }
        }
        return lista;
    }

    public static void finalizarTurno(int id, String stockFinal, double ingresosVentas, double ingresosClientes) {
        String sql = "UPDATE turnos SET fecha_fin = datetime('now'), stock_final = ?, ingresos_ventas = ?, ingresos_clientes = ? WHERE id = ?";
        try {
            executeUpdate(sql, stockFinal, ingresosVentas, ingresosClientes, id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static ObservableList<Auditoria> getAuditoria() throws SQLException {
        ObservableList<Auditoria> registros = FXCollections.observableArrayList();
        String sql = "SELECT a.id, COALESCE(u.username, '') AS usuario, a.accion, a.detalle, a.timestamp " +
                "FROM auditoria a LEFT JOIN usuarios u ON a.usuario_id = u.id ORDER BY a.timestamp DESC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                registros.add(new Auditoria(
                        rs.getInt("id"),
                        rs.getString("usuario"),
                        rs.getString("accion"),
                        rs.getString("detalle"),
                        rs.getString("timestamp")
                ));
            }
        }
        return registros;
    }

    private static List<Pago> obtenerPagosPorAccion(Connection conn, int usuarioId, LocalDateTime inicio,
                                                    LocalDateTime fin, String accion, boolean soloActivos) throws SQLException {
        List<Pago> pagos = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT detalle FROM auditoria WHERE usuario_id = ? AND accion = ?");
        if (inicio != null) {
            sql.append(" AND datetime(timestamp) >= datetime(?)");
        }
        if (fin != null) {
            sql.append(" AND datetime(timestamp) <= datetime(?)");
        }
        sql.append(" ORDER BY timestamp");

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int index = 1;
            stmt.setInt(index++, usuarioId);
            stmt.setString(index++, accion);
            if (inicio != null) {
                stmt.setString(index++, formatDateTime(inicio));
            }
            if (fin != null) {
                stmt.setString(index++, formatDateTime(fin));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                Set<Integer> procesados = new HashSet<>();
                while (rs.next()) {
                    Integer pagoId = extraerPagoId(rs.getString("detalle"));
                    if (pagoId == null || !procesados.add(pagoId)) {
                        continue;
                    }
                    Pago pago = obtenerPagoPorId(conn, pagoId);
                    if (pago == null) {
                        continue;
                    }
                    if (soloActivos && !"ACTIVO".equalsIgnoreCase(pago.getEstado())) {
                        continue;
                    }
                    pagos.add(pago);
                }
            }
        }
        return pagos;
    }

    private static String formatDateTime(LocalDateTime value) {
        return value != null ? value.format(SQLITE_DATETIME_FORMATTER) : null;
    }

    private static Integer extraerPagoId(String detalle) {
        if (detalle == null) {
            return null;
        }
        Matcher matcher = PAGO_ID_PATTERN.matcher(detalle);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Pago obtenerPagoPorId(Connection conn, int pagoId) throws SQLException {
        String sql = "SELECT id, cliente_id, fecha_pago, fecha_vencimiento, tipo_membresia, monto, estado FROM pagos WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pagoId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Pago pago = new Pago();
                    pago.setId(rs.getInt("id"));
                    pago.setClienteId(rs.getInt("cliente_id"));
                    pago.setFechaPago(parseFecha(rs.getString("fecha_pago")));
                    pago.setFechaVencimiento(parseFecha(rs.getString("fecha_vencimiento")));
                    pago.setTipoMembresia(rs.getString("tipo_membresia"));
                    pago.setMonto(rs.getDouble("monto"));
                    pago.setEstado(rs.getString("estado"));
                    return pago;
                }
            }
        }
        return null;
    }

    private static boolean esPrimerPago(Connection conn, int clienteId, int pagoId) throws SQLException {
        String sql = "SELECT MIN(id) AS minimo FROM pagos WHERE cliente_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clienteId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("minimo") == pagoId;
                }
            }
        }
        return false;
    }

    private static LocalDate parseFecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normalizado = valor.trim();
        if (normalizado.length() > 10) {
            normalizado = normalizado.substring(0, 10);
        }
        try {
            return LocalDate.parse(normalizado);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
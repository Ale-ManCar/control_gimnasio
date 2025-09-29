package util;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
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
import models.ProveedorProducto;
import models.CoachClientes;
import models.Equipo;
import models.IngresoData;

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
                "telefono_visible TEXT NOT NULL DEFAULT ''," +
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

        String sqlEquipos = "CREATE TABLE IF NOT EXISTS equipos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT NOT NULL," +
                "tipo TEXT NOT NULL," +
                "estado TEXT NOT NULL," +
                "cantidad INTEGER NOT NULL DEFAULT 0," +
                "marca VARCHAR(255)," +
                "modelo VARCHAR(255)," +
                "peso INTEGER," +
                "fecha_adquisicion TEXT," +
                "frecuencia_mantenimiento TEXT," +
                "fecha_ultimo_mantenimiento TEXT," +
                "ubicacion TEXT," +
                "descripcion TEXT)";

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
                "fecha TEXT NOT NULL DEFAULT (datetime('now','localtime'))," +
                "total REAL NOT NULL)";

        String sqlEgresos = "CREATE TABLE IF NOT EXISTS egresos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "descripcion TEXT NOT NULL," +
                "monto REAL NOT NULL," +
                "fecha TEXT NOT NULL," +
                "categoria TEXT NOT NULL," +
                "proveedor TEXT," +
                "pdf_path TEXT)";

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

        String sqlProveedorProductos = "CREATE TABLE IF NOT EXISTS proveedor_productos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "proveedor_id INTEGER NOT NULL," +
                "tipo TEXT NOT NULL CHECK(tipo IN ('EQUIPO','INSUMO'))," +
                "equipo_id INTEGER," +
                "producto_id INTEGER," +
                "precio REAL NOT NULL DEFAULT 0," +
                "FOREIGN KEY (proveedor_id) REFERENCES proveedores(id) ON DELETE CASCADE," +
                "FOREIGN KEY (equipo_id) REFERENCES equipos(id) ON DELETE CASCADE," +
                "FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE)";

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
            stmt.execute(sqlEquipos);
            try {
                actualizarEsquemaEquipos(conn);
            } catch (SQLException e) {
                System.err.println("No se pudo actualizar el esquema de equipos: " + e.getMessage());
            }
            try {
                actualizarMantenimientosVencidos(conn);
            } catch (SQLException e) {
                System.err.println("No se pudieron actualizar los mantenimientos vencidos: " + e.getMessage());
            }
            stmt.execute(sqlUsuarios);
            stmt.execute(sqlTurnos);
            try { stmt.execute("ALTER TABLE turnos ADD COLUMN resumen_generado TEXT"); } catch (SQLException ignored) {}
            stmt.execute(sqlAuditoria);
            stmt.execute(sqlAuditoriaUsuarios);
            stmt.execute(sqlProveedores);
            stmt.execute(sqlProveedorProductos);
            try { stmt.execute("ALTER TABLE clientes ADD COLUMN telefono_visible TEXT NOT NULL DEFAULT ''"); } catch (SQLException ignored) {}
            try { stmt.execute("UPDATE clientes SET telefono_visible = telefono WHERE telefono_visible IS NULL OR telefono_visible = ''"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE clientes ADD COLUMN area TEXT"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE clientes ADD COLUMN coach_id INTEGER REFERENCES coaches(id)"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE productos ADD COLUMN stock_inicial INTEGER DEFAULT 0"); } catch (SQLException ignored) {}
            try { stmt.execute("UPDATE productos SET stock_inicial = stock_objetivo WHERE stock_inicial = 0 AND stock_objetivo IS NOT NULL"); } catch (SQLException ignored) {}
            try { stmt.execute("UPDATE productos SET stock_inicial = stock WHERE stock_inicial = 0"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE productos ADD COLUMN umbral INTEGER DEFAULT 0"); } catch (SQLException ignored) {}
            stmt.execute("INSERT OR IGNORE INTO config (id) VALUES (1)");
            try { stmt.execute("ALTER TABLE usuarios ADD COLUMN last_login TEXT"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE usuarios ADD COLUMN acciones_realizadas INTEGER DEFAULT 0"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE equipos ADD COLUMN ubicacion TEXT"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE equipos ADD COLUMN descripcion TEXT"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE equipos ADD COLUMN fecha_ultimo_mantenimiento TEXT"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE equipos ADD COLUMN cantidad INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE egresos ADD COLUMN proveedor TEXT"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE egresos ADD COLUMN pdf_path TEXT"); } catch (SQLException ignored) {}
            stmt.execute("INSERT OR IGNORE INTO proveedores (id, nombre, contacto, telefono) VALUES (1, 'Proveedor 1', '', ''), (2, 'Proveedor 2', '', '')");
            try {
                stmt.execute("UPDATE ventas SET fecha = COALESCE(strftime('%Y-%m-%d %H:%M:%S', fecha), fecha) WHERE fecha IS NOT NULL");
            } catch (SQLException ignored) {}
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
                Object param = params[i];
                if (param instanceof LocalDateTime) {
                    stmt.setString(i + 1, formatDateTime((LocalDateTime) param));
                } else if (param instanceof LocalDate) {
                    stmt.setString(i + 1, param.toString());
                } else {
                    stmt.setObject(i + 1, param);
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

    private static void actualizarEsquemaEquipos(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            try { stmt.execute("ALTER TABLE equipos ADD COLUMN marca VARCHAR(255)"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE equipos ADD COLUMN modelo VARCHAR(255)"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE equipos ADD COLUMN peso INTEGER"); } catch (SQLException ignored) {}

            boolean renombradaFecha = false;
            boolean existeFechaAdquisicion = columnExists(conn, "equipos", "fecha_adquisicion");
            boolean existeFechaCompra = columnExists(conn, "equipos", "fecha_compra");
            if (existeFechaCompra && !existeFechaAdquisicion) {
                try {
                    stmt.execute("ALTER TABLE equipos RENAME COLUMN fecha_compra TO fecha_adquisicion");
                    renombradaFecha = true;
                    existeFechaAdquisicion = true;
                    existeFechaCompra = false;
                } catch (SQLException ignored) {}
            }
            if (!existeFechaAdquisicion) {
                try { stmt.execute("ALTER TABLE equipos ADD COLUMN fecha_adquisicion TEXT"); } catch (SQLException ignored) {}
            }
            if (!renombradaFecha && existeFechaCompra) {
                try { stmt.execute("UPDATE equipos SET fecha_adquisicion = fecha_compra WHERE fecha_compra IS NOT NULL AND (fecha_adquisicion IS NULL OR fecha_adquisicion = '')"); } catch (SQLException ignored) {}
            }

            boolean migrarFrecuenciaLegacy = false;
            boolean existeFrecuencia = columnExists(conn, "equipos", "frecuencia_mantenimiento");
            boolean existeFrecuenciaLegacy = columnExists(conn, "equipos", "frecuencia_mantenimiento_legacy");
            if (existeFrecuencia && !existeFrecuenciaLegacy) {
                try {
                    stmt.execute("ALTER TABLE equipos RENAME COLUMN frecuencia_mantenimiento TO frecuencia_mantenimiento_legacy");
                    migrarFrecuenciaLegacy = true;
                    existeFrecuencia = false;
                    existeFrecuenciaLegacy = true;
                } catch (SQLException ignored) {}
            }
            if (!existeFrecuencia) {
                try { stmt.execute("ALTER TABLE equipos ADD COLUMN frecuencia_mantenimiento TEXT"); } catch (SQLException ignored) {}
                existeFrecuencia = true;
            }

            if (migrarFrecuenciaLegacy) {
                try { stmt.execute("UPDATE equipos SET frecuencia_mantenimiento = TRIM(CAST(frecuencia_mantenimiento_legacy AS TEXT)) WHERE frecuencia_mantenimiento_legacy IS NOT NULL"); } catch (SQLException ignored) {}
                try { stmt.execute("ALTER TABLE equipos DROP COLUMN frecuencia_mantenimiento_legacy"); } catch (SQLException ignored) {}
            } else {
                try { stmt.execute("UPDATE equipos SET frecuencia_mantenimiento = TRIM(CAST(frecuencia_mantenimiento AS TEXT)) WHERE frecuencia_mantenimiento IS NOT NULL"); } catch (SQLException ignored) {}
                if (existeFrecuenciaLegacy) {
                    try { stmt.execute("ALTER TABLE equipos DROP COLUMN frecuencia_mantenimiento_legacy"); } catch (SQLException ignored) {}
                }
            }
        }
    }

    private static void actualizarMantenimientosVencidos(Connection conn) throws SQLException {
        String selectSql = "SELECT id, nombre, estado, fecha_ultimo_mantenimiento, frecuencia_mantenimiento " +
                "FROM equipos " +
                "WHERE TRIM(COALESCE(frecuencia_mantenimiento, '')) <> '' " +
                "AND CAST(frecuencia_mantenimiento AS INTEGER) > 0 " +
                "AND fecha_ultimo_mantenimiento IS NOT NULL";

        List<Integer> pendientes = new ArrayList<>();
        List<LocalDate> nuevasFechas = new ArrayList<>();
        List<String> nombresEquipos = new ArrayList<>();
        List<String> estadosPrevios = new ArrayList<>();

        try (PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             ResultSet rs = selectStmt.executeQuery()) {
            LocalDate hoy = LocalDate.now();
            while (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");
                String estadoActual = rs.getString("estado");
                LocalDate fechaUltimo = parseFecha(rs.getString("fecha_ultimo_mantenimiento"));
                int frecuencia = parseEnteroSeguro(rs.getString("frecuencia_mantenimiento"));
                if (fechaUltimo == null || frecuencia <= 0) {
                    continue;
                }
                long diasTranscurridos = ChronoUnit.DAYS.between(fechaUltimo, hoy);
                if (diasTranscurridos < frecuencia) {
                    continue;
                }
                long ciclos = diasTranscurridos / frecuencia;
                if (ciclos <= 0) {
                    continue;
                }
                LocalDate nuevaFecha = fechaUltimo.plusDays(ciclos * (long) frecuencia);
                if (nuevaFecha.isAfter(hoy)) {
                    nuevaFecha = hoy;
                }
                if (!nuevaFecha.isEqual(fechaUltimo)) {
                    pendientes.add(id);
                    nuevasFechas.add(nuevaFecha);
                    nombresEquipos.add(nombre);
                    estadosPrevios.add(estadoActual);
                }
            }
        }

        if (!pendientes.isEmpty()) {
            String updateSql = "UPDATE equipos SET fecha_ultimo_mantenimiento = ?, frecuencia_mantenimiento = NULL, estado = ? WHERE id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                for (int i = 0; i < pendientes.size(); i++) {
                    updateStmt.setString(1, nuevasFechas.get(i).toString());
                    updateStmt.setString(2, "MANTENIMIENTO");
                    updateStmt.setInt(3, pendientes.get(i));
                    updateStmt.addBatch();
                }
                updateStmt.executeBatch();
            }

            String insertAuditoriaSql = "INSERT INTO auditoria (usuario_id, accion, detalle, timestamp) VALUES (?, ?, ?, ?)";
            try (PreparedStatement auditoriaStmt = conn.prepareStatement(insertAuditoriaSql)) {
                boolean registrar = false;
                LocalDateTime ahora = LocalDateTime.now();
                for (int i = 0; i < pendientes.size(); i++) {
                    String estadoAnterior = estadosPrevios.get(i);
                    if (estadoAnterior != null && estadoAnterior.equalsIgnoreCase("MANTENIMIENTO")) {
                        continue;
                    }
                    String nombreEquipo = nombresEquipos.get(i);
                    String identificador = (nombreEquipo != null && !nombreEquipo.isBlank())
                            ? nombreEquipo.trim()
                            : "ID " + pendientes.get(i);
                    String estadoAnteriorDesc = (estadoAnterior == null || estadoAnterior.isBlank())
                            ? "DESCONOCIDO"
                            : estadoAnterior;
                    String detalle = "Equipo " + identificador +
                            " marcado automáticamente como MANTENIMIENTO por mantenimiento vencido (estado anterior: " +
                            estadoAnteriorDesc + ", nueva fecha registrada: " + nuevasFechas.get(i) + ")";
                    auditoriaStmt.setInt(1, 0);
                    auditoriaStmt.setString(2, "EQUIPO_MANTENIMIENTO_AUTOMATICO");
                    auditoriaStmt.setString(3, detalle);
                    auditoriaStmt.setString(4, formatDateTime(ahora));
                    auditoriaStmt.addBatch();
                    registrar = true;
                }
                if (registrar) {
                    auditoriaStmt.executeBatch();
                }
            }
        }
    }

    private static int parseEnteroSeguro(String valor) {
        if (valor == null) {
            return 0;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }

    private static String nullIfBlank(String value) {
        return value != null && value.isBlank() ? null : value;
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
        String sql = "SELECT id, descripcion, monto, fecha, categoria, proveedor, pdf_path FROM egresos " +
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
                e.setProveedor(rs.getString("proveedor"));
                e.setPdfPath(rs.getString("pdf_path"));
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

    public static int registrarEgreso(Egreso egreso) throws SQLException {
        String sql = "INSERT INTO egresos (descripcion, monto, fecha, categoria, proveedor, pdf_path) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, egreso.getDescripcion());
            stmt.setDouble(2, egreso.getMonto());
            stmt.setString(3, egreso.getFecha() != null ? egreso.getFecha().toString() : LocalDate.now().toString());
            stmt.setString(4, egreso.getCategoria());
            stmt.setString(5, egreso.getProveedor());
            stmt.setString(6, egreso.getPdfPath());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    EventBus.fireEvent(EventBus.EventType.EGRESO_REGISTRADO);
                    return id;
                }
            }
        }
        EventBus.fireEvent(EventBus.EventType.EGRESO_REGISTRADO);
        return -1;
    }

    public static void actualizarRutaPdfEgreso(int egresoId, String pdfPath) throws SQLException {
        String sql = "UPDATE egresos SET pdf_path = ? WHERE id = ?";
        executeUpdate(sql, pdfPath, egresoId);
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
        String sql = "SELECT id, descripcion, monto, fecha, categoria, proveedor, pdf_path FROM egresos ORDER BY fecha DESC";
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
                e.setProveedor(rs.getString("proveedor"));
                e.setPdfPath(rs.getString("pdf_path"));
                egresos.add(e);
            }
        }
        return egresos;
    }

    public static ObservableList<Egreso> filtrarEgresos(LocalDate fechaInicio, LocalDate fechaFin, String categoria) throws SQLException {
        ObservableList<Egreso> egresos = FXCollections.observableArrayList();
        StringBuilder sql = new StringBuilder("SELECT id, descripcion, monto, fecha, categoria, proveedor, pdf_path FROM egresos WHERE 1=1");
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
                    e.setProveedor(rs.getString("proveedor"));
                    e.setPdfPath(rs.getString("pdf_path"));
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
            (SELECT COUNT(*) FROM clientes WHERE activo = 1 AND COALESCE(LOWER(tipoMembresia), '') <> 'diario') AS clientes_activos,
            (SELECT COUNT(*) FROM pagos WHERE date(fecha_pago) = CURRENT_DATE AND estado = 'ACTIVO') AS pagos_hoy,
            (SELECT COUNT(*) FROM clientes WHERE fecha_vencimiento BETWEEN CURRENT_DATE AND date('now', '+7 days') AND LOWER(tipoMembresia) <> 'diario') AS por_vencer
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
            (SELECT COUNT(*) FROM clientes WHERE activo = 1 AND COALESCE(LOWER(tipoMembresia), '') <> 'diario') AS clientes_activos,
            (SELECT COUNT(*) FROM clientes WHERE activo = 0) AS clientes_inactivos,
            (SELECT COUNT(*) FROM pagos WHERE strftime('%Y-%m', fecha_pago) = strftime('%Y-%m','now') AND estado = 'ACTIVO') AS membresias_mes,
            (SELECT COUNT(*) FROM clientes WHERE fecha_vencimiento BETWEEN date('now') AND date('now', '+7 day') AND LOWER(tipoMembresia) <> 'diario') AS por_vencer,
            (SELECT COUNT(*) FROM clientes WHERE fecha_vencimiento < date('now') AND activo = 1) AS clientes_morosos,
            (SELECT COALESCE(MAX(cantidad),0) FROM (SELECT COUNT(*) AS cantidad FROM clientes WHERE coach_id IS NOT NULL GROUP BY coach_id)) AS coaches_top,
            (SELECT COUNT(*) FROM clientes WHERE activo = 1 AND COALESCE(LOWER(tipoMembresia), '') <> 'diario' AND date(fecha_inicio) <= date('now') AND date(fecha_vencimiento) >= date('now')) AS activos_hoy
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
        return getDistribucionMembresias(LocalDate.of(año, 1, 1), LocalDate.of(año, 12, 31));
    }

    public static ObservableList<PieChart.Data> getDistribucionMembresias(LocalDate inicio, LocalDate fin) throws SQLException {
        if (inicio == null || fin == null) {
            return FXCollections.observableArrayList();
        }

        LocalDate start = inicio;
        LocalDate end = fin;
        if (end.isBefore(start)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }

        ObservableList<PieChart.Data> datos = FXCollections.observableArrayList();
        String sql = "SELECT COALESCE(tipo_membresia, 'SIN MEMBRESÍA') AS tipo, SUM(monto) AS total "
                + "FROM pagos "
                + "WHERE date(fecha_pago) BETWEEN ? AND ? "
                + "AND estado = 'ACTIVO' "
                + "GROUP BY tipo";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, start.toString());
            stmt.setString(2, end.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                datos.add(new PieChart.Data(
                        rs.getString("tipo"),
                        rs.getDouble("total")
                ));
            }
        }
        return datos;
    }

    public static List<IngresoData> getIngresosPorDia(LocalDate fecha) throws SQLException {
        if (fecha == null) {
            return Collections.emptyList();
        }

        String sql = "SELECT COALESCE(tipo_membresia, 'SIN MEMBRESÍA') AS etiqueta, SUM(monto) AS total "
                + "FROM pagos WHERE date(fecha_pago) = ? AND estado = 'ACTIVO' "
                + "GROUP BY etiqueta ORDER BY total DESC";
        return obtenerIngresosAgrupados(sql, fecha.toString());
    }

    public static List<IngresoData> getIngresosPorSemana(LocalDate inicio, LocalDate fin) throws SQLException {
        if (inicio == null || fin == null) {
            return Collections.emptyList();
        }

        LocalDate[] rango = ordenarFechas(inicio, fin);
        String sql = "SELECT date(fecha_pago) AS etiqueta, SUM(monto) AS total "
                + "FROM pagos WHERE date(fecha_pago) BETWEEN ? AND ? "
                + "AND estado = 'ACTIVO' GROUP BY etiqueta ORDER BY etiqueta";
        return obtenerIngresosAgrupados(sql, rango[0].toString(), rango[1].toString());
    }

    public static List<IngresoData> getIngresosPorMes(int año, int mes) throws SQLException {
        LocalDate inicio = LocalDate.of(año, mes, 1);
        LocalDate fin = inicio.withDayOfMonth(inicio.lengthOfMonth());
        String sql = "SELECT date(fecha_pago) AS etiqueta, SUM(monto) AS total "
                + "FROM pagos WHERE date(fecha_pago) BETWEEN ? AND ? "
                + "AND estado = 'ACTIVO' GROUP BY etiqueta ORDER BY etiqueta";
        return obtenerIngresosAgrupados(sql, inicio.toString(), fin.toString());
    }

    public static List<IngresoData> getIngresosPorAnio(int año) throws SQLException {
        List<IngresoData> datos = new ArrayList<>();
        for (PagoMensual mensual : getIngresosMensuales(año)) {
            datos.add(new IngresoData(mensual.getMes(), mensual.getTotal()));
        }
        return datos;
    }

    private static List<IngresoData> obtenerIngresosAgrupados(String sql, Object... params) throws SQLException {
        List<IngresoData> datos = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                datos.add(new IngresoData(rs.getString("etiqueta"), rs.getDouble("total")));
            }
        }
        return datos;
    }

    private static LocalDate[] ordenarFechas(LocalDate inicio, LocalDate fin) {
        LocalDate start = inicio;
        LocalDate end = fin;
        if (end.isBefore(start)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }
        return new LocalDate[]{start, end};
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
        LocalDate inicio = LocalDate.of(año, 1, 1);
        LocalDate fin = LocalDate.of(año, 12, 31);
        return getDetallesPagosEntre(inicio, fin);
    }

    public static List<PagoDetalle> getDetallesPagosEntre(LocalDate inicio, LocalDate fin) throws SQLException {
        if (inicio == null || fin == null) {
            return Collections.emptyList();
        }

        LocalDate[] rango = ordenarFechas(inicio, fin);
        List<PagoDetalle> detalles = new ArrayList<>();
        String sql = "SELECT date(pagos.fecha_pago) AS fecha, "
                + "clientes.nombres || ' ' || clientes.apellidos AS cliente, "
                + "clientes.id AS cliente_id, "
                + "COALESCE(pagos.tipo_membresia, 'SIN MEMBRESÍA') AS membresia, pagos.monto "
                + "FROM pagos pagos "
                + "JOIN clientes clientes ON pagos.cliente_id = clientes.id "
                + "WHERE date(pagos.fecha_pago) BETWEEN ? AND ? "
                + "AND pagos.estado = 'ACTIVO'";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rango[0].toString());
            stmt.setString(2, rango[1].toString());
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

    // ---------------------------------------------------------------------
    // Gestión de equipos
    // ---------------------------------------------------------------------

    public static ObservableList<Equipo> listarEquipos() throws SQLException {
        ObservableList<Equipo> equipos = FXCollections.observableArrayList();
        String sql = "SELECT id, nombre, tipo, estado, cantidad, marca, modelo, peso, fecha_adquisicion, frecuencia_mantenimiento, fecha_ultimo_mantenimiento, ubicacion, descripcion FROM equipos ORDER BY nombre";

        try (Connection conn = getConnection()) {
            actualizarMantenimientosVencidos(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    equipos.add(mapEquipo(rs));
                }
            }
        }
        return equipos;
    }

    public static void actualizarMantenimientosProgramados() {
        try (Connection conn = getConnection()) {
            actualizarMantenimientosVencidos(conn);
        } catch (SQLException e) {
            System.err.println("No se pudieron actualizar los mantenimientos programados: " + e.getMessage());
        }
    }

    public static void insertarEquipo(Equipo equipo) throws SQLException {
        String sql = "INSERT INTO equipos (nombre, tipo, estado, cantidad, marca, modelo, peso, fecha_adquisicion, frecuencia_mantenimiento, fecha_ultimo_mantenimiento, ubicacion, descripcion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        executeUpdate(sql,
                equipo.getNombre(),
                equipo.getTipo(),
                equipo.getEstado(),
                equipo.getCantidad(),
                nullIfBlank(equipo.getMarca()),
                nullIfBlank(equipo.getModelo()),
                equipo.getPesoAsInteger(),
                nullIfBlank(equipo.getFechaAdquisicion()),
                nullIfBlank(equipo.getFrecuenciaMantenimiento()),
                equipo.getFechaUltimoMantenimiento(),
                equipo.getUbicacion(),
                equipo.getDescripcion());
    }

    public static void actualizarEquipo(Equipo equipo) throws SQLException {
        String sql = "UPDATE equipos SET nombre = ?, tipo = ?, estado = ?, cantidad = ?, marca = ?, modelo = ?, peso = ?, fecha_adquisicion = ?, frecuencia_mantenimiento = ?, fecha_ultimo_mantenimiento = ?, ubicacion = ?, descripcion = ? WHERE id = ?";
        executeUpdate(sql,
                equipo.getNombre(),
                equipo.getTipo(),
                equipo.getEstado(),
                equipo.getCantidad(),
                nullIfBlank(equipo.getMarca()),
                nullIfBlank(equipo.getModelo()),
                equipo.getPesoAsInteger(),
                nullIfBlank(equipo.getFechaAdquisicion()),
                nullIfBlank(equipo.getFrecuenciaMantenimiento()),
                equipo.getFechaUltimoMantenimiento(),
                equipo.getUbicacion(),
                equipo.getDescripcion(),
                equipo.getId());
    }

    public static void eliminarEquipo(int equipoId) throws SQLException {
        String sql = "DELETE FROM equipos WHERE id = ?";
        executeUpdate(sql, equipoId);
    }

    public static ObservableList<Equipo> buscarEquiposPorEstado(String estado) throws SQLException {
        ObservableList<Equipo> equipos = FXCollections.observableArrayList();
        String sql = "SELECT id, nombre, tipo, estado, cantidad, marca, modelo, peso, fecha_adquisicion, frecuencia_mantenimiento, fecha_ultimo_mantenimiento, ubicacion, descripcion FROM equipos WHERE UPPER(estado) = UPPER(?) ORDER BY nombre";

        try (Connection conn = getConnection()) {
            actualizarMantenimientosVencidos(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, estado);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        equipos.add(mapEquipo(rs));
                    }
                }
            }
        }
        return equipos;
    }

    public static ObservableList<Equipo> buscarEquiposPorTipo(String tipo) throws SQLException {
        ObservableList<Equipo> equipos = FXCollections.observableArrayList();
        String sql = "SELECT id, nombre, tipo, estado, cantidad, marca, modelo, peso, fecha_adquisicion, frecuencia_mantenimiento, fecha_ultimo_mantenimiento, ubicacion, descripcion FROM equipos WHERE UPPER(tipo) = UPPER(?) ORDER BY nombre";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tipo);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    equipos.add(mapEquipo(rs));
                }
            }
        }
        return equipos;
    }

    public static List<Equipo> obtenerEquiposConMantenimientoProximo(int dias) throws SQLException {
        List<Equipo> equipos = new ArrayList<>();
        String sql = "SELECT id, nombre, tipo, estado, cantidad, marca, modelo, peso, fecha_adquisicion, frecuencia_mantenimiento, fecha_ultimo_mantenimiento, ubicacion, descripcion " +
                "FROM equipos " +
                "WHERE TRIM(COALESCE(frecuencia_mantenimiento, '')) <> '' " +
                "AND CAST(frecuencia_mantenimiento AS INTEGER) > 0 " +
                "AND fecha_ultimo_mantenimiento IS NOT NULL " +
                "AND date(fecha_ultimo_mantenimiento, '+' || CAST(frecuencia_mantenimiento AS INTEGER) || ' day') <= date('now', '+' || ? || ' day') " +
                "AND date(fecha_ultimo_mantenimiento, '+' || CAST(frecuencia_mantenimiento AS INTEGER) || ' day') >= date('now')";

        try (Connection conn = getConnection()) {
            actualizarMantenimientosVencidos(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, dias);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        equipos.add(mapEquipo(rs));
                    }
                }
            }
        }
        return equipos;
    }

    public static List<Equipo> obtenerEquiposConMantenimientoVencido() throws SQLException {
        List<Equipo> equipos = new ArrayList<>();
        String sql = "SELECT id, nombre, tipo, estado, cantidad, marca, modelo, peso, fecha_adquisicion, frecuencia_mantenimiento, fecha_ultimo_mantenimiento, ubicacion, descripcion " +
                "FROM equipos " +
                "WHERE TRIM(COALESCE(frecuencia_mantenimiento, '')) <> '' " +
                "AND CAST(frecuencia_mantenimiento AS INTEGER) > 0 " +
                "AND fecha_ultimo_mantenimiento IS NOT NULL " +
                "AND date(fecha_ultimo_mantenimiento, '+' || CAST(frecuencia_mantenimiento AS INTEGER) || ' day') < date('now')";

        try (Connection conn = getConnection()) {
            actualizarMantenimientosVencidos(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    equipos.add(mapEquipo(rs));
                }
            }
        }
        return equipos;
    }

    public static List<Equipo> obtenerEquiposEnEstadoCritico() throws SQLException {
        List<Equipo> equipos = new ArrayList<>();
        String sql = "SELECT id, nombre, tipo, estado, cantidad, marca, modelo, peso, fecha_adquisicion, frecuencia_mantenimiento, fecha_ultimo_mantenimiento, ubicacion, descripcion " +
                "FROM equipos WHERE UPPER(estado) IN ('CRITICO', 'FUERA DE SERVICIO', 'FUERA_SERVICIO')";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                equipos.add(mapEquipo(rs));
            }
        }
        return equipos;
    }

    public static List<Equipo> obtenerEquiposEnMalEstado() throws SQLException {
        List<Equipo> equipos = new ArrayList<>();
        String sql = "SELECT id, nombre, tipo, estado, cantidad, marca, modelo, peso, fecha_adquisicion, frecuencia_mantenimiento, fecha_ultimo_mantenimiento, ubicacion, descripcion " +
                "FROM equipos WHERE UPPER(estado) LIKE '%MAL%' OR UPPER(estado) LIKE '%DEFECT%'";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                equipos.add(mapEquipo(rs));
            }
        }
        return equipos;
    }

    private static Equipo mapEquipo(ResultSet rs) throws SQLException {
        Equipo equipo = new Equipo();
        equipo.setId(rs.getInt("id"));
        equipo.setNombre(Optional.ofNullable(rs.getString("nombre")).orElse(""));
        equipo.setTipo(Optional.ofNullable(rs.getString("tipo")).orElse(""));
        equipo.setEstado(Optional.ofNullable(rs.getString("estado")).orElse(""));
        equipo.setCantidad(rs.getInt("cantidad"));
        equipo.setMarca(rs.getString("marca"));
        equipo.setModelo(rs.getString("modelo"));
        Object pesoObj = rs.getObject("peso");
        if (pesoObj instanceof Number number) {
            equipo.setPeso(number.intValue());
        } else {
            equipo.setPeso((String) null);
        }
        equipo.setFechaAdquisicion(rs.getString("fecha_adquisicion"));
        equipo.setFechaUltimoMantenimiento(parseFecha(rs.getString("fecha_ultimo_mantenimiento")));
        equipo.setFrecuenciaMantenimiento(rs.getString("frecuencia_mantenimiento"));
        equipo.setUbicacion(rs.getString("ubicacion"));
        equipo.setDescripcion(rs.getString("descripcion"));
        return equipo;
    }

    // ---------------------------------------------------------------------
    // Gestión de productos
    // ---------------------------------------------------------------------

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
        String insertSql = "INSERT INTO inventario_historial (producto_id, tipo, cantidad, fecha) VALUES (?, 'ENTRADA', ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql);
                 PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                psUpdate.setInt(1, cantidad);
                psUpdate.setInt(2, productoId);
                psUpdate.executeUpdate();

                psInsert.setInt(1, productoId);
                psInsert.setInt(2, cantidad);
                psInsert.setString(3, formatDateTime(LocalDateTime.now()));
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
        String insertSql = "INSERT INTO inventario_historial (producto_id, tipo, cantidad, fecha) VALUES (?, 'SALIDA', ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql);
                 PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                psUpdate.setInt(1, cantidad);
                psUpdate.setInt(2, productoId);
                psUpdate.executeUpdate();

                psInsert.setInt(1, productoId);
                psInsert.setInt(2, cantidad);
                psInsert.setString(3, formatDateTime(LocalDateTime.now()));
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
        String sql = "INSERT INTO ventas (fecha, total) VALUES (?, ?)";
        executeUpdate(sql, LocalDateTime.now(), totalVenta);
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

    public static void actualizarProveedor(Proveedor proveedor) throws SQLException {
        String sql = "UPDATE proveedores SET nombre = ?, contacto = ?, telefono = ? WHERE id = ?";
        executeUpdate(sql,
                proveedor.getNombre(),
                proveedor.getContacto(),
                proveedor.getTelefono(),
                proveedor.getId());
    }

    public static void eliminarProveedor(int proveedorId) throws SQLException {
        String sql = "DELETE FROM proveedores WHERE id = ?";
        executeUpdate(sql, proveedorId);
    }

    public static ObservableList<ProveedorProducto> obtenerProductosProveedor(int proveedorId) throws SQLException {
        ObservableList<ProveedorProducto> productos = FXCollections.observableArrayList();
        String sql = "SELECT pp.id, pp.tipo, pp.precio, pp.equipo_id, pp.producto_id, " +
                "COALESCE(e.nombre, pr.nombre) AS nombre, e.peso AS peso_equipo " +
                "FROM proveedor_productos pp " +
                "LEFT JOIN equipos e ON pp.equipo_id = e.id " +
                "LEFT JOIN productos pr ON pp.producto_id = pr.id " +
                "WHERE pp.proveedor_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, proveedorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ProveedorProducto producto = new ProveedorProducto();
                    producto.setId(rs.getInt("id"));
                    producto.setProveedorId(proveedorId);
                    producto.setTipo(rs.getString("tipo"));
                    int equipoId = rs.getInt("equipo_id");
                    if (!rs.wasNull()) {
                        producto.setEquipoId(equipoId);
                    }
                    int insumoId = rs.getInt("producto_id");
                    if (!rs.wasNull()) {
                        producto.setProductoId(insumoId);
                    }
                    producto.setNombreProducto(rs.getString("nombre"));
                    if ("EQUIPO".equalsIgnoreCase(producto.getTipo())) {
                        Object pesoObj = rs.getObject("peso_equipo");
                        if (pesoObj instanceof Number number) {
                            producto.setPeso(String.valueOf(number.intValue()));
                        } else if (pesoObj != null) {
                            producto.setPeso(String.valueOf(pesoObj));
                        } else {
                            producto.setPeso(null);
                        }
                    }
                    producto.setPrecio(rs.getDouble("precio"));
                    producto.setSeleccionado(true);
                    productos.add(producto);
                }
            }
        }
        return productos;
    }

    public static void reemplazarProductosProveedor(int proveedorId, List<ProveedorProducto> productos) throws SQLException {
        String deleteSql = "DELETE FROM proveedor_productos WHERE proveedor_id = ?";
        String insertSql = "INSERT INTO proveedor_productos (proveedor_id, tipo, equipo_id, producto_id, precio) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setInt(1, proveedorId);
                deleteStmt.executeUpdate();
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                for (ProveedorProducto producto : productos) {
                    insertStmt.setInt(1, proveedorId);
                    insertStmt.setString(2, producto.getTipo());
                    if ("EQUIPO".equalsIgnoreCase(producto.getTipo())) {
                        if (producto.getEquipoId() != null) {
                            insertStmt.setInt(3, producto.getEquipoId());
                        } else {
                            insertStmt.setNull(3, Types.INTEGER);
                        }
                        insertStmt.setNull(4, Types.INTEGER);
                    } else {
                        insertStmt.setNull(3, Types.INTEGER);
                        if (producto.getProductoId() != null) {
                            insertStmt.setInt(4, producto.getProductoId());
                        } else {
                            insertStmt.setNull(4, Types.INTEGER);
                        }
                    }
                    insertStmt.setDouble(5, producto.getPrecio());
                    insertStmt.addBatch();
                }
                insertStmt.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public static ObservableList<Proveedor> obtenerProveedoresDetallados() throws SQLException {
        ObservableList<Proveedor> proveedores = getProveedores();
        if (proveedores.isEmpty()) {
            return proveedores;
        }

        Map<Integer, Proveedor> mapa = new HashMap<>();
        for (Proveedor proveedor : proveedores) {
            mapa.put(proveedor.getId(), proveedor);
        }

        String sql = "SELECT pp.id, pp.proveedor_id, pp.tipo, pp.precio, pp.equipo_id, pp.producto_id, " +
                "COALESCE(e.nombre, pr.nombre) AS nombre, e.peso AS peso_equipo " +
                "FROM proveedor_productos pp " +
                "LEFT JOIN equipos e ON pp.equipo_id = e.id " +
                "LEFT JOIN productos pr ON pp.producto_id = pr.id";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Proveedor proveedor = mapa.get(rs.getInt("proveedor_id"));
                if (proveedor == null) {
                    continue;
                }
                ProveedorProducto producto = new ProveedorProducto();
                producto.setId(rs.getInt("id"));
                producto.setProveedorId(proveedor.getId());
                producto.setTipo(rs.getString("tipo"));
                int equipoId = rs.getInt("equipo_id");
                if (!rs.wasNull()) {
                    producto.setEquipoId(equipoId);
                }
                int insumoId = rs.getInt("producto_id");
                if (!rs.wasNull()) {
                    producto.setProductoId(insumoId);
                }
                producto.setNombreProducto(rs.getString("nombre"));
                if ("EQUIPO".equalsIgnoreCase(producto.getTipo())) {
                    Object pesoObj = rs.getObject("peso_equipo");
                    if (pesoObj instanceof Number number) {
                        producto.setPeso(String.valueOf(number.intValue()));
                    } else if (pesoObj != null) {
                        producto.setPeso(String.valueOf(pesoObj));
                    } else {
                        producto.setPeso(null);
                    }
                }
                producto.setPrecio(rs.getDouble("precio"));
                producto.setSeleccionado(true);
                producto.setProveedor(proveedor);
                proveedor.agregarProducto(producto);
            }
        }
        return proveedores;
    }

    public static ObservableList<ProveedorProducto> obtenerComparativaProducto(String tipo, int itemId) throws SQLException {
        ObservableList<ProveedorProducto> comparacion = FXCollections.observableArrayList();
        String sql;
        if ("EQUIPO".equalsIgnoreCase(tipo)) {
            sql = "SELECT pp.id, pp.proveedor_id, pp.tipo, pp.precio, pp.equipo_id, prov.nombre AS proveedor_nombre, " +
                    "prov.contacto, prov.telefono, e.nombre AS producto_nombre " +
                    "FROM proveedor_productos pp " +
                    "JOIN proveedores prov ON prov.id = pp.proveedor_id " +
                    "LEFT JOIN equipos e ON pp.equipo_id = e.id " +
                    "WHERE pp.tipo = 'EQUIPO' AND pp.equipo_id = ?";
        } else {
            sql = "SELECT pp.id, pp.proveedor_id, pp.tipo, pp.precio, pp.producto_id, prov.nombre AS proveedor_nombre, " +
                    "prov.contacto, prov.telefono, p.nombre AS producto_nombre " +
                    "FROM proveedor_productos pp " +
                    "JOIN proveedores prov ON prov.id = pp.proveedor_id " +
                    "LEFT JOIN productos p ON pp.producto_id = p.id " +
                    "WHERE pp.tipo = 'INSUMO' AND pp.producto_id = ?";
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ProveedorProducto producto = new ProveedorProducto();
                    producto.setId(rs.getInt("id"));
                    producto.setProveedorId(rs.getInt("proveedor_id"));
                    producto.setTipo(rs.getString("tipo"));
                    if ("EQUIPO".equalsIgnoreCase(tipo)) {
                        int equipoId = rs.getInt("equipo_id");
                        if (!rs.wasNull()) {
                            producto.setEquipoId(equipoId);
                        }
                    } else {
                        int productoId = rs.getInt("producto_id");
                        if (!rs.wasNull()) {
                            producto.setProductoId(productoId);
                        }
                    }
                    producto.setNombreProducto(rs.getString("producto_nombre"));
                    producto.setPrecio(rs.getDouble("precio"));
                    Proveedor proveedor = new Proveedor();
                    proveedor.setId(rs.getInt("proveedor_id"));
                    proveedor.setNombre(rs.getString("proveedor_nombre"));
                    proveedor.setContacto(rs.getString("contacto"));
                    proveedor.setTelefono(rs.getString("telefono"));
                    producto.setProveedor(proveedor);
                    comparacion.add(producto);
                }
            }
        }

        return comparacion;
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

    public static double getTotalEgresosEntre(LocalDate inicio, LocalDate fin) throws SQLException {
        if (inicio == null || fin == null) {
            return 0.0;
        }

        LocalDate[] rango = ordenarFechas(inicio, fin);
        String sql = "SELECT SUM(monto) AS total FROM egresos WHERE date(fecha) BETWEEN ? AND ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rango[0].toString());
            stmt.setString(2, rango[1].toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("total");
            }
        }
        return 0.0;
    }

    public static ObservableList<EgresoDetalle> getDetallesEgresos(int año) throws SQLException {
        return getDetallesEgresosEntre(LocalDate.of(año, 1, 1), LocalDate.of(año, 12, 31));
    }

    public static ObservableList<EgresoDetalle> getDetallesEgresosEntre(LocalDate inicio, LocalDate fin) throws SQLException {
        ObservableList<EgresoDetalle> detalles = FXCollections.observableArrayList();
        if (inicio == null || fin == null) {
            return detalles;
        }

        LocalDate[] rango = ordenarFechas(inicio, fin);
        String sql = "SELECT id, descripcion, categoria, fecha, monto, proveedor, pdf_path "
                + "FROM egresos "
                + "WHERE date(fecha) BETWEEN ? AND ? "
                + "ORDER BY fecha DESC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rango[0].toString());
            stmt.setString(2, rango[1].toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                detalles.add(new EgresoDetalle(
                        rs.getInt("id"),
                        LocalDate.parse(rs.getString("fecha")),
                        rs.getString("descripcion"),
                        rs.getString("categoria"),
                        rs.getDouble("monto"),
                        rs.getString("proveedor"),
                        rs.getString("pdf_path")
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
        String sql = "INSERT INTO turnos (usuario_id, fecha_inicio, stock_inicial) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, usuarioId);
            stmt.setString(2, formatDateTime(LocalDateTime.now()));
            stmt.setString(3, stock);
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

    public static String obtenerResumenGeneradoPrimerTurnoDelDia(int usuarioId, LocalDateTime fechaReferencia) {
        LocalDate diaReferencia = fechaReferencia != null ? fechaReferencia.toLocalDate() : LocalDate.now();
        LocalDateTime inicioDia = diaReferencia.atStartOfDay();
        LocalDateTime finDia = diaReferencia.plusDays(1).atStartOfDay();
        String sql = "SELECT resumen_generado FROM turnos WHERE usuario_id = ? " +
                "AND fecha_fin IS NOT NULL " +
                "AND resumen_generado IS NOT NULL " +
                "AND TRIM(resumen_generado) <> '' " +
                "AND datetime(fecha_fin) >= datetime(?) " +
                "AND datetime(fecha_fin) < datetime(?) " +
                "ORDER BY datetime(fecha_fin) ASC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            stmt.setString(2, formatDateTime(inicioDia));
            stmt.setString(3, formatDateTime(finDia));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String ruta = rs.getString("resumen_generado");
                if (ruta != null && !ruta.isBlank()) {
                    return ruta;
                }
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

    public static double obtenerTotalVentasDesde(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            return 0.0;
        }
        double total = 0.0;
        String sql = "SELECT SUM(total) AS total FROM ventas WHERE datetime(fecha) BETWEEN datetime(?) AND datetime(?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, formatDateTime(fechaInicio));
            stmt.setString(2, formatDateTime(fechaFin));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                total = rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }

    public static double obtenerTotalVentasDesde(LocalDateTime fechaInicio) {
        return obtenerTotalVentasDesde(fechaInicio, LocalDateTime.now());
    }

    public static double obtenerTotalVentasDesde(String fechaInicio) {
        return obtenerTotalVentasDesde(parseDateTime(fechaInicio));
    }

    public static double obtenerTotalPagosDesde(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            return 0.0;
        }
        double total = 0.0;
        LocalDateTime inicio = fechaInicio.isAfter(fechaFin) ? fechaFin : fechaInicio;
        LocalDateTime fin = fechaInicio.isAfter(fechaFin) ? fechaInicio : fechaFin;
        String sql = "SELECT SUM(monto) AS total FROM pagos WHERE estado = 'ACTIVO' " +
                "AND datetime(fecha_pago) BETWEEN datetime(?) AND datetime(?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, formatDateTime(inicio));
            stmt.setString(2, formatDateTime(fin));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                total = rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }

    public static double obtenerTotalPagosDesde(LocalDateTime fechaInicio) {
        return obtenerTotalPagosDesde(fechaInicio, LocalDateTime.now());
    }

    public static double obtenerTotalPagosDesde(String fechaInicio) {
        return obtenerTotalPagosDesde(parseDateTime(fechaInicio));
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
        if (inicio == null || fin == null) {
            return 0.0;
        }
        String sql = "SELECT SUM(total) AS total FROM ventas WHERE datetime(fecha) BETWEEN datetime(?) AND datetime(?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, formatDateTime(inicio));
            stmt.setString(2, formatDateTime(fin));
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

    public static int contarClientesActivos() throws SQLException {
        String sql = "SELECT COUNT(*) FROM clientes WHERE activo = 1 AND COALESCE(LOWER(tipoMembresia), '') <> 'diario'";
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
        finalizarTurno(id, stockFinal, ingresosVentas, ingresosClientes, LocalDateTime.now());
    }

    public static void finalizarTurno(int id, String stockFinal, double ingresosVentas, double ingresosClientes, LocalDateTime fechaFin) {
        String sql = "UPDATE turnos SET fecha_fin = ?, stock_final = ?, ingresos_ventas = ?, ingresos_clientes = ? WHERE id = ?";
        LocalDateTime fin = fechaFin != null ? fechaFin : LocalDateTime.now();
        try {
            executeUpdate(sql, fin, stockFinal, ingresosVentas, ingresosClientes, id);
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

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : SQLITE_DATETIME_FORMATTER.format(dateTime);
    }

    public static LocalDateTime parseDateTime(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normalizado = valor.trim();
        try {
            if (normalizado.contains("T")) {
                return LocalDateTime.parse(normalizado);
            }
            if (normalizado.length() == 16) {
                normalizado = normalizado + ":00";
            }
            return LocalDateTime.parse(normalizado, SQLITE_DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(normalizado.replace(' ', 'T'));
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
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
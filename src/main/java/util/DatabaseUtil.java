package util;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import models.Auditoria;
import models.Egreso;
import models.EgresoDetalle;
import models.PagoDetalle;
import models.PagoMensual;
import models.Producto;
import models.User;
import models.Role;
import models.Turno;
import models.Equipo;
import models.Proveedor;
import models.Compra;
import models.CompraDetalle;

public class DatabaseUtil {
    private static final String URL = "jdbc:sqlite:database/gimnasio.db";
    private static final int BUSY_TIMEOUT_MS = 60000;

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
                "rol TEXT NOT NULL)";

        String sqlProductos = "CREATE TABLE IF NOT EXISTS productos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT NOT NULL UNIQUE," +
                "stock INTEGER NOT NULL," +
                "precio REAL NOT NULL," +
                "tipo TEXT NOT NULL," +
                "precio_compra REAL NOT NULL," +
                "unidades_por_paca INTEGER," +
                "peso_total REAL," +
                "peso_por_scoop REAL)";

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
                "FOREIGN KEY (usuario_id) REFERENCES usuarios(id))";

        String sqlAuditoria = "CREATE TABLE IF NOT EXISTS auditoria (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "usuario_id INTEGER," +
                "accion TEXT NOT NULL," +
                "detalle TEXT," +
                "timestamp TEXT DEFAULT (datetime('now'))," +
                "FOREIGN KEY (usuario_id) REFERENCES usuarios(id))";

        String sqlProveedores = "CREATE TABLE IF NOT EXISTS proveedores (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT NOT NULL UNIQUE," +
                "contacto TEXT," +
                "telefono TEXT)";

        String sqlEquipos = "CREATE TABLE IF NOT EXISTS equipos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT NOT NULL UNIQUE," +
                "stock INTEGER NOT NULL," +
                "precio REAL NOT NULL," +
                "proveedor_id INTEGER," +
                "FOREIGN KEY (proveedor_id) REFERENCES proveedores(id))";

        String sqlCompras = "CREATE TABLE IF NOT EXISTS compras (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "proveedor_id INTEGER NOT NULL," +
                "fecha TEXT NOT NULL," +
                "total REAL," +
                "ruta_pdf TEXT," +
                "FOREIGN KEY (proveedor_id) REFERENCES proveedores(id))";

        String sqlComprasDetalle = "CREATE TABLE IF NOT EXISTS compras_detalle (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "compra_id INTEGER NOT NULL," +
                "equipo_id INTEGER NOT NULL," +
                "cantidad INTEGER NOT NULL," +
                "precio REAL NOT NULL," +
                "FOREIGN KEY (compra_id) REFERENCES compras(id)," +
                "FOREIGN KEY (equipo_id) REFERENCES equipos(id))";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false);
            stmt.execute(sqlClientes);
            stmt.execute(sqlAlertas);
            stmt.execute(sqlConfig);
            stmt.execute(sqlPagos);
            stmt.execute(sqlProductos);
            stmt.execute(sqlVentas);
            stmt.execute(sqlEgresos);
            stmt.execute(sqlCoaches);
            stmt.execute(sqlUsuarios);
            stmt.execute(sqlTurnos);
            stmt.execute(sqlAuditoria);
            stmt.execute(sqlProveedores);
            stmt.execute(sqlEquipos);
            stmt.execute(sqlCompras);
            stmt.execute(sqlComprasDetalle);
            try { stmt.execute("ALTER TABLE clientes ADD COLUMN area TEXT"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE clientes ADD COLUMN coach_id INTEGER REFERENCES coaches(id)"); } catch (SQLException ignored) {}
            stmt.execute("INSERT OR IGNORE INTO config (id) VALUES (1)");
            stmt.execute("INSERT OR IGNORE INTO usuarios (id, username, password, rol) VALUES (1, 'admin', 'admin', 'ADMIN'), (2, 'recep', 'recep', 'RECEPCIONISTA')");
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
                "AND strftime('%m', fecha_pago) = ?";

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
            (SELECT COUNT(*) FROM pagos WHERE strftime('%Y-%m', fecha_pago) = strftime('%Y-%m','now')) AS membresias_mes,
            (SELECT COUNT(*) FROM clientes WHERE fecha_vencimiento BETWEEN CURRENT_DATE AND date('now', '+7 days')) AS por_vencer
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                stats.put("clientes_activos", rs.getInt("clientes_activos"));
                stats.put("clientes_inactivos", rs.getInt("clientes_inactivos"));
                stats.put("membresias_mes", rs.getInt("membresias_mes"));
                stats.put("por_vencer", rs.getInt("por_vencer"));
            }
        }
        return stats;
    }

    public static ObservableList<PagoMensual> getIngresosMensuales(int año) throws SQLException {
        ObservableList<PagoMensual> data = FXCollections.observableArrayList();

        String sql = "SELECT mes, SUM(total) AS total FROM ("
                + "SELECT strftime('%Y-%m', fecha_pago) AS mes, monto AS total FROM pagos "
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

    public static List<PagoDetalle> getDetallesPagos(int año) throws SQLException {
        List<PagoDetalle> detalles = new ArrayList<>();
        String sql = "SELECT pagos.fecha_pago AS fecha, "
                + "clientes.nombres || ' ' || clientes.apellidos AS cliente, "
                + "clientes.id AS cliente_id, "
                + "pagos.tipo_membresia AS membresia, pagos.monto "
                + "FROM pagos pagos "
                + "JOIN clientes clientes ON pagos.cliente_id = clientes.id "
                + "WHERE strftime('%Y', pagos.fecha_pago) = ?";

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
        String sql = "INSERT INTO productos (nombre, stock, precio, tipo, precio_compra, unidades_por_paca, peso_total, peso_por_scoop) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        executeUpdate(sql,
                producto.getNombre(),
                producto.getStock(),
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
        String sql = "SELECT id, nombre, stock, precio, tipo, precio_compra, unidades_por_paca, peso_total, peso_por_scoop FROM productos";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Producto p = new Producto();
                p.setId(rs.getInt("id"));
                p.setNombre(rs.getString("nombre").toUpperCase(Locale.ROOT));
                p.setStock(rs.getInt("stock"));
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

    public static void actualizarProducto(int id, double nuevoPrecio, int unidadesExtra) throws SQLException {
        String sql = "UPDATE productos SET precio = ?, stock = stock + ? WHERE id = ?";
        executeUpdate(sql, nuevoPrecio, unidadesExtra, id);
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

    public static ObservableList<Equipo> getEquipos() throws SQLException {
        ObservableList<Equipo> equipos = FXCollections.observableArrayList();
        String sql = "SELECT id, nombre, stock, precio, proveedor_id FROM equipos";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Equipo e = new Equipo();
                e.setId(rs.getInt("id"));
                e.setNombre(rs.getString("nombre"));
                e.setStock(rs.getInt("stock"));
                e.setPrecio(rs.getDouble("precio"));
                e.setProveedorId(rs.getInt("proveedor_id"));
                equipos.add(e);
            }
        }
        return equipos;
    }

    public static int insertarEquipo(Equipo equipo) throws SQLException {
        String sql = "INSERT INTO equipos (nombre, stock, precio, proveedor_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, equipo.getNombre());
            stmt.setInt(2, equipo.getStock());
            stmt.setDouble(3, equipo.getPrecio());
            stmt.setObject(4, equipo.getProveedorId());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public static void actualizarStockEquipo(int id, int nuevoStock) throws SQLException {
        String sql = "UPDATE equipos SET stock = ? WHERE id = ?";
        executeUpdate(sql, nuevoStock, id);
    }

    public static void registrarCompra(int proveedorId, int equipoId, int cantidad, double precioUnitario, String rutaPdf) throws SQLException {
        String sqlCompra = "INSERT INTO compras (proveedor_id, fecha, total, ruta_pdf) VALUES (?, date('now'), ?, ?)";
        String sqlDetalle = "INSERT INTO compras_detalle (compra_id, equipo_id, cantidad, precio) VALUES (?, ?, ?, ?)";
        String sqlUpdate = "UPDATE equipos SET stock = stock + ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmtCompra = conn.prepareStatement(sqlCompra, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtDetalle = conn.prepareStatement(sqlDetalle);
             PreparedStatement stmtUpdate = conn.prepareStatement(sqlUpdate)) {
            conn.setAutoCommit(false);
            double total = cantidad * precioUnitario;
            stmtCompra.setInt(1, proveedorId);
            stmtCompra.setDouble(2, total);
            stmtCompra.setString(3, rutaPdf);
            stmtCompra.executeUpdate();
            int compraId = -1;
            try (ResultSet rs = stmtCompra.getGeneratedKeys()) {
                if (rs.next()) {
                    compraId = rs.getInt(1);
                }
            }
            stmtDetalle.setInt(1, compraId);
            stmtDetalle.setInt(2, equipoId);
            stmtDetalle.setInt(3, cantidad);
            stmtDetalle.setDouble(4, precioUnitario);
            stmtDetalle.executeUpdate();

            stmtUpdate.setInt(1, cantidad);
            stmtUpdate.setInt(2, equipoId);
            stmtUpdate.executeUpdate();

            conn.commit();
        }
    }

    public static Map<String, Double> getPreciosPorProveedor(int equipoId) throws SQLException {
        Map<String, Double> data = new LinkedHashMap<>();
        String sql = "SELECT p.nombre, AVG(cd.precio) AS promedio FROM compras_detalle cd " +
                "JOIN compras c ON cd.compra_id = c.id " +
                "JOIN proveedores p ON c.proveedor_id = p.id " +
                "WHERE cd.equipo_id = ? GROUP BY p.nombre";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, equipoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    data.put(rs.getString("nombre"), rs.getDouble("promedio"));
                }
            }
        }
        return data;
    }

    public static void insertarEgreso(Egreso egreso) throws SQLException {
        String sql = "INSERT INTO egresos (descripcion, monto, fecha, categoria) VALUES (?, ?, ?, ?)";
        executeUpdate(sql,
                egreso.getDescripcion(),
                egreso.getMonto(),
                egreso.getFecha().toString(),
                egreso.getCategoria());

        EventBus.fireEvent(EventBus.EventType.EGRESO_REGISTRADO);
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
        String sql = "SELECT id, username, password, rol FROM usuarios WHERE username = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Role role = Role.valueOf(rs.getString("rol"));
                return new User(rs.getInt("id"), rs.getString("username"), rs.getString("password"), role);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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
                Turno t = new Turno();
                t.setId(rs.getInt("id"));
                t.setUsuario_id(rs.getInt("usuario_id"));
                t.setFecha_inicio(rs.getString("fecha_inicio"));
                t.setStock_inicial(rs.getString("stock_inicial"));
                return t;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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
        String sql = "SELECT SUM(monto) AS total FROM pagos WHERE date(fecha_pago) >= date(?)";
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
}
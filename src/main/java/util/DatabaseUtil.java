package util;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import models.Egreso;
import models.EgresoDetalle;
import models.PagoDetalle;
import models.PagoMensual;
import models.Producto;
import models.Proveedor;
import models.Usuario;
import models.MovimientoInventario;
import util.SecurityUtil;

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
                "area TEXT," +
                "coach_id INTEGER REFERENCES coaches(id)," +
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

        String sqlProveedores = "CREATE TABLE IF NOT EXISTS proveedores (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT NOT NULL UNIQUE," +
                "telefono TEXT," +
                "email TEXT," +
                "activo BOOLEAN DEFAULT 1)";

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
                "categoria TEXT NOT NULL," +
                "numero_factura TEXT," +
                "proveedor_id INTEGER," +
                "adjunto TEXT)";

        String sqlEgresoDetalles = "CREATE TABLE IF NOT EXISTS egreso_detalles (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "egreso_id INTEGER NOT NULL," +
                "producto_id INTEGER NOT NULL," +
                "cantidad INTEGER NOT NULL," +
                "costo REAL NOT NULL," +
                "FOREIGN KEY (egreso_id) REFERENCES egresos(id)," +
                "FOREIGN KEY (producto_id) REFERENCES productos(id))";

        String sqlMovimientosInventario = "CREATE TABLE IF NOT EXISTS movimientos_inventario (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "producto_id INTEGER NOT NULL," +
                "tipo TEXT NOT NULL," +
                "cantidad INTEGER NOT NULL," +
                "motivo TEXT," +
                "usuario TEXT NOT NULL," +
                "fecha TEXT NOT NULL," +
                "saldo INTEGER NOT NULL," +
                "FOREIGN KEY (producto_id) REFERENCES productos(id))";

        String sqlUsuarios = "CREATE TABLE IF NOT EXISTS usuarios (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT NOT NULL UNIQUE," +
                "password TEXT NOT NULL," +
                "rol TEXT NOT NULL," +
                "activo BOOLEAN DEFAULT TRUE," +
                "ultimo_ingreso TEXT)";

        String sqlAuditoria = "CREATE TABLE IF NOT EXISTS auditoria (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "usuario TEXT NOT NULL," +
                "fecha_hora TEXT NOT NULL," +
                "accion TEXT NOT NULL," +
                "entidad TEXT NOT NULL," +
                "id_entidad INTEGER," +
                "detalle TEXT)";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false);
            stmt.execute(sqlClientes);
            stmt.execute(sqlAlertas);
            stmt.execute(sqlConfig);
            stmt.execute(sqlPagos);
            stmt.execute(sqlProductos);
            stmt.execute(sqlVentas);
            stmt.execute(sqlProveedores);
            stmt.execute(sqlEgresos);
            stmt.execute(sqlEgresoDetalles);
            stmt.execute(sqlCoaches);
            stmt.execute(sqlUsuarios);
            stmt.execute(sqlAuditoria);
            stmt.execute(sqlMovimientosInventario);
            stmt.execute("INSERT OR IGNORE INTO config (id) VALUES (1)");
            // Inicializamos un usuario administrador con contraseña cifrada utilizando BCrypt
            final String adminHash = SecurityUtil.hashPassword("admin123");
            stmt.execute("INSERT OR IGNORE INTO usuarios (id,nombre,password,rol,activo) VALUES (1,'admin','" + adminHash + "','ADMIN',1)");
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

    /**
     * Obtiene un usuario por su nombre. La contraseña recuperada está en formato
     * de hash BCrypt, por lo que debe verificarse utilizando
     * {@link util.SecurityUtil#verifyPassword(String, String)}.
     */
    public static Usuario obtenerUsuarioPorNombre(String nombre) {
        String sql = "SELECT * FROM usuarios WHERE nombre = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Usuario u = new Usuario();
                u.setId(rs.getInt("id"));
                u.setNombre(rs.getString("nombre"));
                u.setPasswordHash(rs.getString("password"));
                u.setRol(rs.getString("rol"));
                u.setActivo(rs.getBoolean("activo"));
                String ultimo = rs.getString("ultimo_ingreso");
                if (ultimo != null) {
                    u.setUltimoIngreso(LocalDateTime.parse(ultimo));
                }
                return u;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void actualizarUltimoIngreso(int userId) {
        String sql = "UPDATE usuarios SET ultimo_ingreso = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, LocalDateTime.now().toString());
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int countUsuarios() {
        String sql = "SELECT COUNT(*) FROM usuarios";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int contarClientesActivos(LocalDate inicio, LocalDate fin) throws SQLException {
        String sql = "SELECT COUNT(*) FROM clientes WHERE activo = 1 AND date(fechaInicio) BETWEEN ? AND ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, inicio.toString());
            ps.setString(2, fin.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public static int contarClientesInactivos(LocalDate inicio, LocalDate fin) throws SQLException {
        String sql = "SELECT COUNT(*) FROM clientes WHERE activo = 0 AND date(fechaInicio) BETWEEN ? AND ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, inicio.toString());
            ps.setString(2, fin.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public static int contarMembresiasNuevas(LocalDate inicio, LocalDate fin) throws SQLException {
        String sql = "SELECT COUNT(*) FROM pagos WHERE date(fecha_pago) BETWEEN ? AND ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, inicio.toString());
            ps.setString(2, fin.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public static int contarProductosStockCritico() throws SQLException {
        String sql = "SELECT COUNT(*) FROM productos WHERE stock <= 5";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public static String obtenerTopProductoVendido(LocalDate inicio, LocalDate fin) throws SQLException {
        String sql = "SELECT p.nombre, SUM(mi.cantidad) AS total FROM movimientos_inventario mi " +
                "JOIN productos p ON mi.producto_id = p.id " +
                "WHERE mi.tipo = 'VENTA' AND date(mi.fecha) BETWEEN ? AND ? " +
                "GROUP BY mi.producto_id ORDER BY total DESC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, inicio.toString());
            ps.setString(2, fin.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("nombre") : null;
        }
    }

    public static Integer insertUsuario(String nombre, String passwordHash, String rol, boolean activo) {
        String sql = "INSERT INTO usuarios(nombre, password, rol, activo) VALUES(?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, nombre);
            stmt.setString(2, passwordHash);
            stmt.setString(3, rol);
            stmt.setInt(4, activo ? 1 : 0);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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
        String sql = "SELECT id, descripcion, monto, fecha, categoria, numero_factura, proveedor_id, adjunto FROM egresos " +
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
                e.setNumeroFactura(rs.getString("numero_factura"));
                e.setProveedorId(rs.getInt("proveedor_id"));
                e.setRutaAdjunto(rs.getString("adjunto"));
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

    public static ObservableList<Proveedor> getProveedores(boolean activos) throws SQLException {
        ObservableList<Proveedor> proveedores = FXCollections.observableArrayList();
        String sql = "SELECT id, nombre, telefono, email, activo FROM proveedores" + (activos ? " WHERE activo = 1" : "");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Proveedor p = new Proveedor();
                p.setId(rs.getInt("id"));
                p.setNombre(rs.getString("nombre"));
                p.setTelefono(rs.getString("telefono"));
                p.setEmail(rs.getString("email"));
                p.setActivo(rs.getBoolean("activo"));
                proveedores.add(p);
            }
        }
        return proveedores;
    }

    public static Integer insertarProveedor(String nombre, String telefono, String email) throws SQLException {
        String sql = "INSERT INTO proveedores(nombre, telefono, email, activo) VALUES(?,?,?,1)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, nombre);
            stmt.setString(2, telefono);
            stmt.setString(3, email);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return null;
    }

    public static int actualizarProveedor(int id, String nombre, String telefono, String email) throws SQLException {
        String sql = "UPDATE proveedores SET nombre = ?, telefono = ?, email = ? WHERE id = ?";
        return executeUpdate(sql, nombre, telefono, email, id);
    }

    public static int cambiarEstadoProveedor(int id, boolean activo) throws SQLException {
        String sql = "UPDATE proveedores SET activo = ? WHERE id = ?";
        return executeUpdate(sql, activo ? 1 : 0, id);
    }

    public static void actualizarStockProducto(int id, int cantidadVendida) throws SQLException {
        String sql = "UPDATE productos SET stock = stock - ? WHERE id = ?";
        executeUpdate(sql, cantidadVendida, id);
    }

    public static void actualizarProducto(int id, double nuevoPrecio, int unidadesExtra) throws SQLException {
        String sql = "UPDATE productos SET precio = ?, stock = stock + ? WHERE id = ?";
        executeUpdate(sql, nuevoPrecio, unidadesExtra, id);
    }

    public static void actualizarCostoPromedio(int productoId, double costoUnitario, int cantidad) throws SQLException {
        String selectSql = "SELECT stock, precio_compra FROM productos WHERE id = ?";
        String updateSql = "UPDATE productos SET precio_compra = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement selectStmt = null;
        PreparedStatement updateStmt = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            selectStmt = conn.prepareStatement(selectSql);
            selectStmt.setInt(1, productoId);
            ResultSet rs = selectStmt.executeQuery();
            if (rs.next()) {
                int stockActual = rs.getInt("stock");
                double costoAnterior = rs.getDouble("precio_compra");
                int stockAnterior = stockActual - cantidad;
                int nuevoStock = stockActual;
                double nuevoCosto = ((stockAnterior * costoAnterior) + (cantidad * costoUnitario)) / nuevoStock;
                updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setDouble(1, nuevoCosto);
                updateStmt.setInt(2, productoId);
                updateStmt.executeUpdate();
            }
            conn.commit();
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
            if (selectStmt != null) try { selectStmt.close(); } catch (SQLException ignored) {}
            if (updateStmt != null) try { updateStmt.close(); } catch (SQLException ignored) {}
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    public static void insertMovimientoInventario(int productoId, String tipo, int cantidad,
                                                  String motivo, String usuario, LocalDateTime fecha,
                                                  int saldo) throws SQLException {
        String sql = "INSERT INTO movimientos_inventario " +
                "(producto_id, tipo, cantidad, motivo, usuario, fecha, saldo) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        executeUpdate(sql, productoId, tipo, cantidad, motivo, usuario, fecha.toString(), saldo);
    }

    public static ObservableList<MovimientoInventario> getMovimientosPorProducto(int productoId) throws SQLException {
        ObservableList<MovimientoInventario> movimientos = FXCollections.observableArrayList();
        String sql = "SELECT * FROM movimientos_inventario WHERE producto_id = ? ORDER BY fecha DESC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productoId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                MovimientoInventario m = new MovimientoInventario();
                m.setId(rs.getInt("id"));
                m.setProductoId(rs.getInt("producto_id"));
                m.setTipo(rs.getString("tipo"));
                m.setCantidad(rs.getInt("cantidad"));
                m.setMotivo(rs.getString("motivo"));
                m.setUsuario(rs.getString("usuario"));
                m.setFecha(LocalDateTime.parse(rs.getString("fecha")));
                m.setSaldo(rs.getInt("saldo"));
                movimientos.add(m);
            }
        }
        return movimientos;
    }

    public static void registrarVenta(double totalVenta) throws SQLException {
        String sql = "INSERT INTO ventas (fecha, total) VALUES (date('now'), ?)";
        executeUpdate(sql, totalVenta);
        EventBus.fireVentaRealizadaEvent();
    }

    public static int insertarEgreso(Egreso egreso) throws SQLException {
        String sql = "INSERT INTO egresos (descripcion, monto, fecha, categoria, numero_factura, proveedor_id, adjunto) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, egreso.getDescripcion());
            stmt.setDouble(2, egreso.getMonto());
            stmt.setString(3, egreso.getFecha().toString());
            stmt.setString(4, egreso.getCategoria());
            stmt.setString(5, egreso.getNumeroFactura());
            stmt.setInt(6, egreso.getProveedorId());
            stmt.setString(7, egreso.getRutaAdjunto());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            int id = -1;
            if (rs.next()) {
                id = rs.getInt(1);
            }
            conn.commit();
            EventBus.fireEvent(EventBus.EventType.EGRESO_REGISTRADO);
            return id;
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

    public static void insertarEgresoDetalle(int egresoId, int productoId, int cantidad, double costo) throws SQLException {
        String sql = "INSERT INTO egreso_detalles (egreso_id, producto_id, cantidad, costo) VALUES (?, ?, ?, ?)";
        executeUpdate(sql, egresoId, productoId, cantidad, costo);
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
}
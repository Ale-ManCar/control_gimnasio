package util;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import models.PagoDetalle;
import models.PagoMensual;
import models.Producto;

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
                "mensaje_whatsapp TEXT DEFAULT '¡Hola [NOMBRE] [APELLIDO]! Tu membresía en [GIMNASIO] vence en [DIAS] días')";

        String sqlPagos = "CREATE TABLE IF NOT EXISTS pagos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "cliente_id INTEGER NOT NULL," +
                "fecha_pago TEXT NOT NULL," +
                "fecha_vencimiento TEXT," +
                "tipo_membresia TEXT," +
                "monto REAL NOT NULL," +
                "FOREIGN KEY (cliente_id) REFERENCES clientes(id))";

        String sqlProductos = "CREATE TABLE IF NOT EXISTS productos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT NOT NULL UNIQUE," +
                "stock INTEGER NOT NULL," +
                "precio REAL NOT NULL," + // Precio de venta (por unidad o por scoop)
                "tipo TEXT NOT NULL," +   // PACA, KG, LB
                "precio_compra REAL NOT NULL," + // Precio de compra (por paca o envase)
                "unidades_por_paca INTEGER," + // Solo para tipo PACA
                "peso_total REAL," + // Para KG/LB: peso total del envase
                "peso_por_scoop REAL)"; // Para KG/LB: peso por scoop en gramos

        String sqlVentas = "CREATE TABLE IF NOT EXISTS ventas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "fecha TEXT NOT NULL DEFAULT (date('now'))," +
                "total REAL NOT NULL)";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false);
            stmt.execute(sqlClientes);
            stmt.execute(sqlAlertas);
            stmt.execute(sqlConfig);
            stmt.execute(sqlPagos);
            stmt.execute(sqlProductos);
            stmt.execute(sqlVentas);
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

    public static double obtenerTotalPagosDelMesActual() {
        double total = 0.0;
        String sql = "SELECT SUM(monto) AS total FROM pagos WHERE strftime('%Y-%m', fecha_pago) = strftime('%Y-%m', 'now')";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                total = rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return total;
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
                        rs.getString("membresia"),
                        rs.getDouble("monto")
                ));
            }
        }
        return detalles;
    }

    // 🔽 Métodos relacionados con productos (ACTUALIZADOS)
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
                p.setNombre(rs.getString("nombre"));
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
    }

    public static double obtenerTotalVentasDelMes() {
        double total = 0.0;
        String sql = "SELECT SUM(total) AS total FROM ventas "
                + "WHERE strftime('%Y-%m', fecha) = strftime('%Y-%m', 'now')";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                total = rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }
}
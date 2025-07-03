package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
//import javafx.scene.control.cell.PropertyValueFactory;
import models.Cliente;
import models.PagoHistorial;
import util.DatabaseUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RenovacionController {
    // Controles UI
    @FXML private TableView<Cliente> tablaClientes;
    @FXML private ComboBox<String> cbNuevaMembresia;
    @FXML private DatePicker dpFechaRenovacion;
    @FXML private TextField txtMonto;
    @FXML private TableView<PagoHistorial> tablaHistorial;
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;
    @FXML private Label lblPagina;

    // Datos
    private final ObservableList<Cliente> todosClientes = FXCollections.observableArrayList();
    private List<Cliente> clientesPaginados = new ArrayList<>();
    private int paginaActual = 1;
    private int clientesPorPagina = 8;
    private int totalPaginas = 1;

    @FXML
    public void initialize() {
        try {
            // Configuración inicial
            cbNuevaMembresia.getItems().addAll("1 Mes", "3 Meses", "6 Meses", "1 Año");
            dpFechaRenovacion.setValue(LocalDate.now());

            // Cargar clientes
            cargarTodosClientes();
            actualizarTablaClientes();

            // Configurar paginación
            actualizarControlesPaginacion();

            // Configurar listeners
            tablaClientes.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    cargarHistorialPagos(newVal.getTelefono());
                }
            });

        } catch (Exception e) {
            mostrarAlerta("Error Crítico", "No se pudo cargar la pantalla: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cargarTodosClientes() {
        String sql = "SELECT nombres, apellidos, telefono, tipoMembresia, fecha_vencimiento FROM clientes " +
                "WHERE fecha_vencimiento BETWEEN date('now') AND date('now', '+7 days')";

        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            todosClientes.clear();
            while (rs.next()) {
                todosClientes.add(new Cliente(
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("telefono"),
                        rs.getString("tipoMembresia"),
                        LocalDate.parse(rs.getString("fecha_vencimiento"))
                ));
            }

            // Calcular paginación
            totalPaginas = (int) Math.ceil((double) todosClientes.size() / clientesPorPagina);
            if (totalPaginas == 0) totalPaginas = 1;

        } catch (SQLException e) {
            mostrarAlerta("Error de BD", "Error al cargar clientes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void actualizarTablaClientes() {
        int inicio = (paginaActual - 1) * clientesPorPagina;
        int fin = Math.min(inicio + clientesPorPagina, todosClientes.size());

        if (inicio < todosClientes.size()) {
            clientesPaginados = todosClientes.subList(inicio, fin);
            tablaClientes.setItems(FXCollections.observableArrayList(clientesPaginados));
        } else {
            tablaClientes.setItems(FXCollections.observableArrayList());
        }
    }

    private void actualizarControlesPaginacion() {
        lblPagina.setText("Página " + paginaActual + " de " + totalPaginas);
        btnAnterior.setDisable(paginaActual <= 1);
        btnSiguiente.setDisable(paginaActual >= totalPaginas);
    }

    // Método para precargar cliente
    public void precargarCliente(Cliente cliente) {
        // Buscar el cliente en la lista
        for (int i = 0; i < todosClientes.size(); i++) {
            if (todosClientes.get(i).getTelefono().equals(cliente.getTelefono())) {
                // Seleccionar el cliente en la tabla
                int pagina = (i / clientesPorPagina) + 1;

                // Si está en otra página, cambiar a esa página
                if (pagina != paginaActual) {
                    paginaActual = pagina;
                    actualizarTablaClientes();
                }

                // Seleccionar y desplazar
                tablaClientes.getSelectionModel().select(i % clientesPorPagina);
                tablaClientes.scrollTo(i % clientesPorPagina);

                // Cargar su historial
                cargarHistorialPagos(cliente.getTelefono());
                break;
            }
        }
    }

    @FXML
    private void paginaAnterior() {
        if (paginaActual > 1) {
            paginaActual--;
            actualizarTablaClientes();
            actualizarControlesPaginacion();
        }
    }

    @FXML
    private void paginaSiguiente() {
        if (paginaActual < totalPaginas) {
            paginaActual++;
            actualizarTablaClientes();
            actualizarControlesPaginacion();
        }
    }

    @FXML
    private void handleRenovar() {
        Cliente seleccionado = tablaClientes.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta("Error", "Selecciona un cliente de la tabla");
            return;
        }

        if (cbNuevaMembresia.getValue() == null || dpFechaRenovacion.getValue() == null || txtMonto.getText().isEmpty()) {
            mostrarAlerta("Error", "Completa todos los campos: membresía, fecha y monto");
            return;
        }

        double monto;
        try {
            monto = Double.parseDouble(txtMonto.getText());
            if (monto < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "Monto inválido. Ingresa un número válido positivo.");
            return;
        }

        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);

            String sql = "UPDATE clientes SET tipoMembresia = ?, fecha_vencimiento = ? WHERE telefono = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            LocalDate nuevaFecha = calcularNuevaFecha(
                    dpFechaRenovacion.getValue(),
                    cbNuevaMembresia.getValue()
            );

            stmt.setString(1, cbNuevaMembresia.getValue());
            stmt.setString(2, nuevaFecha.toString());
            stmt.setString(3, seleccionado.getTelefono());

            int filasActualizadas = stmt.executeUpdate();

            if (filasActualizadas > 0) {
                // Obtener ID del cliente
                String sqlId = "SELECT id FROM clientes WHERE telefono = ?";
                PreparedStatement stmtId = conn.prepareStatement(sqlId);
                stmtId.setString(1, seleccionado.getTelefono());
                ResultSet rs = stmtId.executeQuery();

                if (rs.next()) {
                    int clienteId = rs.getInt("id");

                    String sqlPago = "INSERT INTO pagos (cliente_id, fecha_pago, fecha_vencimiento, monto) VALUES (?, ?, ?, ?)";
                    PreparedStatement stmtPago = conn.prepareStatement(sqlPago);
                    stmtPago.setInt(1, clienteId);
                    stmtPago.setString(2, LocalDate.now().toString());
                    stmtPago.setString(3, nuevaFecha.toString());
                    stmtPago.setDouble(4, monto);
                    stmtPago.executeUpdate();
                }

                conn.commit();
                mostrarAlerta("Éxito", "Membresía renovada y pago registrado.");
                cargarTodosClientes();
                actualizarTablaClientes();
                txtMonto.clear();
            } else {
                mostrarAlerta("Error", "No se pudo actualizar la membresía");
            }

        } catch (SQLException e) {
            mostrarAlerta("Error de BD", "Error en base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private LocalDate calcularNuevaFecha(LocalDate fechaRenovacion, String membresia) {
        return switch (membresia) {
            case "1 Mes" -> fechaRenovacion.plusMonths(1);
            case "3 Meses" -> fechaRenovacion.plusMonths(3);
            case "6 Meses" -> fechaRenovacion.plusMonths(6);
            case "1 Año" -> fechaRenovacion.plusYears(1);
            default -> fechaRenovacion;
        };
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void cargarHistorialPagos(String telefono) {
        ObservableList<PagoHistorial> historial = FXCollections.observableArrayList();
        String sql = "SELECT pagos.fecha_pago, pagos.tipo_membresia, pagos.monto FROM pagos JOIN clientes ON pagos.cliente_id = clientes.id WHERE clientes.telefono = ? ORDER BY pagos.fecha_pago DESC";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, telefono);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                historial.add(new PagoHistorial(
                        LocalDate.parse(rs.getString("fecha_pago")),
                        rs.getString("tipo_membresia"),
                        rs.getDouble("monto")
                ));
            }
            tablaHistorial.setItems(historial);
        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudo cargar historial: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
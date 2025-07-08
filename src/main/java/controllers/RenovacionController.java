package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import models.Cliente;
import models.PagoHistorial;
import util.DatabaseUtil;

import java.sql.*;
import java.time.LocalDate;
//import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RenovacionController {
    @FXML private TableView<Cliente> tablaClientes;
    @FXML private ComboBox<String> cbNuevaMembresia;
    @FXML private DatePicker dpFechaRenovacion;
    @FXML private TextField txtMonto;
    @FXML private TableView<PagoHistorial> tablaHistorial;
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;
    @FXML private Label lblPagina;
    @FXML private TextField txtBuscar;
    @FXML private VBox panelDerecho;
    @FXML private Label lblInfoCliente;

    private final ObservableList<Cliente> clientesProximos = FXCollections.observableArrayList();
    private final ObservableList<Cliente> todosClientes = FXCollections.observableArrayList();
    private List<Cliente> clientesPaginados = new ArrayList<>();
    private int paginaActual = 1;
    private int clientesPorPagina = 8;
    private int totalPaginas = 1;

    @FXML
    public void initialize() {
        try {
            // ESTILOS DE CONTROLES
            aplicarEstilos();

            cbNuevaMembresia.getItems().addAll("1 Mes", "3 Meses", "6 Meses", "1 Año");
            cbNuevaMembresia.setValue("1 Mes");
            dpFechaRenovacion.setValue(LocalDate.now());

            cargarClientesProximos();
            actualizarTablaClientes();
            actualizarControlesPaginacion();

            // PANEL DERECHO OCULTO INICIALMENTE
            panelDerecho.setVisible(false);

            tablaClientes.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    cargarHistorialPagos(newVal.getTelefono());
                    mostrarInformacionCliente(newVal);
                    panelDerecho.setVisible(true);
                } else {
                    panelDerecho.setVisible(false);
                }
            });

            tablaClientes.setRowFactory(tv -> {
                TableRow<Cliente> row = new TableRow<>();
                row.setOnMouseEntered(event -> {
                    if (!row.isEmpty()) {
                        Tooltip tooltip = new Tooltip(row.getItem().getTooltipText());
                        tooltip.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                        Tooltip.install(row, tooltip);
                    }
                });
                row.setOnMouseExited(event -> {
                    if (!row.isEmpty()) {
                        Tooltip.uninstall(row, null);
                    }
                });
                return row;
            });

        } catch (Exception e) {
            mostrarAlerta("Error Crítico", "No se pudo cargar la pantalla: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void aplicarEstilos() {
        // ESTILOS TABLA CLIENTES
        tablaClientes.setStyle("-fx-font-size: 12px; -fx-padding: 5px;");
        tablaHistorial.setStyle("-fx-font-size: 12px; -fx-padding: 5px;");

        // ESTILO BOTONES
        String botonStyle = "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-padding: 5 10; -fx-background-radius: 5;";
        btnAnterior.setStyle(botonStyle);
        btnSiguiente.setStyle(botonStyle);

        // ESTILO PANEL DERECHO
        panelDerecho.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 15px; -fx-border-color: #e0e0e0; "
                + "-fx-border-width: 1px; -fx-border-radius: 5px; -fx-background-radius: 5px;");

        // ESTILO FORMULARIO
        cbNuevaMembresia.setStyle("-fx-font-size: 12px;");
        dpFechaRenovacion.setStyle("-fx-font-size: 12px;");
        txtMonto.setStyle("-fx-font-size: 12px;");
    }

    private void mostrarInformacionCliente(Cliente cliente) {
        String info = String.format("Cliente seleccionado:\n\n"
                        + "Nombre: %s %s\n"
                        + "Teléfono: %s\n"
                        + "Membresía actual: %s\n"
                        + "Vencimiento: %s\n"
                        + "Días restantes: %d",
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getTelefono(),
                cliente.getTipoMembresia(),
                cliente.getFecha_vencimiento(),
                cliente.getDiasRestantes());

        lblInfoCliente.setText(info);
    }

    // ===== MÉTODOS PRINCIPALES ACTUALIZADOS ===== //
    private void cargarClientesProximos() {
        String sql = "SELECT nombres, apellidos, telefono, tipoMembresia, fecha_vencimiento " +
                "FROM clientes WHERE activo = 1 " +
                "AND fecha_vencimiento BETWEEN date('now') AND date('now', '+7 days')";

        cargarClientesDesdeSQL(sql, clientesProximos);
    }

    private void cargarTodosClientesActivos() {
        String sql = "SELECT nombres, apellidos, telefono, tipoMembresia, fecha_vencimiento " +
                "FROM clientes WHERE activo = 1";

        cargarClientesDesdeSQL(sql, todosClientes);
    }

    private void cargarClientesDesdeSQL(String sql, ObservableList<Cliente> destino) {
        destino.clear();
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Cliente cliente = new Cliente(
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("telefono"),
                        rs.getString("tipoMembresia"),
                        LocalDate.parse(rs.getString("fecha_vencimiento"))
                );

                //long diasRestantes = ChronoUnit.DAYS.between(
                //        LocalDate.now(),
                //        cliente.getFecha_vencimientoDate()
                //);
                //cliente.setDiasRestantes((int) diasRestantes);

                destino.add(cliente);
            }

        } catch (SQLException e) {
            mostrarAlerta("Error de BD", "Error al cargar clientes: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // ===== FIN MÉTODOS PRINCIPALES ===== //

    // ===== FILTRADO Y PAGINACIÓN ===== //
    @FXML
    private void filtrarClientes() {
        String filtro = txtBuscar.getText().trim().toLowerCase();

        if (filtro.isEmpty()) {
            paginaActual = 1;
            cargarClientesProximos();
            actualizarTablaClientes();
            actualizarControlesPaginacion();
            return;
        }

        if (todosClientes.isEmpty()) {
            cargarTodosClientesActivos();
        }

        List<Cliente> filtrados = todosClientes.stream()
                .filter(cliente ->
                        (cliente.getNombres() + " " + cliente.getApellidos()).toLowerCase().contains(filtro) ||
                                cliente.getTelefono().contains(filtro)
                )
                .collect(Collectors.toList());

        tablaClientes.setItems(FXCollections.observableArrayList(filtrados));
    }

    @FXML
    private void limpiarFiltro() {
        txtBuscar.clear();
        paginaActual = 1;
        cargarClientesProximos();
        actualizarTablaClientes();
        actualizarControlesPaginacion();
    }

    private void actualizarTablaClientes() {
        int inicio = (paginaActual - 1) * clientesPorPagina;
        int fin = Math.min(inicio + clientesPorPagina, clientesProximos.size());

        if (inicio < clientesProximos.size()) {
            clientesPaginados = clientesProximos.subList(inicio, fin);
            tablaClientes.setItems(FXCollections.observableArrayList(clientesPaginados));
        } else {
            tablaClientes.setItems(FXCollections.observableArrayList());
        }

        totalPaginas = (int) Math.ceil((double) clientesProximos.size() / clientesPorPagina);
        if (totalPaginas == 0) totalPaginas = 1;
    }

    private void actualizarControlesPaginacion() {
        lblPagina.setText("Página " + paginaActual + " de " + totalPaginas);
        btnAnterior.setDisable(paginaActual <= 1);
        btnSiguiente.setDisable(paginaActual >= totalPaginas);
    }
    // ===== FIN FILTRADO Y PAGINACIÓN ===== //

    // ===== RENOVACIÓN Y ACCIONES ===== //
    public void precargarCliente(Cliente cliente) {
        for (int i = 0; i < clientesProximos.size(); i++) {
            if (clientesProximos.get(i).getTelefono().equals(cliente.getTelefono())) {
                int pagina = (i / clientesPorPagina) + 1;

                if (pagina != paginaActual) {
                    paginaActual = pagina;
                    actualizarTablaClientes();
                }

                tablaClientes.getSelectionModel().select(i % clientesPorPagina);
                tablaClientes.scrollTo(i % clientesPorPagina);

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

                // Recargar solo clientes próximos después de renovar
                cargarClientesProximos();
                actualizarTablaClientes();
                actualizarControlesPaginacion();
                txtMonto.clear();
            } else {
                mostrarAlerta("Error", "No se pudo actualizar la membresía");
            }

        } catch (SQLException e) {
            mostrarAlerta("Error de BD", "Error en base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // ===== FIN RENOVACIÓN Y ACCIONES ===== //

    // ===== MÉTODOS AUXILIARES ===== //
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

    // Método añadido para compatibilidad con DashboardController
    public void setModoTodosClientes(boolean modo) {
        if (modo) {
            cargarTodosClientesActivos();
        } else {
            cargarClientesProximos();
        }
        actualizarTablaClientes();
        actualizarControlesPaginacion();
    }
    // ===== FIN MÉTODOS AUXILIARES ===== //
}
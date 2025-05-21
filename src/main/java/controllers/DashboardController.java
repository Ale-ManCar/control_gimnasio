package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import models.Cliente;
import models.PagoMensual;
import util.DatabaseUtil;

import java.sql.*;
import java.util.Map;

public class DashboardController {

    @FXML private LineChart<String, Number> chartIngresos;
    @FXML private TableView<Cliente> tblProximosVencer;

    @FXML
    public void initialize() {
        cargarMetricas();
        cargarGrafico();
        cargarClientesProximosAVencer();
    }

    private void cargarMetricas() {
        try {
            Map<String, Integer> stats = DatabaseUtil.getEstadisticas();

            // Aquí deberías tener referencias a los controladores de las tarjetas
            // por ejemplo si usas fx:include con fx:id="cardClientes", deberías inyectar su controller
            // Esto se puede hacer con FXMLLoader y controladores personalizados, lo configuraremos luego

            System.out.println("Clientes activos: " + stats.get("clientes_activos"));
            System.out.println("Pagos hoy: " + stats.get("pagos_hoy"));
            System.out.println("Próximos a vencer: " + stats.get("por_vencer"));

        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudieron cargar las estadísticas");
        }
    }

    private void cargarGrafico() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Ingresos");

        try {
            ObservableList<PagoMensual> datos = DatabaseUtil.getIngresosMensuales();
            for (PagoMensual p : datos) {
                series.getData().add(new XYChart.Data<>(p.getMes(), p.getTotal()));
            }
            chartIngresos.getData().clear();
            chartIngresos.getData().add(series);
        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudo cargar el gráfico");
        }
    }

    private void cargarClientesProximosAVencer() {
        ObservableList<Cliente> clientes = FXCollections.observableArrayList();
        String sql = "SELECT nombres, telefono, fecha_vencimiento FROM clientes " +
                "WHERE fecha_vencimiento BETWEEN CURRENT_DATE AND date('now', '+7 days') " +
                "ORDER BY fecha_vencimiento LIMIT 10";

        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Cliente c = new Cliente();
                c.setNombres(rs.getString("nombres"));
                c.setTelefono(rs.getString("telefono"));
                c.setFechaVencimiento(rs.getString("fecha_vencimiento"));
                clientes.add(c);
            }

            // Configura columnas si aún no lo has hecho
            if (tblProximosVencer.getColumns().isEmpty()) {
                TableColumn<Cliente, String> colNombre = new TableColumn<>("Nombre");
                colNombre.setCellValueFactory(new PropertyValueFactory<>("nombres"));
                colNombre.setPrefWidth(150);

                TableColumn<Cliente, String> colTelefono = new TableColumn<>("Teléfono");
                colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
                colTelefono.setPrefWidth(100);

                TableColumn<Cliente, String> colVence = new TableColumn<>("Vence");
                colVence.setCellValueFactory(new PropertyValueFactory<>("fechaVencimiento"));
                colVence.setPrefWidth(100);

                tblProximosVencer.getColumns().addAll(colNombre, colTelefono, colVence);
            }

            tblProximosVencer.setItems(clientes);

        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudo cargar la tabla de clientes próximos a vencer");
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
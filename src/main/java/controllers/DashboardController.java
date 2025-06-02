package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.scene.Node;
import models.PagoMensual;
import util.DatabaseUtil;
import util.ReporteUtil;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML
    private AnchorPane cardClientes;

    @FXML
    private AnchorPane cardPagos;

    @FXML
    private AnchorPane cardVencimientos;

    @FXML
    private ListView<String> listaClientesProximosAVencer;

    @FXML
    private VBox miniGrafico;

    @FXML
    private BarChart<String, Number> ingresosChart;

    @FXML
    private CategoryAxis xAxis;

    @FXML
    private NumberAxis yAxis;

    @FXML
    private Label lblMensaje;

    private MetricCardController ctrlClientes;
    private MetricCardController ctrlPagos;
    private MetricCardController ctrlVencimientos;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            FXMLLoader loaderClientes = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
            Pane paneClientes = loaderClientes.load();
            ctrlClientes = loaderClientes.getController();
            ctrlClientes.setTitulo("Clientes Activos");
            cardClientes.getChildren().add(paneClientes);

            FXMLLoader loaderPagos = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
            Pane panePagos = loaderPagos.load();
            ctrlPagos = loaderPagos.getController();
            ctrlPagos.setTitulo("Pagos Recibidos");
            cardPagos.getChildren().add(panePagos);

            FXMLLoader loaderVencimientos = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
            Pane paneVencimientos = loaderVencimientos.load();
            ctrlVencimientos = loaderVencimientos.getController();
            ctrlVencimientos.setTitulo("Próximos a Vencer");
            cardVencimientos.getChildren().add(paneVencimientos);

            cargarDatosTarjetas();
            cargarClientesProximosAVencer();
            cargarMiniGraficoIngresos();

        } catch (Exception e) {
            e.printStackTrace();
            lblMensaje.setText("Error al inicializar el panel.");
        }
    }

    private void cargarDatosTarjetas() {
        // Clientes activos
        String sqlClientes = "SELECT COUNT(*) AS total FROM clientes WHERE activo = 1";
        // Pagos recibidos (ejemplo: pagos en el mes actual)
        String sqlPagos = "SELECT COUNT(*) AS total FROM pagos WHERE strftime('%Y-%m', fecha_pago) = strftime('%Y-%m', 'now')";
        // Clientes próximos a vencer
        String sqlVencimientos = "SELECT COUNT(*) AS total FROM clientes " +
                "WHERE fecha_vencimiento BETWEEN date('now') AND date('now', '+7 days')";

        try (Connection conn = DatabaseUtil.getConnection()) {
            // Clientes activos
            try (PreparedStatement ps = conn.prepareStatement(sqlClientes);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ctrlClientes.setValor(rs.getString("total"));
                }
            }
            // Pagos recibidos
            try (PreparedStatement ps = conn.prepareStatement(sqlPagos);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ctrlPagos.setValor(rs.getString("total"));
                }
            }
            // Próximos a vencer
            try (PreparedStatement ps = conn.prepareStatement(sqlVencimientos);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ctrlVencimientos.setValor(rs.getString("total"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al cargar datos métricos.");
        }
    }

    private void cargarClientesProximosAVencer() {
        listaClientesProximosAVencer.getItems().clear();

        String sql = "SELECT nombres, telefono, fecha_vencimiento FROM clientes " +
                "WHERE fecha_vencimiento BETWEEN date('now') AND date('now', '+7 days') " +
                "ORDER BY fecha_vencimiento LIMIT 10";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            boolean hayClientes = false;

            while (rs.next()) {
                hayClientes = true;
                String nombre = rs.getString("nombres");
                String telefono = rs.getString("telefono");
                String fechaVenc = rs.getString("fecha_vencimiento");

                String item = nombre + " - Tel: " + telefono + " - Vence: " + fechaVenc;
                listaClientesProximosAVencer.getItems().add(item);
            }

            if (!hayClientes) {
                lblMensaje.setText("No hay clientes próximos a vencer en los próximos 7 días.");
            } else {
                lblMensaje.setText("");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al cargar clientes próximos a vencer.");
        }
    }

    private void cargarMiniGraficoIngresos() {
        // Ejes
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Mes");
        yAxis.setLabel("Ingresos");

        // Gráficos
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barChart.setTitle(null);
        barChart.setPrefHeight(90);
        barChart.setVerticalGridLinesVisible(false);
        barChart.setHorizontalGridLinesVisible(false);
        barChart.setBarGap(2);
        barChart.setCategoryGap(10);
        barChart.setAnimated(false);
        barChart.setStyle("-fx-background-color: transparent;");

        XYChart.Series<String, Number> datos = new XYChart.Series<>();

        String sql = "SELECT strftime('%m', fecha_pago) AS mes, SUM(monto) as total " +
                "FROM pagos WHERE strftime('%Y', fecha_pago) = strftime('%Y', 'now') " +
                "GROUP BY mes ORDER BY mes";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String mes = switch (rs.getString("mes")) {
                    case "01" -> "Ene";
                    case "02" -> "Feb";
                    case "03" -> "Mar";
                    case "04" -> "Abr";
                    case "05" -> "May";
                    case "06" -> "Jun";
                    case "07" -> "Jul";
                    case "08" -> "Ago";
                    case "09" -> "Sep";
                    case "10" -> "Oct";
                    case "11" -> "Nov";
                    case "12" -> "Dic";
                    default -> "";
                };
                datos.getData().add(new XYChart.Data<>(mes, rs.getDouble("total")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al cargar datos del gráfico de ingresos.");
        }

        barChart.getData().add(datos);

        barChart.setOnMouseClicked(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ingresos_mensuales.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) miniGrafico.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Ingresos Mensuales");
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                lblMensaje.setText("Error al abrir ingresos mensuales.");
            }
        });

        miniGrafico.getChildren().add(barChart);
    }

    @FXML
    private void handleVerIngresos(MouseEvent event) {
        ingresosChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Ingresos Mensuales");

        try {
            List<PagoMensual> ingresosMensuales = DatabaseUtil.getIngresosMensuales();

            for (PagoMensual ingreso : ingresosMensuales) {
                String mes = ingreso.getMes();
                double total = ingreso.getTotal();
                series.getData().add(new XYChart.Data<>(mes, total));
            }

            ingresosChart.getData().add(series);
        } catch (SQLException e) {
            e.printStackTrace(); // O muestra un alert al usuario
            System.out.println("Error al obtener los ingresos mensuales: " + e.getMessage());
        }
    }

    public void handleExportarPDF() {
        ReporteUtil.generarReporteIngresos();
    }

    @FXML
    private void handleRegistroCliente(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/registro_cliente.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Registro de Cliente");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            lblMensaje.setText("No se pudo abrir el formulario de registro.");
        }
    }
}
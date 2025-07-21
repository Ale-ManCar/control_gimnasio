package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import models.PagoDetalle;
import models.PagoMensual;
import util.DatabaseUtil;

import java.net.URL;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IngresosMensualesController implements Initializable {

    @FXML private BarChart<String, Number> barChart;
    @FXML private PieChart pieChart;
    @FXML private TableView<PagoDetalle> tablaDetalles;
    @FXML private TableColumn<PagoDetalle, LocalDate> colFecha;
    @FXML private TableColumn<PagoDetalle, String> colCliente;
    @FXML private TableColumn<PagoDetalle, String> colMembresia;
    @FXML private TableColumn<PagoDetalle, Double> colMonto;
    @FXML private ComboBox<Integer> cbAnio;
    @FXML private Label lblTotalAnual;
    @FXML private Label lblPromedioMensual;
    @FXML private Label lblMejorMes;
    @FXML private Button btnExportarPDF;

    private ObservableList<PagoDetalle> detallesPagos = FXCollections.observableArrayList();
    private int anioActual = Year.now().getValue();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarTabla();
        configurarAnioSelector();
        cargarDatos(anioActual);
    }

    private void configurarTabla() {
        // Configurar las propiedades de las columnas
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("cliente"));
        colMembresia.setCellValueFactory(new PropertyValueFactory<>("membresia"));
        colMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));

        // Estilo para centrar todas las columnas
        String centerStyle = "-fx-alignment: CENTER;";
        colFecha.setStyle(centerStyle);
        colCliente.setStyle(centerStyle);
        colMembresia.setStyle(centerStyle);
        colMonto.setStyle(centerStyle);

        // Ajustar el tamaño de las columnas para ocupar todo el espacio
        colFecha.prefWidthProperty().bind(tablaDetalles.widthProperty().multiply(0.15));
        colCliente.prefWidthProperty().bind(tablaDetalles.widthProperty().multiply(0.43));
        colMembresia.prefWidthProperty().bind(tablaDetalles.widthProperty().multiply(0.20));
        colMonto.prefWidthProperty().bind(tablaDetalles.widthProperty().multiply(0.20));

        // Formatear columna de monto como moneda y centrar
        colMonto.setCellFactory(column -> new TableCell<PagoDetalle, Double>() {
            @Override
            protected void updateItem(Double monto, boolean empty) {
                super.updateItem(monto, empty);
                if (empty || monto == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("$%,.2f", monto));
                    setStyle(centerStyle + " -fx-font-weight: bold;");
                }
            }
        });

        // Formatear y centrar columna de fecha (ahora usando LocalDate)
        colFecha.setCellFactory(column -> new TableCell<PagoDetalle, LocalDate>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            protected void updateItem(LocalDate fecha, boolean empty) {
                super.updateItem(fecha, empty);
                if (empty || fecha == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(fecha.format(formatter));
                    setStyle(centerStyle);
                }
            }
        });

        // Formatear y centrar columna de cliente
        colCliente.setCellFactory(column -> new TableCell<PagoDetalle, String>() {
            @Override
            protected void updateItem(String cliente, boolean empty) {
                super.updateItem(cliente, empty);
                if (empty || cliente == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(cliente);
                    setStyle(centerStyle);
                }
            }
        });

        // Formatear y centrar columna de membresía
        colMembresia.setCellFactory(column -> new TableCell<PagoDetalle, String>() {
            @Override
            protected void updateItem(String membresia, boolean empty) {
                super.updateItem(membresia, empty);
                if (empty || membresia == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(membresia);
                    setStyle(centerStyle);
                }
            }
        });

        tablaDetalles.setItems(detallesPagos);

        // Estilo general para la tabla
        tablaDetalles.setStyle("-fx-font-size: 14px;");
    }

    // Método auxiliar para centrar y formatear columnas
    private <T> void centrarColumna(TableColumn<PagoDetalle, T> columna, Function<T, String> formateador) {
        columna.setCellFactory(column -> new TableCell<PagoDetalle, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(formateador.apply(item));
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });
    }

    private void configurarAnioSelector() {
        // Obtener años disponibles (desde 2023 hasta el año actual +1)
        int añoInicial = 2025;
        for (int año = añoInicial; año <= anioActual + 1; año++) {
            cbAnio.getItems().add(año);
        }
        cbAnio.setValue(anioActual);

        // Manejar cambio de año
        cbAnio.setOnAction(event -> cargarDatos(cbAnio.getValue()));
    }

    private void cargarDatos(int año) {
        cargarGraficoBarras(año);
        cargarGraficoCircular(año);
        cargarTablaDetalles(año);
        calcularMetricas(año);
    }

    private void cargarGraficoBarras(int año) {
        XYChart.Series<String, Number> datos = new XYChart.Series<>();
        datos.setName("Ingresos Mensuales");

        try {
            List<PagoMensual> ingresos = DatabaseUtil.getIngresosMensuales(año);

            // Estilo moderno para las barras
            String[] colores = {"#3498db", "#2ecc71", "#e74c3c", "#9b59b6", "#1abc9c",
                    "#f1c40f", "#e67e22", "#d35400", "#34495e", "#16a085",
                    "#8e44ad", "#27ae60"};

            for (int i = 0; i < ingresos.size(); i++) {
                PagoMensual ingreso = ingresos.get(i);
                String mes = obtenerNombreMes(ingreso.getMes());
                double total = ingreso.getTotal();

                XYChart.Data<String, Number> data = new XYChart.Data<>(mes, total);
                datos.getData().add(data);

                // Aplicar estilo personalizado a cada barra
                final int index = i;
                data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        newNode.setStyle("-fx-bar-fill: " + colores[index % colores.length] + ";");

                        // Efecto hover
                        newNode.setOnMouseEntered(e -> {
                            newNode.setStyle("-fx-bar-fill: derive(" + colores[index % colores.length] + ", 30%); " +
                                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 8, 0, 0, 0);");
                        });

                        newNode.setOnMouseExited(e -> {
                            newNode.setStyle("-fx-bar-fill: " + colores[index % colores.length] + ";");
                        });

                        // Tooltip con información detallada
                        Tooltip tooltip = new Tooltip(String.format("%s: $%,.2f", mes, total));
                        tooltip.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                        Tooltip.install(newNode, tooltip);
                    }
                });
            }

            barChart.getData().clear();
            barChart.getData().add(datos);

            // Estilo general del gráfico
            barChart.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 10px;" +
                            "-fx-padding: 15px;" +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);"
            );

            // Personalizar ejes
            barChart.getXAxis().setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            barChart.getYAxis().setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

            // Eliminar leyenda innecesaria
            barChart.setLegendVisible(false);

        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudieron cargar los datos del gráfico");
            e.printStackTrace();
        }
    }

    private void cargarGraficoCircular(int año) {
        try {
            ObservableList<PieChart.Data> datosPie = DatabaseUtil.getDistribucionMembresias(año);
            pieChart.setData(datosPie);

            // Estilo para las etiquetas
            pieChart.setLabelLineLength(15);
            pieChart.setLegendVisible(true);
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo cargar la distribución de membresías");
            e.printStackTrace();
        }
    }

    private void cargarTablaDetalles(int año) {
        try {
            detallesPagos.setAll(DatabaseUtil.getDetallesPagos(año));

            // Ordenar por fecha descendente
            detallesPagos.sort(Comparator.comparing(PagoDetalle::getFecha).reversed());
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudieron cargar los detalles de pagos");
            e.printStackTrace();
        }
    }

    private void calcularMetricas(int año) {
        try {
            double totalAnual = detallesPagos.stream()
                    .mapToDouble(PagoDetalle::getMonto)
                    .sum();

            double promedioMensual = totalAnual / 12;

            // Encontrar el mejor mes
            String mejorMes = "N/A";
            double maxMonto = 0;
            for (PagoMensual ingreso : DatabaseUtil.getIngresosMensuales(año)) {
                if (ingreso.getTotal() > maxMonto) {
                    maxMonto = ingreso.getTotal();
                    mejorMes = obtenerNombreMes(ingreso.getMes());
                }
            }

            lblTotalAnual.setText(String.format("$%,.2f", totalAnual));
            lblPromedioMensual.setText(String.format("$%,.2f", promedioMensual));
            lblMejorMes.setText(mejorMes + ": $" + String.format("%,.2f", maxMonto));

        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudieron calcular las métricas");
            e.printStackTrace();
        }
    }

    private String obtenerNombreMes(String mesNumero) {
        try {
            int mes = Integer.parseInt(mesNumero.split("-")[1]);
            return DateTimeFormatter.ofPattern("MMMM")
                    .format(LocalDate.of(2000, mes, 1))
                    .toUpperCase();
        } catch (Exception e) {
            return mesNumero;
        }
    }

    @FXML
    private void handleExportarPDF(ActionEvent event) {
        // Implementar lógica de exportación a PDF
        mostrarAlerta("Exportar PDF", "Función de exportación a PDF implementada próximamente");
    }

    @FXML
    private void handleVolver(ActionEvent event) {
        Stage stage = (Stage) barChart.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
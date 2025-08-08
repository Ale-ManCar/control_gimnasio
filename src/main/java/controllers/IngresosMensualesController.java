package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import models.Egreso;
import models.EgresoDetalle;
import models.PagoDetalle;
import models.PagoMensual;
import util.DatabaseUtil;
import util.EventBus;
import util.ReporteUtil;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
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
    @FXML private Button btnRegistrarEgreso;

    @FXML private TableView<EgresoDetalle> tablaEgresos;
    @FXML private TableColumn<EgresoDetalle, LocalDate> colFechaEgreso;
    @FXML private TableColumn<EgresoDetalle, String> colDescripcion;
    @FXML private TableColumn<EgresoDetalle, String> colCategoria;
    @FXML private TableColumn<EgresoDetalle, Double> colMontoEgreso;

    private ObservableList<PagoDetalle> detallesPagos = FXCollections.observableArrayList();
    private int anioActual = Year.now().getValue();
    private Consumer<EventBus.EventType> eventConsumer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarTabla();
        configurarAnioSelector();
        cargarDatos(anioActual);
        configurarTablaEgresos();
        configurarBotonEgreso();

        eventConsumer = eventType -> {
            if (eventType == EventBus.EventType.EGRESO_REGISTRADO) {
                Platform.runLater(() -> {
                    int selectedYear = cbAnio.getValue();
                    cargarDatos(selectedYear);
                });
            }
        };
        EventBus.registerListener(EventBus.EventType.EGRESO_REGISTRADO, eventConsumer);

        Platform.runLater(() -> {
            Stage stage = (Stage) barChart.getScene().getWindow();
            stage.setOnCloseRequest(e ->
                    EventBus.unregisterListener(EventBus.EventType.EGRESO_REGISTRADO, eventConsumer));
        });
    }

    private void configurarBotonEgreso() {
        btnRegistrarEgreso.setOnAction(e -> abrirRegistroEgreso());
    }

    private void abrirRegistroEgreso() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/registro_egreso.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Registrar Egreso");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configurarTabla() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("cliente"));
        colMembresia.setCellValueFactory(new PropertyValueFactory<>("membresia"));
        colMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));

        String centerStyle = "-fx-alignment: CENTER;";
        colFecha.setStyle(centerStyle);
        colCliente.setStyle(centerStyle);
        colMembresia.setStyle(centerStyle);
        colMonto.setStyle(centerStyle);

        colFecha.prefWidthProperty().bind(tablaDetalles.widthProperty().multiply(0.15));
        colCliente.prefWidthProperty().bind(tablaDetalles.widthProperty().multiply(0.52));
        colMembresia.prefWidthProperty().bind(tablaDetalles.widthProperty().multiply(0.15));
        colMonto.prefWidthProperty().bind(tablaDetalles.widthProperty().multiply(0.15));

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
        tablaDetalles.setStyle("-fx-font-size: 14px;");
    }

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
        int añoInicial = 2025;
        for (int año = añoInicial; año <= anioActual + 1; año++) {
            cbAnio.getItems().add(año);
        }
        cbAnio.setValue(anioActual);

        cbAnio.setOnAction(event -> cargarDatos(cbAnio.getValue()));
    }

    private void configurarTablaEgresos() {
        colFechaEgreso.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colMontoEgreso.setCellValueFactory(new PropertyValueFactory<>("monto"));

        colFechaEgreso.prefWidthProperty().bind(tablaEgresos.widthProperty().multiply(0.15));
        colDescripcion.prefWidthProperty().bind(tablaEgresos.widthProperty().multiply(0.52));
        colCategoria.prefWidthProperty().bind(tablaEgresos.widthProperty().multiply(0.15));
        colMontoEgreso.prefWidthProperty().bind(tablaEgresos.widthProperty().multiply(0.15));

        String centerStyle = "-fx-alignment: CENTER;";
        colFechaEgreso.setStyle(centerStyle);
        colDescripcion.setStyle(centerStyle);
        colCategoria.setStyle(centerStyle);
        colMontoEgreso.setStyle(centerStyle);

        colFechaEgreso.setCellFactory(column -> new TableCell<EgresoDetalle, LocalDate>() {
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

        colMontoEgreso.setCellFactory(column -> new TableCell<EgresoDetalle, Double>() {
            @Override
            protected void updateItem(Double monto, boolean empty) {
                super.updateItem(monto, empty);
                if (empty || monto == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("$%,.2f", monto));
                    setStyle(centerStyle + "-fx-font-weight: bold;");
                }
            }
        });

        colDescripcion.setCellFactory(column -> new TableCell<EgresoDetalle, String>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(value);
                    setStyle(centerStyle);
                }
            }
        });

        colCategoria.setCellFactory(column -> new TableCell<EgresoDetalle, String>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(value);
                    setStyle(centerStyle);
                }
            }
        });

        tablaEgresos.setStyle("-fx-font-size: 14px;");
    }

    private void cargarDatos(int año) {
        cargarGraficoBarras(año);
        cargarGraficoCircular(año);
        cargarTablaDetalles(año);
        calcularMetricas(año);
        cargarTablaEgresos(año);
    }

    private void cargarTablaEgresos(int año) {
        try {
            ObservableList<EgresoDetalle> egresos = DatabaseUtil.getDetallesEgresos(año);
            tablaEgresos.setItems(egresos);

            for (TableColumn<EgresoDetalle, ?> col : tablaEgresos.getColumns()) {
                col.setStyle("-fx-alignment: CENTER;");
            }
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudieron cargar los egresos");
            e.printStackTrace();
        }
    }

    private void cargarGraficoBarras(int año) {
        XYChart.Series<String, Number> datosIngresos = new XYChart.Series<>();
        datosIngresos.setName("Ingresos");

        XYChart.Series<String, Number> datosEgresos = new XYChart.Series<>();
        datosEgresos.setName("Egresos");

        try {
            List<PagoMensual> ingresos = DatabaseUtil.getIngresosMensuales(año);
            List<PagoMensual> egresos = DatabaseUtil.getEgresosMensuales(año);

            for (int i = 0; i < ingresos.size(); i++) {
                PagoMensual ingreso = ingresos.get(i);
                String mes = obtenerNombreMes(ingreso.getMes());

                double totalEgresos = 0;
                for (PagoMensual egreso : egresos) {
                    if (egreso.getMes().equals(ingreso.getMes())) {
                        totalEgresos = egreso.getTotal();
                        break;
                    }
                }

                datosIngresos.getData().add(new XYChart.Data<>(mes, ingreso.getTotal()));
                datosEgresos.getData().add(new XYChart.Data<>(mes, totalEgresos));
            }

            barChart.getData().clear();
            barChart.getData().addAll(datosIngresos, datosEgresos);

            barChart.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 10px;" +
                            "-fx-padding: 15px;" +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);"
            );

            barChart.getXAxis().setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            barChart.getYAxis().setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

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

            detallesPagos.sort(Comparator.comparing(PagoDetalle::getFecha).reversed());
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudieron cargar los detalles de pagos");
            e.printStackTrace();
        }
    }

    private void calcularMetricas(int año) {
        try {
            double totalIngresos = detallesPagos.stream()
                    .mapToDouble(PagoDetalle::getMonto)
                    .sum();

            double totalEgresos = DatabaseUtil.getTotalEgresosAnual(año);
            double totalAnual = totalIngresos - totalEgresos;
            double promedioMensual = totalAnual / 12;

            String mejorMes = "N/A";
            double maxUtilidad = Double.NEGATIVE_INFINITY;

            List<PagoMensual> ingresos = DatabaseUtil.getIngresosMensuales(año);
            List<PagoMensual> egresos = DatabaseUtil.getEgresosMensuales(año);

            for (PagoMensual ingreso : ingresos) {
                double totalEgresosMes = 0;
                for (PagoMensual egreso : egresos) {
                    if (egreso.getMes().equals(ingreso.getMes())) {
                        totalEgresosMes = egreso.getTotal();
                        break;
                    }
                }

                double utilidad = ingreso.getTotal() - totalEgresosMes;
                if (utilidad > maxUtilidad) {
                    maxUtilidad = utilidad;
                    mejorMes = obtenerNombreMes(ingreso.getMes());
                }
            }

            lblTotalAnual.setText(String.format("$%,.2f", totalAnual));
            lblPromedioMensual.setText(String.format("$%,.2f", promedioMensual));
            lblMejorMes.setText(mejorMes + ": $" + String.format("%,.2f", maxUtilidad));

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
        ReporteUtil.generarReporteFinanciero(8, 2025);
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
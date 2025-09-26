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
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import models.EgresoDetalle;
import models.IngresoData;
import models.PagoDetalle;
import util.DatabaseUtil;
import util.EventBus;
import util.ReporteUtil;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IngresosMensualesController implements Initializable {

    private enum ReportType {
        DIARIO("Diario"),
        SEMANAL("Semanal"),
        MENSUAL("Mensual"),
        ANUAL("Anual");

        private final String displayName;

        ReportType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @FXML private BarChart<String, Number> barChart;
    @FXML private PieChart pieChart;
    @FXML private TableView<PagoDetalle> tablaDetalles;
    @FXML private TableColumn<PagoDetalle, LocalDate> colFecha;
    @FXML private TableColumn<PagoDetalle, String> colCliente;
    @FXML private TableColumn<PagoDetalle, String> colMembresia;
    @FXML private TableColumn<PagoDetalle, Double> colMonto;
    @FXML private ComboBox<ReportType> cbTipoReporte;
    @FXML private ComboBox<Integer> cbAnio;
    @FXML private ComboBox<Month> cbMes;
    @FXML private DatePicker dpFecha;
    @FXML private DatePicker dpSemanaInicio;
    @FXML private DatePicker dpSemanaFin;
    @FXML private Label lblTituloGrafico;
    @FXML private Label lblTituloMetricas;
    @FXML private Label lblMetric1Title;
    @FXML private Label lblMetric1Value;
    @FXML private Label lblMetric2Title;
    @FXML private Label lblMetric2Value;
    @FXML private Label lblMetric3Title;
    @FXML private Label lblMetric3Value;
    @FXML private Button btnExportarPDF;
    @FXML private Button btnExportarExcel;
    @FXML private Button btnRegistrarEgreso;
    @FXML private TableView<EgresoDetalle> tablaEgresos;
    @FXML private TableColumn<EgresoDetalle, LocalDate> colFechaEgreso;
    @FXML private TableColumn<EgresoDetalle, String> colDescripcion;
    @FXML private TableColumn<EgresoDetalle, String> colCategoria;
    @FXML private TableColumn<EgresoDetalle, Double> colMontoEgreso;
    @FXML private HBox contenedorAnio;
    @FXML private HBox contenedorMes;
    @FXML private HBox contenedorDia;
    @FXML private HBox contenedorSemana;

    private final ObservableList<PagoDetalle> detallesPagos = FXCollections.observableArrayList();
    private final ObservableList<EgresoDetalle> detallesEgresos = FXCollections.observableArrayList();
    private final Locale localeEs = new Locale("es", "ES");
    private final DateTimeFormatter fechaLarga = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter fechaCorta = DateTimeFormatter.ofPattern("dd/MM");
    private int anioActual = Year.now().getValue();
    private Consumer<EventBus.EventType> eventConsumer;

    private ReportType reporteActual = ReportType.ANUAL;
    private LocalDate periodoInicio;
    private LocalDate periodoFin;
    private String ultimoMetric1Title;
    private String ultimoMetric2Title;
    private String ultimoMetric3Title;
    private String ultimoMetric3Value;
    private double ultimoTotal;
    private double ultimoPromedio;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarTablaPagos();
        configurarTablaEgresos();
        configurarTipoReporte();
        configurarAnioSelector();
        configurarMesSelector();
        configurarDatePickers();
        configurarBotones();

        tablaDetalles.setItems(detallesPagos);
        tablaEgresos.setItems(detallesEgresos);

        eventConsumer = eventType -> {
            if (eventType == EventBus.EventType.EGRESO_REGISTRADO) {
                Platform.runLater(this::actualizarReporte);
            }
        };
        EventBus.registerListener(EventBus.EventType.EGRESO_REGISTRADO, eventConsumer);

        Platform.runLater(() -> {
            Stage stage = (Stage) barChart.getScene().getWindow();
            if (stage != null) {
                stage.setOnCloseRequest(e ->
                        EventBus.unregisterListener(EventBus.EventType.EGRESO_REGISTRADO, eventConsumer));
            }
        });

        actualizarControlesVisibles();
        actualizarReporte();
    }

    private void configurarBotones() {
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

    private void configurarTipoReporte() {
        cbTipoReporte.getItems().setAll(ReportType.values());
        cbTipoReporte.setValue(ReportType.ANUAL);
        Callback<javafx.scene.control.ListView<ReportType>, javafx.scene.control.ListCell<ReportType>> cellFactory = list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(ReportType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        };
        cbTipoReporte.setCellFactory(cellFactory);
        cbTipoReporte.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(ReportType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        });
        cbTipoReporte.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                reporteActual = newVal;
                actualizarControlesVisibles();
                actualizarReporte();
            }
        });
    }

    private void configurarAnioSelector() {
        int añoInicial = 2025;
        for (int año = añoInicial; año <= anioActual + 1; año++) {
            cbAnio.getItems().add(año);
        }
        cbAnio.setValue(anioActual);
        cbAnio.valueProperty().addListener((obs, old, val) -> actualizarReporte());
    }

    private void configurarMesSelector() {
        cbMes.getItems().setAll(Month.values());
        cbMes.setValue(LocalDate.now().getMonth());
        Callback<javafx.scene.control.ListView<Month>, javafx.scene.control.ListCell<Month>> cellFactory = list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Month item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatearMes(item));
            }
        };
        cbMes.setCellFactory(cellFactory);
        cbMes.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Month item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatearMes(item));
            }
        });
        cbMes.valueProperty().addListener((obs, old, val) -> actualizarReporte());
    }

    private void configurarDatePickers() {
        dpFecha.setValue(LocalDate.now());
        dpFecha.valueProperty().addListener((obs, old, val) -> actualizarReporte());

        LocalDate inicioSemana = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        dpSemanaInicio.setValue(inicioSemana);
        dpSemanaFin.setValue(inicioSemana.plusDays(6));

        dpSemanaInicio.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                dpSemanaFin.setValue(val.plusDays(6));
                actualizarReporte();
            }
        });
        dpSemanaFin.valueProperty().addListener((obs, old, val) -> {
            if (val != null && dpSemanaInicio.getValue() != null) {
                if (!val.equals(dpSemanaInicio.getValue().plusDays(6))) {
                    dpSemanaInicio.setValue(val.minusDays(6));
                    return;
                }
                actualizarReporte();
            }
        });
    }

    private void actualizarControlesVisibles() {
        ReportType tipo = cbTipoReporte.getValue();
        boolean mostrarAnio = tipo == ReportType.MENSUAL || tipo == ReportType.ANUAL;
        boolean mostrarMes = tipo == ReportType.MENSUAL;
        boolean mostrarDia = tipo == ReportType.DIARIO;
        boolean mostrarSemana = tipo == ReportType.SEMANAL;

        configurarVisibilidad(contenedorAnio, mostrarAnio);
        configurarVisibilidad(contenedorMes, mostrarMes);
        configurarVisibilidad(contenedorDia, mostrarDia);
        configurarVisibilidad(contenedorSemana, mostrarSemana);
    }

    private void configurarVisibilidad(HBox contenedor, boolean visible) {
        contenedor.setVisible(visible);
        contenedor.setManaged(visible);
    }

    private void configurarTablaPagos() {
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

        colMonto.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double monto, boolean empty) {
                super.updateItem(monto, empty);
                if (empty || monto == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(formatearMoneda(monto));
                    setStyle(centerStyle + " -fx-font-weight: bold;");
                }
            }
        });

        colFecha.setCellFactory(column -> new TableCell<>() {
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

        colCliente.setCellFactory(column -> new TableCell<>() {
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

        colMembresia.setCellFactory(column -> new TableCell<>() {
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

        tablaDetalles.setStyle("-fx-font-size: 14px;");
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

        colFechaEgreso.setCellFactory(column -> new TableCell<>() {
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

        colMontoEgreso.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double monto, boolean empty) {
                super.updateItem(monto, empty);
                if (empty || monto == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(formatearMoneda(monto));
                    setStyle(centerStyle + "-fx-font-weight: bold;");
                }
            }
        });

        colDescripcion.setCellFactory(column -> new TableCell<>() {
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

        colCategoria.setCellFactory(column -> new TableCell<>() {
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

    private void actualizarReporte() {
        switch (reporteActual) {
            case DIARIO -> cargarReporteDiario();
            case SEMANAL -> cargarReporteSemanal();
            case MENSUAL -> cargarReporteMensual();
            case ANUAL -> cargarReporteAnual();
        }
    }

    private void cargarReporteDiario() {
        LocalDate fecha = dpFecha.getValue() != null ? dpFecha.getValue() : LocalDate.now();
        dpFecha.setValue(fecha);
        periodoInicio = fecha;
        periodoFin = fecha;

        lblTituloGrafico.setText("Ingresos del " + fecha.format(fechaLarga));
        lblTituloMetricas.setText("Métricas del día");
        actualizarBarChart(() -> DatabaseUtil.getIngresosPorDia(fecha),
                "Ingresos por tipo", Function.identity());
        actualizarPieChart(fecha, fecha);
        actualizarTablaPagos(obtenerPagos(fecha, fecha), Comparator.comparing(PagoDetalle::getFecha).reversed());
        actualizarEgresos(fecha, fecha);

        double total = calcularTotalIngresos();
        actualizarMetricas("Total del día", total, "Promedio diario", total, null, null);
    }

    private void cargarReporteSemanal() {
        LocalDate inicio = dpSemanaInicio.getValue();
        if (inicio == null) {
            inicio = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            dpSemanaInicio.setValue(inicio);
        }
        LocalDate fin = dpSemanaFin.getValue();
        if (fin == null) {
            fin = inicio.plusDays(6);
            dpSemanaFin.setValue(fin);
        }
        final LocalDate inicioSemana = inicio;
        final LocalDate finSemana = fin;
        periodoInicio = inicioSemana;
        periodoFin = finSemana;

        lblTituloGrafico.setText("Ingresos del " + inicioSemana.format(fechaLarga) + " al " + finSemana.format(fechaLarga));
        lblTituloMetricas.setText("Métricas de la semana");
        actualizarBarChart(() -> DatabaseUtil.getIngresosPorSemana(inicioSemana, finSemana),
                "Ingresos diarios", etiqueta -> LocalDate.parse(etiqueta).format(fechaCorta));
        actualizarPieChart(inicioSemana, finSemana);
        actualizarTablaPagos(obtenerPagos(inicioSemana, finSemana), Comparator.comparing(PagoDetalle::getFecha).reversed());
        actualizarEgresos(inicioSemana, finSemana);

        double total = calcularTotalIngresos();
        double promedio = total / 7.0;
        actualizarMetricas("Total de la semana", total, "Promedio semanal", promedio, null, null);
    }

    private void cargarReporteMensual() {
        Integer año = cbAnio.getValue() != null ? cbAnio.getValue() : anioActual;
        cbAnio.setValue(año);
        Month mes = cbMes.getValue() != null ? cbMes.getValue() : LocalDate.now().getMonth();
        cbMes.setValue(mes);

        LocalDate inicio = LocalDate.of(año, mes, 1);
        LocalDate fin = inicio.withDayOfMonth(inicio.lengthOfMonth());
        periodoInicio = inicio;
        periodoFin = fin;

        lblTituloGrafico.setText("Ingresos de " + formatearMes(mes) + " " + año);
        lblTituloMetricas.setText("Métricas del mes");
        actualizarBarChart(() -> DatabaseUtil.getIngresosPorMes(año, mes.getValue()),
                "Ingresos diarios", etiqueta -> LocalDate.parse(etiqueta).format(fechaCorta));
        actualizarPieChart(inicio, fin);
        actualizarTablaPagos(obtenerPagos(inicio, fin), Comparator.comparing(PagoDetalle::getFecha).reversed());
        actualizarEgresos(inicio, fin);

        double total = calcularTotalIngresos();
        double promedio = total / inicio.lengthOfMonth();
        actualizarMetricas("Total mensual", total, "Promedio mensual", promedio, null, null);
    }

    private void cargarReporteAnual() {
        Integer año = cbAnio.getValue() != null ? cbAnio.getValue() : anioActual;
        cbAnio.setValue(año);
        LocalDate inicio = LocalDate.of(año, Month.JANUARY, 1);
        LocalDate fin = LocalDate.of(año, Month.DECEMBER, 31);
        periodoInicio = inicio;
        periodoFin = fin;

        lblTituloGrafico.setText("Ingresos del año " + año);
        lblTituloMetricas.setText("Métricas anuales");
        actualizarBarChart(() -> DatabaseUtil.getIngresosPorAnio(año),
                "Ingresos mensuales", this::formatearMesDesdeEtiqueta);
        actualizarPieChart(inicio, fin);
        List<PagoDetalle> pagosAnuales = obtenerPagos(inicio, fin);
        List<PagoDetalle> pagosAgrupados = agruparPagosPorMes(pagosAnuales, año);
        actualizarTablaPagos(pagosAgrupados, Comparator.comparing(PagoDetalle::getFecha).reversed());
        actualizarEgresos(inicio, fin);

        double total = pagosAnuales.stream().mapToDouble(PagoDetalle::getMonto).sum();
        double promedio = total / 12.0;
        String mejorMes = determinarMejorMes(año);
        actualizarMetricas("Total anual", total, "Promedio mensual", promedio, "Mejor mes", mejorMes);
    }

    private void actualizarBarChart(ThrowingSupplier<List<IngresoData>> supplier,
                                    String serieNombre,
                                    Function<String, String> etiquetaFormatter) {
        try {
            List<IngresoData> datos = supplier.get();
            XYChart.Series<String, Number> serie = new XYChart.Series<>();
            serie.setName(serieNombre);
            serie.getData().addAll(datos.stream()
                    .map(d -> new XYChart.Data<String, Number>(
                            etiquetaFormatter.apply(d.getEtiqueta()), d.getTotal()))
                    .collect(Collectors.toList()));

            barChart.getData().setAll(serie);
            CategoryAxis xAxis = (CategoryAxis) barChart.getXAxis();
            switch (reporteActual) {
                case DIARIO -> xAxis.setLabel("Tipo de ingreso");
                case SEMANAL -> xAxis.setLabel("Día");
                case MENSUAL -> xAxis.setLabel("Día");
                case ANUAL -> xAxis.setLabel("Mes");
            }
        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudieron cargar los datos del gráfico");
            e.printStackTrace();
        }
    }

    private List<PagoDetalle> obtenerPagos(LocalDate inicio, LocalDate fin) {
        try {
            return DatabaseUtil.getDetallesPagosEntre(inicio, fin);
        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudieron cargar los detalles de pagos");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void actualizarPieChart(LocalDate inicio, LocalDate fin) {
        try {
            ObservableList<PieChart.Data> datosPie = DatabaseUtil.getDistribucionMembresias(inicio, fin);
            pieChart.setData(datosPie);
            pieChart.setLabelLineLength(15);
            pieChart.setLegendVisible(true);
        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudo cargar la distribución de membresías");
            e.printStackTrace();
        }
    }

    private void actualizarTablaPagos(List<PagoDetalle> pagos, Comparator<PagoDetalle> comparator) {
        detallesPagos.setAll(pagos);
        if (comparator != null) {
            detallesPagos.sort(comparator);
        }
    }

    private void actualizarEgresos(LocalDate inicio, LocalDate fin) {
        try {
            detallesEgresos.setAll(DatabaseUtil.getDetallesEgresosEntre(inicio, fin));
            for (TableColumn<EgresoDetalle, ?> col : tablaEgresos.getColumns()) {
                col.setStyle("-fx-alignment: CENTER;");
            }
        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudieron cargar los egresos");
            e.printStackTrace();
        }
    }

    private double calcularTotalIngresos() {
        return detallesPagos.stream().mapToDouble(PagoDetalle::getMonto).sum();
    }

    private void actualizarMetricas(String titulo1, double valor1,
                                    String titulo2, double valor2,
                                    String titulo3, String valor3) {
        lblMetric1Title.setText(titulo1);
        lblMetric1Value.setText(formatearMoneda(valor1));
        lblMetric2Title.setText(titulo2);
        lblMetric2Value.setText(formatearMoneda(valor2));

        boolean mostrarTercera = titulo3 != null && valor3 != null && !valor3.isBlank();
        if (mostrarTercera) {
            lblMetric3Title.setText(titulo3);
            lblMetric3Value.setText(valor3);
        } else {
            lblMetric3Title.setText("");
            lblMetric3Value.setText("");
        }
        lblMetric3Title.setVisible(mostrarTercera);
        lblMetric3Title.setManaged(mostrarTercera);
        lblMetric3Value.setVisible(mostrarTercera);
        lblMetric3Value.setManaged(mostrarTercera);

        ultimoMetric1Title = titulo1;
        ultimoMetric2Title = titulo2;
        ultimoMetric3Title = titulo3;
        ultimoMetric3Value = valor3;
        ultimoTotal = valor1;
        ultimoPromedio = valor2;
    }

    private List<PagoDetalle> agruparPagosPorMes(List<PagoDetalle> pagos, int año) {
        Map<Month, Map<String, Double>> agrupado = new LinkedHashMap<>();
        pagos.forEach(pago -> {
            Month mes = pago.getFecha().getMonth();
            agrupado
                    .computeIfAbsent(mes, m -> new LinkedHashMap<>())
                    .merge(pago.getMembresia(), pago.getMonto(), Double::sum);
        });

        List<PagoDetalle> resultado = new ArrayList<>();
        agrupado.forEach((mes, mapaMembresias) -> mapaMembresias.forEach((membresia, total) ->
                resultado.add(new PagoDetalle(LocalDate.of(año, mes, 1),
                        formatearMes(mes), 0, membresia, total))));
        return resultado;
    }

    private String determinarMejorMes(int año) {
        try {
            List<IngresoData> datos = DatabaseUtil.getIngresosPorAnio(año);
            return datos.stream()
                    .max(Comparator.comparingDouble(IngresoData::getTotal))
                    .map(data -> formatearMesDesdeEtiqueta(data.getEtiqueta())
                            + " (" + formatearMoneda(data.getTotal()) + ")")
                    .orElse("N/A");
        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudieron calcular las métricas");
            e.printStackTrace();
            return "N/A";
        }
    }

    private String formatearMes(Month mes) {
        return mes.getDisplayName(TextStyle.FULL, localeEs).toUpperCase(localeEs);
    }

    private String formatearMesDesdeEtiqueta(String etiqueta) {
        try {
            if (reporteActual == ReportType.ANUAL) {
                String[] partes = etiqueta.split("-");
                int mesNumero = Integer.parseInt(partes[1]);
                return formatearMes(Month.of(mesNumero));
            }
            LocalDate fecha = LocalDate.parse(etiqueta);
            return formatearMes(fecha.getMonth()) + " " + fecha.getDayOfMonth();
        } catch (Exception e) {
            return etiqueta;
        }
    }

    private String formatearMoneda(double monto) {
        return String.format(localeEs, "$%,.2f", monto);
    }

    @FXML
    private void handleExportarPDF(ActionEvent event) {
        if (periodoInicio == null || periodoFin == null) {
            mostrarAlerta("Periodo no válido", "Selecciona un rango de fechas antes de exportar.");
            return;
        }

        String titulo;
        String rango;
        switch (reporteActual) {
            case DIARIO -> {
                titulo = "Reporte de ingresos diario";
                rango = periodoInicio.format(fechaLarga);
            }
            case SEMANAL -> {
                titulo = "Reporte de ingresos semanal";
                rango = periodoInicio.format(fechaLarga) + " al " + periodoFin.format(fechaLarga);
            }
            case MENSUAL -> {
                titulo = "Reporte de ingresos mensual";
                rango = formatearMes(periodoInicio.getMonth()) + " " + periodoInicio.getYear();
            }
            case ANUAL -> {
                titulo = "Reporte de ingresos anual";
                rango = String.valueOf(periodoInicio.getYear());
            }
            default -> {
                titulo = "Reporte de ingresos";
                rango = periodoInicio.format(fechaLarga) + " - " + periodoFin.format(fechaLarga);
            }
        }

        boolean generado = ReporteUtil.generarReporteIngresos(periodoInicio, periodoFin, titulo, rango);
        if (!generado) {
            mostrarAlerta("Sin datos", "No se encontraron movimientos para el periodo seleccionado.");
        }
    }

    @FXML
    private void handleExportarExcel(ActionEvent event) {
        exportarReporteCsv();
    }

    private void exportarReporteCsv() {
        String extension = ".csv";
        String prefijo = "reporte_ingresos_excel";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String nombreArchivo = prefijo + "_" + reporteActual.name().toLowerCase() + "_" + timestamp + extension;
        Path destino = Paths.get(nombreArchivo);

        try {
            Files.writeString(destino, construirContenidoReporteCsv());
            mostrarAlerta("Exportación exitosa", "Reporte exportado en: " + destino.toAbsolutePath());
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo exportar el reporte");
            e.printStackTrace();
        }
    }

    private String construirContenidoReporteCsv() {
        String separador = ";";
        StringBuilder builder = new StringBuilder();
        builder.append("Tipo de reporte: ").append(reporteActual).append('\n');
        if (periodoInicio != null && periodoFin != null) {
            builder.append("Periodo: ")
                    .append(periodoInicio.format(fechaLarga))
                    .append(" - ")
                    .append(periodoFin.format(fechaLarga))
                    .append('\n');
        }
        builder.append(ultimoMetric1Title).append(':').append(separador).append(formatearMoneda(ultimoTotal)).append('\n');
        builder.append(ultimoMetric2Title).append(':').append(separador).append(formatearMoneda(ultimoPromedio)).append('\n');
        if (ultimoMetric3Title != null && ultimoMetric3Value != null && !ultimoMetric3Value.isBlank()) {
            builder.append(ultimoMetric3Title).append(':').append(separador).append(ultimoMetric3Value).append('\n');
        }
        builder.append('\n');
        builder.append("Fecha").append(separador).append("Detalle").append(separador)
                .append("Membresía").append(separador).append("Monto").append('\n');
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        detallesPagos.forEach(pago -> builder.append(pago.getFecha().format(formatter)).append(separador)
                .append(pago.getCliente()).append(separador)
                .append(pago.getMembresia()).append(separador)
                .append(formatearMoneda(pago.getMonto())).append('\n'));
        return builder.toString();
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

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws SQLException;
    }
}
package controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import models.Equipo;
import models.Role;
import models.User;
import util.AuditoriaUtil;
import util.DatabaseUtil;
import util.ReporteUtil;
import util.SessionManager;
import util.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

public class EquiposAdminController implements Initializable {

    private static final Locale LOCALE_ES = new Locale("es", "ES");
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd MMM yyyy", LOCALE_ES);
    private static final int DIAS_AVISO_MANTENIMIENTO = 7;

    @FXML private TableView<Equipo> tablaEquipos;
    @FXML private TableColumn<Equipo, String> colNombre;
    @FXML private TableColumn<Equipo, String> colEstado;
    @FXML private TableColumn<Equipo, String> colMarca;
    @FXML private TableColumn<Equipo, Integer> colCantidad;
    @FXML private TableColumn<Equipo, String> colPeso;
    @FXML private TableColumn<Equipo, String> colUltimoMantenimiento;
    @FXML private TableColumn<Equipo, String> colProximoMantenimiento;
    @FXML private ComboBox<String> filtroEstado;
    @FXML private ComboBox<String> filtroTipo;
    @FXML private Label lblMensaje;

    private final ObservableList<Equipo> equipos = FXCollections.observableArrayList();
    private FilteredList<Equipo> equiposFiltrados;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (!SessionManager.tienePermiso(Role.ADMIN)) {
            if (lblMensaje != null) {
                lblMensaje.setText("Acceso denegado");
            }
            if (tablaEquipos != null) {
                tablaEquipos.setDisable(true);
            }
            return;
        }
        configurarTabla();
        equiposFiltrados = new FilteredList<>(equipos, equipo -> true);
        SortedList<Equipo> sorted = new SortedList<>(equiposFiltrados);
        sorted.comparatorProperty().bind(tablaEquipos.comparatorProperty());
        tablaEquipos.setItems(sorted);
        cargarEquipos();
        configurarFiltros();
    }

    private void configurarTabla() {
        tablaEquipos.setPlaceholder(crearPlaceholderTabla());

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colMarca.setCellValueFactory(new PropertyValueFactory<>("marca"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPeso.setCellValueFactory(new PropertyValueFactory<>("peso"));
        colUltimoMantenimiento.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatearFecha(cellData.getValue().getFechaUltimoMantenimiento())));
        colProximoMantenimiento.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatearFecha(cellData.getValue().getProximoMantenimiento())));

        centrarColumnas(colEstado, colCantidad, colPeso, colUltimoMantenimiento, colProximoMantenimiento);

        configurarCeldaNombre();
        configurarCeldaEstado();
        configurarCeldaMarca();
        configurarCeldaCantidad();
        configurarCeldaPeso();
        configurarCeldaMantenimiento(colUltimoMantenimiento, false);
        configurarCeldaMantenimiento(colProximoMantenimiento, true);

        tablaEquipos.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Equipo equipo, boolean empty) {
                super.updateItem(equipo, empty);
                getStyleClass().removeAll("row-critical", "row-warning");
                if (empty || equipo == null) {
                    setTooltip(null);
                } else if (equipo.isEstadoCritico() || equipo.needsMaintenance(LocalDate.now())) {
                    getStyleClass().add("row-critical");
                    setTooltip(new Tooltip("Mantenimiento vencido o estado crítico"));
                } else if (equipo.maintenanceDueSoon(LocalDate.now(), DIAS_AVISO_MANTENIMIENTO)) {
                    getStyleClass().add("row-warning");
                    setTooltip(new Tooltip("Mantenimiento próximo"));
                } else {
                    setTooltip(null);
                }
            }
        });
    }

    private Label crearPlaceholderTabla() {
        Label placeholder = new Label("No se encontraron equipos con los filtros actuales");
        placeholder.setWrapText(true);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        placeholder.setStyle("-fx-text-fill: rgba(148,163,184,0.75); -fx-font-size: 13px;");
        placeholder.setMaxWidth(Double.MAX_VALUE);
        return placeholder;
    }

    private void configurarCeldaNombre() {
        colNombre.setCellFactory(column -> new TableCell<>() {
            private final Label titulo = new Label();
            private final Label subtitulo = new Label();
            private final VBox contenedor = new VBox(titulo, subtitulo);

            {
                contenedor.getStyleClass().add("equipo-name-cell");
                titulo.getStyleClass().setAll("label", "equipo-name-cell__titulo");
                subtitulo.getStyleClass().setAll("label", "equipo-name-cell__subtitulo");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(String nombre, boolean empty) {
                super.updateItem(nombre, empty);
                if (empty || nombre == null || nombre.isBlank()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                titulo.setText(nombre);
                Equipo equipo = obtenerEquipoFila(this);
                String tipo = equipo != null ? equipo.getTipo() : null;
                boolean mostrarTipo = tipo != null && !tipo.isBlank();
                subtitulo.setText(mostrarTipo ? tipo : "");
                subtitulo.setVisible(mostrarTipo);
                subtitulo.setManaged(mostrarTipo);
                setGraphic(contenedor);
                setText(null);
            }
        });
    }

    private void configurarCeldaEstado() {
        colEstado.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null || estado.isBlank()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(estado.trim().toUpperCase(Locale.ROOT));
                setGraphic(null);
            }
        });
    }

    private void configurarCeldaMarca() {
        colMarca.setCellFactory(column -> new TableCell<>() {
            private final Label marca = new Label();
            private final Label modelo = new Label();
            private final VBox contenedor = new VBox(marca, modelo);

            {
                contenedor.getStyleClass().add("marca-modelo-cell");
                marca.getStyleClass().setAll("label", "marca-modelo-cell__marca");
                modelo.getStyleClass().setAll("label", "marca-modelo-cell__modelo");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(String valorMarca, boolean empty) {
                super.updateItem(valorMarca, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                marca.setText((valorMarca == null || valorMarca.isBlank()) ? "-" : valorMarca);
                Equipo equipo = obtenerEquipoFila(this);
                String valorModelo = equipo != null ? equipo.getModelo() : null;
                boolean mostrarModelo = valorModelo != null && !valorModelo.isBlank();
                modelo.setText(mostrarModelo ? valorModelo : "");
                modelo.setVisible(mostrarModelo);
                modelo.setManaged(mostrarModelo);
                setGraphic(contenedor);
                setText(null);
            }
        });
    }

    private void configurarCeldaCantidad() {
        colCantidad.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer cantidad, boolean empty) {
                super.updateItem(cantidad, empty);
                if (empty || cantidad == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(String.valueOf(cantidad));
                setGraphic(null);
            }
        });
    }

    private void configurarCeldaPeso() {
        colPeso.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String peso, boolean empty) {
                super.updateItem(peso, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(peso == null || peso.isBlank() ? "-" : peso + " kg");
                }
                setGraphic(null);
            }
        });
    }

    private void configurarCeldaMantenimiento(TableColumn<Equipo, String> columna, boolean esProximo) {
        columna.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String fechaTexto, boolean empty) {
                super.updateItem(fechaTexto, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Equipo equipo = obtenerEquipoFila(this);
                if (equipo == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                String textoNormalizado = (fechaTexto == null || fechaTexto.isBlank() || "-".equals(fechaTexto))
                        ? (esProximo ? "SIN PLAN" : "SIN REGISTRO")
                        : fechaTexto;

                setText(textoNormalizado);
                setGraphic(null);
            }
        });
    }

    private Equipo obtenerEquipoFila(TableCell<Equipo, ?> celda) {
        if (celda == null || celda.getTableView() == null) {
            return null;
        }
        int indice = celda.getIndex();
        if (indice < 0 || indice >= celda.getTableView().getItems().size()) {
            return null;
        }
        return celda.getTableView().getItems().get(indice);
    }

    private void centrarColumnas(TableColumn<?, ?>... columnas) {
        for (TableColumn<?, ?> columna : columnas) {
            columna.setStyle("-fx-alignment: CENTER;");
        }
    }

    private void configurarFiltros() {
        filtroEstado.getItems().setAll("Todos");
        filtroTipo.getItems().setAll("Todos", "Máquina Estática", "Equipo con Peso");
        actualizarOpcionesFiltros();

        filtroEstado.setValue("Todos");
        filtroTipo.setValue("Todos");

        filtroEstado.setOnAction(event -> aplicarFiltros());
        filtroTipo.setOnAction(event -> aplicarFiltros());
    }

    private void actualizarOpcionesFiltros() {
        Set<String> estados = new HashSet<>();
        for (Equipo equipo : equipos) {
            if (equipo.getEstado() != null && !equipo.getEstado().isBlank()) {
                estados.add(equipo.getEstado());
            }
        }
        filtroEstado.getItems().setAll("Todos");
        filtroEstado.getItems().addAll(estados.stream().sorted().toList());
    }

    private void aplicarFiltros() {
        String estadoSeleccionado = filtroEstado.getValue();
        String tipoSeleccionado = filtroTipo.getValue();
        equiposFiltrados.setPredicate(equipo -> {
            boolean coincideEstado = estadoSeleccionado == null || "Todos".equalsIgnoreCase(estadoSeleccionado)
                    || equipo.getEstado().equalsIgnoreCase(estadoSeleccionado);
            boolean coincideTipo = tipoSeleccionado == null || "Todos".equalsIgnoreCase(tipoSeleccionado)
                    || equipo.getTipo().equalsIgnoreCase(tipoSeleccionado);
            return coincideEstado && coincideTipo;
        });
    }

    @FXML
    private void handleLimpiarFiltros() {
        filtroEstado.setValue("Todos");
        filtroTipo.setValue("Todos");
        aplicarFiltros();
    }

    private void cargarEquipos() {
        try {
            equipos.setAll(DatabaseUtil.listarEquipos());
            actualizarOpcionesFiltros();
            aplicarFiltros();
            if (lblMensaje != null) {
                lblMensaje.setText("Equipos cargados: " + equipos.size());
            }
        } catch (SQLException e) {
            mostrarError("Error de base de datos", "No se pudieron cargar los equipos.");
        }
    }

    @FXML
    private void handleRegistrarEquipo() {
        Dialog<Equipo> dialogo = crearDialogoEquipo(null);
        Optional<Equipo> resultado = dialogo.showAndWait();
        resultado.ifPresent(equipo -> {
            try {
                DatabaseUtil.insertarEquipo(equipo);
                registrarAuditoria("REGISTRO_EQUIPO", "Se registró el equipo " + equipo.getNombre());
                cargarEquipos();
                mostrarInformacion("Éxito", "Equipo registrado correctamente.");
            } catch (SQLException ex) {
                mostrarError("Error", "No se pudo registrar el equipo.");
            }
        });
    }

    @FXML
    private void handleEditarEquipo() {
        Equipo seleccionado = tablaEquipos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAdvertencia("Seleccione un equipo", "Debe seleccionar un equipo para editar.");
            return;
        }
        Dialog<Equipo> dialogo = crearDialogoEquipo(seleccionado);
        Optional<Equipo> resultado = dialogo.showAndWait();
        resultado.ifPresent(equipoActualizado -> {
            try {
                DatabaseUtil.actualizarEquipo(equipoActualizado);
                registrarAuditoria("ACTUALIZACION_EQUIPO", "Se actualizó el equipo " + equipoActualizado.getNombre());
                cargarEquipos();
                mostrarInformacion("Éxito", "Equipo actualizado correctamente.");
            } catch (SQLException ex) {
                mostrarError("Error", "No se pudo actualizar el equipo.");
            }
        });
    }

    @FXML
    private void handleEliminarEquipo() {
        Equipo seleccionado = tablaEquipos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAdvertencia("Seleccione un equipo", "Debe seleccionar un equipo para eliminar.");
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Desea eliminar el equipo seleccionado?");
        confirmacion.setContentText(seleccionado.getNombre());
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            try {
                DatabaseUtil.eliminarEquipo(seleccionado.getId());
                registrarAuditoria("ELIMINACION_EQUIPO", "Se eliminó el equipo " + seleccionado.getNombre());
                cargarEquipos();
                mostrarInformacion("Éxito", "Equipo eliminado correctamente.");
            } catch (SQLException ex) {
                mostrarError("Error", "No se pudo eliminar el equipo.");
            }
        }
    }

    @FXML
    private void handleGenerarReporte() {
        try {
            ReporteUtil.generarReporteEquipos(7);
            registrarAuditoria("REPORTE_EQUIPOS", "Se generó el reporte de equipos críticos");
        } catch (Exception ex) {
            mostrarError("Error", "No se pudo generar el reporte.");
        }
    }

    private Dialog<Equipo> crearDialogoEquipo(Equipo equipoExistente) {
        boolean esEdicion = equipoExistente != null;
        Dialog<Equipo> dialogo = new Dialog<>();
        dialogo.setTitle(esEdicion ? "Editar equipo" : "Registrar equipo");
        dialogo.setHeaderText(null);

        ButtonType btnGuardar = new ButtonType(esEdicion ? "Actualizar" : "Guardar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        DialogPane dialogPane = dialogo.getDialogPane();
        dialogPane.getButtonTypes().addAll(btnGuardar, btnCancelar);
        dialogPane.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-dark-pane");
        dialogPane.setPrefSize(900, 600);
        dialogPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        TextField txtNombre = estilizarCampo(new TextField());
        aplicarFormatoMayusculas(txtNombre);
        txtNombre.setPromptText("Nombre del equipo");
        ComboBox<String> cbTipo = estilizarCampo(new ComboBox<>());
        cbTipo.getItems().addAll("Máquina Estática", "Equipo con Peso");
        cbTipo.setPromptText("Tipo de equipo");
        cbTipo.getSelectionModel().selectFirst();
        if (cbTipo.getEditor() != null) {
            cbTipo.getEditor().getStyleClass().add("dialog-field");
        }
        ComboBox<String> cbEstado = estilizarCampo(new ComboBox<>(FXCollections.observableArrayList(
                "OPERATIVO", "MANTENIMIENTO", "CRITICO", "FUERA DE SERVICIO")));
        cbEstado.setEditable(true);
        cbEstado.getSelectionModel().selectFirst();
        if (cbEstado.getEditor() != null) {
            cbEstado.getEditor().getStyleClass().add("dialog-field");
            aplicarFormatoMayusculas(cbEstado.getEditor());
        }
        Spinner<Integer> spCantidad = estilizarCampo(new Spinner<>(0, Integer.MAX_VALUE, 0));
        spCantidad.setEditable(true);
        spCantidad.getEditor().getStyleClass().add("dialog-field");
        TextField txtMarca = estilizarCampo(new TextField());
        aplicarFormatoMayusculas(txtMarca);
        txtMarca.setPromptText("Marca");
        TextField txtModelo = estilizarCampo(new TextField());
        aplicarFormatoMayusculas(txtModelo);
        txtModelo.setPromptText("Modelo (opcional)");
        TextField txtPeso = estilizarCampo(new TextField());
        txtPeso.setPromptText("Peso (kg)");
        txtPeso.setTextFormatter(crearFormatterEntero());
        DatePicker dpCompra = estilizarCampo(new DatePicker());
        dpCompra.getEditor().getStyleClass().add("dialog-field");
        DatePicker dpUltimoMantenimiento = estilizarCampo(new DatePicker());
        dpUltimoMantenimiento.getEditor().getStyleClass().add("dialog-field");
        TextField txtFrecuencia = estilizarCampo(new TextField());
        txtFrecuencia.setPromptText("Frecuencia en días");
        txtFrecuencia.setTextFormatter(crearFormatterEntero());
        TextField txtUbicacion = estilizarCampo(new TextField());
        aplicarFormatoMayusculas(txtUbicacion);
        txtUbicacion.setPromptText("Ubicación");
        TextArea txtDescripcion = estilizarCampo(new TextArea());
        aplicarFormatoMayusculas(txtDescripcion);
        txtDescripcion.setPromptText("Notas o descripción");
        txtDescripcion.setPrefRowCount(3);
        txtDescripcion.setWrapText(true);

        if (esEdicion) {
            txtNombre.setText(equipoExistente.getNombre());
            String tipoExistente = equipoExistente.getTipo();
            if (tipoExistente != null && !tipoExistente.isBlank()) {
                cbTipo.getItems().stream()
                        .filter(item -> item.equalsIgnoreCase(tipoExistente))
                        .findFirst()
                        .ifPresentOrElse(cbTipo::setValue, () -> {
                            cbTipo.getItems().add(tipoExistente);
                            cbTipo.setValue(tipoExistente);
                        });
            }
            String estadoExistente = equipoExistente.getEstado();
            if (estadoExistente != null && !estadoExistente.isBlank()) {
                cbEstado.getItems().stream()
                        .filter(item -> item.equalsIgnoreCase(estadoExistente))
                        .findFirst()
                        .ifPresentOrElse(cbEstado::setValue, () -> {
                            String mayusculas = estadoExistente.toUpperCase(Locale.ROOT);
                            cbEstado.getItems().add(mayusculas);
                            cbEstado.setValue(mayusculas);
                        });
            }
            spCantidad.getValueFactory().setValue(equipoExistente.getCantidad());
            txtMarca.setText(equipoExistente.getMarca());
            txtModelo.setText(equipoExistente.getModelo());
            if (equipoExistente.getPeso() != null) {
                txtPeso.setText(equipoExistente.getPeso());
            }
            dpCompra.setValue(equipoExistente.getFechaAdquisicionDate());
            dpUltimoMantenimiento.setValue(equipoExistente.getFechaUltimoMantenimiento());
            txtFrecuencia.setText(Optional.ofNullable(equipoExistente.getFrecuenciaMantenimiento()).orElse(""));
            txtUbicacion.setText(equipoExistente.getUbicacion());
            txtDescripcion.setText(equipoExistente.getDescripcion());
        }

        GridPane seccionIdentidad = new GridPane();
        seccionIdentidad.setHgap(10);
        seccionIdentidad.setVgap(10);
        seccionIdentidad.setMaxWidth(Double.MAX_VALUE);
        seccionIdentidad.add(crearEtiquetaCampo("Nombre:"), 0, 0);
        seccionIdentidad.add(txtNombre, 1, 0);
        seccionIdentidad.add(crearEtiquetaCampo("Tipo:"), 0, 1);
        seccionIdentidad.add(cbTipo, 1, 1);

        GridPane seccionMarcaModelo = new GridPane();
        seccionMarcaModelo.setHgap(10);
        seccionMarcaModelo.setVgap(10);
        seccionMarcaModelo.setMaxWidth(Double.MAX_VALUE);
        seccionMarcaModelo.add(crearEtiquetaCampo("Marca:"), 0, 0);
        seccionMarcaModelo.add(txtMarca, 1, 0);
        seccionMarcaModelo.add(crearEtiquetaCampo("Modelo:"), 0, 1);
        seccionMarcaModelo.add(txtModelo, 1, 1);

        GridPane seccionEstadoUbicacion = new GridPane();
        seccionEstadoUbicacion.setHgap(10);
        seccionEstadoUbicacion.setVgap(10);
        seccionEstadoUbicacion.setMaxWidth(Double.MAX_VALUE);
        seccionEstadoUbicacion.add(crearEtiquetaCampo("Estado:"), 0, 0);
        seccionEstadoUbicacion.add(cbEstado, 1, 0);
        seccionEstadoUbicacion.add(crearEtiquetaCampo("Ubicación:"), 0, 1);
        seccionEstadoUbicacion.add(txtUbicacion, 1, 1);

        GridPane seccionFechas = new GridPane();
        seccionFechas.setHgap(10);
        seccionFechas.setVgap(10);
        seccionFechas.setMaxWidth(Double.MAX_VALUE);
        seccionFechas.add(crearEtiquetaCampo("Fecha de adquisición:"), 0, 0);
        seccionFechas.add(dpCompra, 1, 0);
        seccionFechas.add(crearEtiquetaCampo("Último mantenimiento:"), 0, 1);
        seccionFechas.add(dpUltimoMantenimiento, 1, 1);
        seccionFechas.add(crearEtiquetaCampo("Frecuencia (días):"), 0, 2);
        seccionFechas.add(txtFrecuencia, 1, 2);

        GridPane seccionPesoCantidad = new GridPane();
        seccionPesoCantidad.setHgap(10);
        seccionPesoCantidad.setVgap(10);
        seccionPesoCantidad.setMaxWidth(Double.MAX_VALUE);
        seccionPesoCantidad.add(crearEtiquetaCampo("Cantidad:"), 0, 0);
        seccionPesoCantidad.add(spCantidad, 1, 0);
        seccionPesoCantidad.add(crearEtiquetaCampo("Peso (kg):"), 0, 1);
        seccionPesoCantidad.add(txtPeso, 1, 1);

        VBox bloqueIdentidad = crearSeccion("Identidad del equipo", seccionIdentidad);
        VBox bloqueMarcaModelo = crearSeccion("Marca y modelo", seccionMarcaModelo);
        VBox bloqueEstadoUbicacion = crearSeccion("Estado y ubicación", seccionEstadoUbicacion);
        VBox bloqueFechas = crearSeccion("Plan de mantenimiento", seccionFechas);
        VBox bloquePesoCantidad = crearSeccion("Cantidad y peso", seccionPesoCantidad);
        bloquePesoCantidad.managedProperty().bind(bloquePesoCantidad.visibleProperty());
        VBox bloqueDescripcion = crearSeccion("Descripción", txtDescripcion);

        GridPane contenedor = new GridPane();
        contenedor.setHgap(18);
        contenedor.setVgap(18);
        contenedor.setPrefSize(900, 600);
        contenedor.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        contenedor.getStyleClass().add("dialog-grid");

        ColumnConstraints columnaIzquierda = new ColumnConstraints();
        columnaIzquierda.setPercentWidth(50);
        columnaIzquierda.setHgrow(Priority.ALWAYS);
        ColumnConstraints columnaDerecha = new ColumnConstraints();
        columnaDerecha.setPercentWidth(50);
        columnaDerecha.setHgrow(Priority.ALWAYS);
        contenedor.getColumnConstraints().addAll(columnaIzquierda, columnaDerecha);

        GridPane.setHgrow(bloqueIdentidad, Priority.ALWAYS);
        GridPane.setVgrow(bloqueIdentidad, Priority.ALWAYS);
        GridPane.setHgrow(bloqueMarcaModelo, Priority.ALWAYS);
        GridPane.setVgrow(bloqueMarcaModelo, Priority.ALWAYS);
        GridPane.setHgrow(bloqueEstadoUbicacion, Priority.ALWAYS);
        GridPane.setVgrow(bloqueEstadoUbicacion, Priority.ALWAYS);
        GridPane.setHgrow(bloqueFechas, Priority.ALWAYS);
        GridPane.setVgrow(bloqueFechas, Priority.ALWAYS);
        GridPane.setHgrow(bloquePesoCantidad, Priority.ALWAYS);
        GridPane.setVgrow(bloquePesoCantidad, Priority.ALWAYS);
        GridPane.setHgrow(bloqueDescripcion, Priority.ALWAYS);
        GridPane.setVgrow(bloqueDescripcion, Priority.ALWAYS);

        Insets margenSeccion = new Insets(0);
        GridPane.setMargin(bloqueIdentidad, margenSeccion);
        GridPane.setMargin(bloqueMarcaModelo, margenSeccion);
        GridPane.setMargin(bloqueEstadoUbicacion, margenSeccion);
        GridPane.setMargin(bloqueFechas, margenSeccion);
        GridPane.setMargin(bloquePesoCantidad, margenSeccion);
        GridPane.setMargin(bloqueDescripcion, margenSeccion);

        contenedor.add(bloqueIdentidad, 0, 0);
        contenedor.add(bloqueMarcaModelo, 1, 0);
        contenedor.add(bloqueEstadoUbicacion, 0, 1);
        contenedor.add(bloqueFechas, 1, 1);
        contenedor.add(bloquePesoCantidad, 0, 2);
        contenedor.add(bloqueDescripcion, 0, 3, 2, 1);

        Runnable ajustarAlturaDialogo = () -> {
            double alturaPreferida = esEquipoConPeso(cbTipo.getValue()) ? 720 : 600;
            dialogPane.setPrefHeight(alturaPreferida);
            if (dialogPane.getScene() != null) {
                Window window = dialogPane.getScene().getWindow();
                if (window != null) {
                    window.sizeToScene();
                }
            }
        };

        boolean mostrarPeso = esEquipoConPeso(cbTipo.getValue());
        bloquePesoCantidad.setVisible(mostrarPeso);
        ajustarAlturaDialogo.run();
        cbTipo.valueProperty().addListener((obs, oldValue, newValue) -> {
            boolean esPeso = esEquipoConPeso(newValue);
            bloquePesoCantidad.setVisible(esPeso);
            if (!esPeso) {
                txtPeso.clear();
                if (spCantidad.getValueFactory() != null) {
                    spCantidad.getValueFactory().setValue(0);
                }
            }
            ajustarAlturaDialogo.run();
        });

        dialogo.getDialogPane().setContent(contenedor);

        Node btnGuardarNode = dialogo.getDialogPane().lookupButton(btnGuardar);
        if (btnGuardarNode instanceof Button botonGuardar) {
            botonGuardar.setDefaultButton(true);
        }
        btnGuardarNode.getStyleClass().addAll("dialog-action-button", "dialog-action-button--primary");

        Node btnCancelarNode = dialogo.getDialogPane().lookupButton(btnCancelar);
        if (btnCancelarNode != null) {
            btnCancelarNode.getStyleClass().addAll("dialog-action-button", "dialog-action-button--ghost");
            if (btnCancelarNode instanceof Button botonCancelar) {
                botonCancelar.setCancelButton(true);
            }
        }

        btnGuardarNode.addEventFilter(ActionEvent.ACTION, event -> {
            boolean requiereMedidas = esEquipoConPeso(cbTipo.getValue());
            Optional<String> error = validarFormulario(txtNombre.getText(), cbTipo.getValue(), cbEstado.getValue(),
                    txtMarca.getText(), requiereMedidas, txtPeso.getText(), spCantidad.getValue(), txtFrecuencia.getText());
            if (error.isPresent()) {
                mostrarAdvertencia("Validación", error.get());
                event.consume();
            }
        });

        dialogo.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardar) {
                Equipo equipo = new Equipo();
                if (esEdicion) {
                    equipo.setId(equipoExistente.getId());
                }
                equipo.setNombre(txtNombre.getText());
                equipo.setTipo(cbTipo.getValue());
                if (cbEstado.isEditable()) {
                    String textoEstado = cbEstado.getEditor().getText();
                    if (textoEstado != null && !textoEstado.isBlank()) {
                        String mayusculas = textoEstado.toUpperCase(Locale.ROOT);
                        if (cbEstado.getItems().stream().noneMatch(item -> item.equalsIgnoreCase(mayusculas))) {
                            cbEstado.getItems().add(mayusculas);
                        }
                        cbEstado.setValue(mayusculas);
                    }
                }
                equipo.setEstado(cbEstado.getValue());
                boolean requiereMedidas = esEquipoConPeso(cbTipo.getValue());
                if (requiereMedidas) {
                    equipo.setCantidad(spCantidad.getValue());
                    equipo.setPeso(txtPeso.getText());
                } else {
                    equipo.setCantidad(0);
                    equipo.setPeso((String) null);
                }
                equipo.setMarca(txtMarca.getText());
                equipo.setModelo(txtModelo.getText());
                equipo.setFechaAdquisicion(dpCompra.getValue());
                equipo.setFechaUltimoMantenimiento(dpUltimoMantenimiento.getValue());
                equipo.setUbicacion(txtUbicacion.getText());
                equipo.setDescripcion(txtDescripcion.getText());
                equipo.setFrecuenciaMantenimiento(parseFrecuencia(txtFrecuencia.getText()));
                return equipo;
            }
            return null;
        });

        return dialogo;
    }

    private Label crearEtiquetaCampo(String texto) {
        Label label = new Label(texto);
        label.getStyleClass().add("dialog-label");
        return label;
    }

    private VBox crearSeccion(String titulo, Node contenido) {
        Label tituloSeccion = new Label(titulo);
        tituloSeccion.getStyleClass().add("dialog-label");
        VBox seccion = new VBox(10, tituloSeccion, contenido);
        seccion.setFillWidth(true);
        seccion.setMaxWidth(Double.MAX_VALUE);
        seccion.getStyleClass().add("dialog-section");
        if (contenido instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
            VBox.setVgrow(region, Priority.ALWAYS);
        }
        GridPane.setFillWidth(seccion, true);
        GridPane.setHgrow(seccion, Priority.ALWAYS);
        return seccion;
    }

    private <T extends Control> T estilizarCampo(T control) {
        control.getStyleClass().add("dialog-field");
        return control;
    }

    private boolean esEquipoConPeso(String tipo) {
        return tipo != null && tipo.trim().equalsIgnoreCase("Equipo con Peso");
    }

    private Optional<String> validarFormulario(String nombre, String tipo, String estado, String marca,
                                               boolean requiereMedidas, String pesoTexto, Integer cantidad,
                                               String frecuenciaTexto) {
        if (nombre == null || nombre.isBlank()) {
            return Optional.of("El nombre es obligatorio.");
        }
        if (tipo == null || tipo.isBlank()) {
            return Optional.of("El tipo es obligatorio.");
        }
        if (estado == null || estado.isBlank()) {
            return Optional.of("Debe seleccionar un estado.");
        }
        if (marca == null || marca.isBlank()) {
            return Optional.of("La marca es obligatoria.");
        }
        if (requiereMedidas) {
            if (cantidad == null || cantidad <= 0) {
                return Optional.of("La cantidad debe ser un entero positivo para equipos con peso.");
            }
            if (pesoTexto == null || pesoTexto.isBlank()) {
                return Optional.of("El peso es obligatorio para equipos con peso.");
            }
            try {
                int peso = Integer.parseInt(pesoTexto);
                if (peso <= 0) {
                    return Optional.of("El peso debe ser un entero positivo.");
                }
            } catch (NumberFormatException e) {
                return Optional.of("El peso debe ser un entero positivo.");
            }
        } else if (cantidad != null && cantidad < 0) {
            return Optional.of("La cantidad no puede ser negativa.");
        }
        if (frecuenciaTexto != null && !frecuenciaTexto.isBlank()) {
            try {
                int frecuencia = Integer.parseInt(frecuenciaTexto);
                if (frecuencia < 0) {
                    return Optional.of("La frecuencia debe ser positiva.");
                }
            } catch (NumberFormatException e) {
                return Optional.of("La frecuencia debe ser un número entero.");
            }
        }
        return Optional.empty();
    }

    private String parseFrecuencia(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return String.valueOf(Integer.parseInt(texto));
    }

    private void registrarAuditoria(String accion, String detalle) {
        User usuario = SessionManager.getCurrentUser();
        int usuarioId = usuario != null ? usuario.getId() : 0;
        AuditoriaUtil.registrarAccion(usuarioId, accion, detalle);
        if (usuario != null) {
            try {
                UserService.registrarActividad(usuario, accion);
            } catch (SQLException e) {
                // se registra pero no se interrumpe el flujo principal
                System.err.println("No se pudo registrar la actividad del usuario: " + e.getMessage());
            }
        }
    }

    private TextFormatter<String> crearFormatterEntero() {
        return new TextFormatter<>(change -> {
            String nuevo = change.getControlNewText();
            if (nuevo.matches("\\d*")) {
                return change;
            }
            return null;
        });
    }

    private void aplicarFormatoMayusculas(TextInputControl control) {
        if (control == null) {
            return;
        }
        control.setTextFormatter(crearFormatterMayusculas());
    }

    private TextFormatter<String> crearFormatterMayusculas() {
        return new TextFormatter<>(change -> {
            String texto = change.getText();
            if (texto != null) {
                change.setText(texto.toUpperCase(Locale.ROOT));
            }
            return change;
        });
    }

    private String formatearFecha(LocalDate fecha) {
        if (fecha == null) {
            return "-";
        }
        return FORMATO_FECHA.format(fecha).toUpperCase(LOCALE_ES).replace(".", "");
    }

    private void mostrarInformacion(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
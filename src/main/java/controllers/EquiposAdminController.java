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
import javafx.scene.layout.GridPane;
import models.Equipo;
import models.Role;
import models.User;
import util.AuditoriaUtil;
import util.DatabaseUtil;
import util.ReporteUtil;
import util.SessionManager;
import util.UserService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.ResourceBundle;
import java.net.URL;
import java.util.Set;

public class EquiposAdminController implements Initializable {

    @FXML private TableView<Equipo> tablaEquipos;
    @FXML private TableColumn<Equipo, String> colNombre;
    @FXML private TableColumn<Equipo, String> colTipo;
    @FXML private TableColumn<Equipo, String> colEstado;
    @FXML private TableColumn<Equipo, Integer> colCantidad;
    @FXML private TableColumn<Equipo, String> colUbicacion;
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
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colUbicacion.setCellValueFactory(new PropertyValueFactory<>("ubicacion"));
        colUltimoMantenimiento.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatearFecha(cellData.getValue().getFechaUltimoMantenimiento())));
        colProximoMantenimiento.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatearFecha(cellData.getValue().getProximoMantenimiento())));

        tablaEquipos.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Equipo equipo, boolean empty) {
                super.updateItem(equipo, empty);
                if (empty || equipo == null) {
                    setStyle("");
                    setTooltip(null);
                } else if (equipo.isEstadoCritico() || equipo.needsMaintenance(LocalDate.now())) {
                    setStyle("-fx-background-color: rgba(244,67,54,0.25);");
                    setTooltip(new Tooltip("Mantenimiento vencido o estado crítico"));
                } else if (equipo.maintenanceDueSoon(LocalDate.now(), 7)) {
                    setStyle("-fx-background-color: rgba(255,193,7,0.2);");
                    setTooltip(new Tooltip("Mantenimiento próximo"));
                } else {
                    setStyle("");
                    setTooltip(null);
                }
            }
        });
    }

    private void configurarFiltros() {
        filtroEstado.getItems().setAll("Todos");
        filtroTipo.getItems().setAll("Todos");
        actualizarOpcionesFiltros();

        filtroEstado.setValue("Todos");
        filtroTipo.setValue("Todos");

        filtroEstado.setOnAction(event -> aplicarFiltros());
        filtroTipo.setOnAction(event -> aplicarFiltros());
    }

    private void actualizarOpcionesFiltros() {
        Set<String> estados = new HashSet<>();
        Set<String> tipos = new HashSet<>();
        for (Equipo equipo : equipos) {
            if (equipo.getEstado() != null && !equipo.getEstado().isBlank()) {
                estados.add(equipo.getEstado());
            }
            if (equipo.getTipo() != null && !equipo.getTipo().isBlank()) {
                tipos.add(equipo.getTipo());
            }
        }
        filtroEstado.getItems().setAll("Todos");
        filtroEstado.getItems().addAll(estados.stream().sorted().toList());
        filtroTipo.getItems().setAll("Todos");
        filtroTipo.getItems().addAll(tipos.stream().sorted().toList());
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
        dialogo.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre del equipo");
        TextField txtTipo = new TextField();
        txtTipo.setPromptText("Tipo");
        ComboBox<String> cbEstado = new ComboBox<>(FXCollections.observableArrayList(
                "OPERATIVO", "MANTENIMIENTO", "CRITICO", "FUERA DE SERVICIO"));
        cbEstado.setEditable(true);
        cbEstado.getSelectionModel().selectFirst();
        Spinner<Integer> spCantidad = new Spinner<>(0, Integer.MAX_VALUE, 0);
        spCantidad.setEditable(true);
        DatePicker dpCompra = new DatePicker();
        DatePicker dpUltimoMantenimiento = new DatePicker();
        TextField txtFrecuencia = new TextField();
        txtFrecuencia.setPromptText("Frecuencia en días");
        txtFrecuencia.setTextFormatter(crearFormatterEntero());
        TextField txtUbicacion = new TextField();
        txtUbicacion.setPromptText("Ubicación");
        TextArea txtDescripcion = new TextArea();
        txtDescripcion.setPromptText("Notas o descripción");
        txtDescripcion.setPrefRowCount(3);

        if (esEdicion) {
            txtNombre.setText(equipoExistente.getNombre());
            txtTipo.setText(equipoExistente.getTipo());
            cbEstado.setValue(equipoExistente.getEstado());
            spCantidad.getValueFactory().setValue(equipoExistente.getCantidad());
            dpCompra.setValue(equipoExistente.getFechaCompra());
            dpUltimoMantenimiento.setValue(equipoExistente.getFechaUltimoMantenimiento());
            if (equipoExistente.getFrecuenciaMantenimiento() != null) {
                txtFrecuencia.setText(String.valueOf(equipoExistente.getFrecuenciaMantenimiento()));
            }
            txtUbicacion.setText(equipoExistente.getUbicacion());
            txtDescripcion.setText(equipoExistente.getDescripcion());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        int fila = 0;
        grid.add(new Label("Nombre:"), 0, fila);
        grid.add(txtNombre, 1, fila++);
        grid.add(new Label("Tipo:"), 0, fila);
        grid.add(txtTipo, 1, fila++);
        grid.add(new Label("Estado:"), 0, fila);
        grid.add(cbEstado, 1, fila++);
        grid.add(new Label("Cantidad:"), 0, fila);
        grid.add(spCantidad, 1, fila++);
        grid.add(new Label("Fecha de compra:"), 0, fila);
        grid.add(dpCompra, 1, fila++);
        grid.add(new Label("Último mantenimiento:"), 0, fila);
        grid.add(dpUltimoMantenimiento, 1, fila++);
        grid.add(new Label("Frecuencia (días):"), 0, fila);
        grid.add(txtFrecuencia, 1, fila++);
        grid.add(new Label("Ubicación:"), 0, fila);
        grid.add(txtUbicacion, 1, fila++);
        grid.add(new Label("Descripción:"), 0, fila);
        grid.add(txtDescripcion, 1, fila);

        dialogo.getDialogPane().setContent(grid);

        Node btnAceptar = dialogo.getDialogPane().lookupButton(btnGuardar);
        btnAceptar.addEventFilter(ActionEvent.ACTION, event -> {
            Optional<String> error = validarFormulario(txtNombre.getText(), txtTipo.getText(), cbEstado.getValue(),
                    spCantidad.getValue(), txtFrecuencia.getText());
            if (error.isPresent()) {
                mostrarAdvertencia("Validación", error.get());
                event.consume();
            }
        });

        dialogo.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardar) {
                Equipo equipo = esEdicion ? new Equipo() : new Equipo();
                if (esEdicion) {
                    equipo.setId(equipoExistente.getId());
                }
                equipo.setNombre(txtNombre.getText());
                equipo.setTipo(txtTipo.getText());
                equipo.setEstado(cbEstado.getValue());
                equipo.setCantidad(spCantidad.getValue());
                equipo.setFechaCompra(dpCompra.getValue());
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

    private Optional<String> validarFormulario(String nombre, String tipo, String estado, int cantidad, String frecuenciaTexto) {
        if (nombre == null || nombre.isBlank()) {
            return Optional.of("El nombre es obligatorio.");
        }
        if (tipo == null || tipo.isBlank()) {
            return Optional.of("El tipo es obligatorio.");
        }
        if (estado == null || estado.isBlank()) {
            return Optional.of("Debe seleccionar un estado.");
        }
        if (cantidad < 0) {
            return Optional.of("La cantidad debe ser un número positivo.");
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

    private Integer parseFrecuencia(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return Integer.parseInt(texto);
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

    private String formatearFecha(LocalDate fecha) {
        return fecha == null ? "-" : fecha.toString();
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
package controllers;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;
import models.Auditoria;
import models.ResumenTipo;
import models.Role;
import models.User;
import util.AuditoriaUtil;
import util.ReporteUtil;
import util.SessionManager;
import util.UserService;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

public class AuditoriaController implements Initializable {

    private static final DateTimeFormatter FECHA_MOSTRAR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final List<ResumenTipo> TIPOS_HIERARCHY = List.of(
            ResumenTipo.DIARIO, ResumenTipo.SEMANAL, ResumenTipo.MENSUAL, ResumenTipo.ANUAL);

    @FXML private TreeView<ResumenTreeData> treeResumenes;
    @FXML private TableView<Auditoria> tablaAuditoria;
    @FXML private TableColumn<Auditoria, Integer> colId;
    @FXML private TableColumn<Auditoria, String> colUsuario;
    @FXML private TableColumn<Auditoria, String> colAccion;
    @FXML private TableColumn<Auditoria, String> colDetalle;
    @FXML private TableColumn<Auditoria, LocalDateTime> colFecha;
    @FXML private TableColumn<Auditoria, Void> colVer;
    @FXML private TableColumn<Auditoria, Void> colDescargar;
    @FXML private ComboBox<User> cbUsuarios;
    @FXML private ComboBox<Integer> cbAnio;
    @FXML private ComboBox<Month> cbMes;
    @FXML private ComboBox<ResumenTipo> cbTipo;
    @FXML private Button btnVer;
    @FXML private Button btnDescargar;

    private final ObservableList<Auditoria> auditorias = FXCollections.observableArrayList();
    private Path archivoSeleccionado;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (!SessionManager.tienePermiso(Role.ADMIN)) {
            deshabilitarAcceso();
            return;
        }
        configurarTabla();
        configurarArbol();
        configurarCombos();
        cargarUsuarios();
        aplicarFiltros();
    }

    private void deshabilitarAcceso() {
        if (tablaAuditoria != null) {
            tablaAuditoria.setPlaceholder(new Label("Acceso restringido a administradores"));
        }
        if (treeResumenes != null) {
            treeResumenes.setRoot(new TreeItem<>(new ResumenTreeData("Acceso restringido", null, null, null)));
            treeResumenes.setDisable(true);
        }
        if (btnVer != null) {
            btnVer.setDisable(true);
        }
        if (btnDescargar != null) {
            btnDescargar.setDisable(true);
        }
    }

    private void configurarTabla() {
        if (tablaAuditoria == null) {
            return;
        }
        tablaAuditoria.setItems(auditorias);
        tablaAuditoria.setPlaceholder(new Label("Sin registros para los filtros seleccionados"));

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colAccion.setCellValueFactory(data -> new SimpleStringProperty(formatearTipo(data.getValue())));
        colDetalle.setCellValueFactory(data -> new SimpleStringProperty(formatearDetalle(data.getValue())));
        colDetalle.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                setText(item);
                Auditoria registro = getTableRow() != null ? getTableRow().getItem() : null;
                Path archivo = obtenerArchivoDesdeRegistro(registro);
                setTooltip(archivo != null ? new Tooltip(archivo.toString()) : null);
            }
        });

        colFecha.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getFecha()));
        colFecha.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(FECHA_MOSTRAR.format(item));
                }
            }
        });
        colFecha.setSortType(TableColumn.SortType.DESCENDING);

        configurarColumnaVer();
        configurarColumnaDescargar();

        tablaAuditoria.getSelectionModel().selectedItemProperty().addListener((obs, old, registro) -> {
            archivoSeleccionado = registro != null ? registro.getArchivo() : null;
            actualizarBotones();
        });
    }

    private void configurarArbol() {
        if (treeResumenes == null) {
            return;
        }
        treeResumenes.setShowRoot(false);
        treeResumenes.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item != null && item.getValue() != null && item.getValue().registro() != null) {
                Auditoria registro = item.getValue().registro();
                tablaAuditoria.getSelectionModel().select(registro);
                tablaAuditoria.scrollTo(registro);
                archivoSeleccionado = item.getValue().archivo();
            } else {
                tablaAuditoria.getSelectionModel().clearSelection();
                archivoSeleccionado = null;
            }
            actualizarBotones();
        });
    }

    private void configurarCombos() {
        if (cbTipo != null) {
            cbTipo.setItems(FXCollections.observableArrayList(ResumenTipo.values()));
            cbTipo.setConverter(new StringConverter<>() {
                @Override
                public String toString(ResumenTipo object) {
                    return object != null ? object.getDisplayName() : "";
                }

                @Override
                public ResumenTipo fromString(String string) {
                    return null;
                }
            });
            cbTipo.getSelectionModel().select(ResumenTipo.TODOS);
            cbTipo.valueProperty().addListener((obs, old, value) -> aplicarFiltros());
        }
        if (cbMes != null) {
            cbMes.setItems(FXCollections.observableArrayList());
            cbMes.setConverter(new StringConverter<>() {
                @Override
                public String toString(Month month) {
                    return month != null ? month.getDisplayName(TextStyle.FULL, Locale.getDefault()) : "";
                }

                @Override
                public Month fromString(String string) {
                    return null;
                }
            });
            cbMes.setDisable(true);
            cbMes.valueProperty().addListener((obs, old, value) -> aplicarFiltros());
        }
        if (cbAnio != null) {
            cbAnio.valueProperty().addListener((obs, old, value) -> {
                actualizarMesesDisponibles(value);
                aplicarFiltros();
            });
        }
        if (btnVer != null) {
            btnVer.setDisable(true);
        }
        if (btnDescargar != null) {
            btnDescargar.setDisable(true);
        }
    }

    private void cargarUsuarios() {
        if (cbUsuarios == null) {
            return;
        }
        try {
            ObservableList<User> usuarios = UserService.listarUsuariosPorRol(Role.RECEPCIONISTA);
            User todos = new User(0, "Todos", "", Role.RECEPCIONISTA);
            usuarios.add(0, todos);
            cbUsuarios.setItems(usuarios);
            cbUsuarios.setConverter(new StringConverter<>() {
                @Override
                public String toString(User user) {
                    return user != null ? user.getUsername() : "";
                }

                @Override
                public User fromString(String string) {
                    return null;
                }
            });
            cbUsuarios.getSelectionModel().selectFirst();
            cbUsuarios.valueProperty().addListener((obs, old, value) -> {
                actualizarAniosDisponibles();
                aplicarFiltros();
            });
            actualizarAniosDisponibles();
        } catch (Exception e) {
            mostrarAlerta("No se pudieron cargar los recepcionistas: " + e.getMessage());
        }
    }

    private void actualizarAniosDisponibles() {
        if (cbAnio == null) {
            return;
        }
        Integer usuarioId = getUsuarioSeleccionadoId();
        ObservableList<Integer> anios = AuditoriaUtil.listarAniosResumenes(usuarioId);
        cbAnio.setItems(anios);
        cbAnio.getSelectionModel().clearSelection();
        actualizarMesesDisponibles(null);
    }

    private void actualizarMesesDisponibles(Integer anio) {
        if (cbMes == null) {
            return;
        }
        cbMes.getSelectionModel().clearSelection();
        if (anio == null) {
            cbMes.getItems().clear();
            cbMes.setDisable(true);
            return;
        }
        Integer usuarioId = getUsuarioSeleccionadoId();
        ObservableList<Month> meses = AuditoriaUtil.listarMesesResumenes(usuarioId, anio);
        cbMes.setItems(meses);
        cbMes.setDisable(meses.isEmpty());
    }

    @FXML
    private void aplicarFiltros() {
        if (tablaAuditoria == null) {
            return;
        }
        Integer usuarioId = getUsuarioSeleccionadoId();
        Integer anio = cbAnio != null ? cbAnio.getValue() : null;
        Month mes = cbMes != null ? cbMes.getValue() : null;
        ResumenTipo tipo = cbTipo != null ? cbTipo.getValue() : ResumenTipo.TODOS;

        LocalDate inicio = null;
        LocalDate fin = null;
        if (anio != null) {
            if (mes != null) {
                LocalDate primerDia = LocalDate.of(anio, mes, 1);
                inicio = primerDia;
                fin = primerDia.withDayOfMonth(primerDia.lengthOfMonth());
            } else {
                inicio = LocalDate.of(anio, 1, 1);
                fin = LocalDate.of(anio, 12, 31);
            }
        }

        ObservableList<Auditoria> registros = AuditoriaUtil.filtrarResumenes(usuarioId, inicio, fin, tipo);
        auditorias.setAll(registros);
        tablaAuditoria.getSelectionModel().clearSelection();
        archivoSeleccionado = null;
        if (treeResumenes != null) {
            treeResumenes.getSelectionModel().clearSelection();
        }
        reconstruirArbol(registros);
        if (colFecha != null) {
            tablaAuditoria.getSortOrder().setAll(colFecha);
            tablaAuditoria.sort();
        }
        actualizarBotones();
    }

    @FXML
    private void limpiarFiltros() {
        if (cbAnio != null) {
            cbAnio.getSelectionModel().clearSelection();
        }
        if (cbMes != null) {
            cbMes.getSelectionModel().clearSelection();
            cbMes.setDisable(true);
            cbMes.getItems().clear();
        }
        if (cbTipo != null) {
            cbTipo.getSelectionModel().select(ResumenTipo.TODOS);
        }
        aplicarFiltros();
    }

    @FXML
    private void verSeleccion() {
        Path archivo = obtenerArchivoSeleccionado();
        if (archivo == null) {
            mostrarAlerta("Selecciona un resumen con archivo disponible.");
            return;
        }
        if (!Files.exists(archivo)) {
            mostrarAlerta("El archivo indicado ya no existe: " + archivo);
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(archivo.toFile());
            } else {
                mostrarAlerta("La apertura automática no está soportada en este sistema.");
            }
        } catch (IOException e) {
            mostrarAlerta("No se pudo abrir el archivo: " + e.getMessage());
        }
    }

    @FXML
    private void descargarSeleccion() {
        Path archivo = obtenerArchivoSeleccionado();
        if (archivo == null) {
            mostrarAlerta("Selecciona un resumen para descargar.");
            return;
        }
        if (!Files.exists(archivo)) {
            mostrarAlerta("El archivo indicado ya no existe: " + archivo);
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(archivo.getFileName().toString());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));
        File destino = fileChooser.showSaveDialog(obtenerVentana());
        if (destino != null) {
            try {
                Files.copy(archivo, destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
                mostrarAlerta("Archivo descargado en: " + destino.getAbsolutePath());
            } catch (IOException e) {
                mostrarAlerta("No se pudo guardar el archivo: " + e.getMessage());
            }
        }
    }

    @FXML
    private void exportarReporte() {
        if (auditorias.isEmpty()) {
            mostrarAlerta("No hay registros para exportar.");
            return;
        }
        ReporteUtil.generarReporteAuditoria(auditorias);
    }

    private void configurarColumnaVer() {
        if (colVer == null) {
            return;
        }
        colVer.setCellFactory(col -> new TableCell<>() {
            private final Button boton = new Button("Ver");

            {
                boton.setOnAction(e -> {
                    Auditoria registro = getTableView().getItems().get(getIndex());
                    abrirArchivo(registro);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Path archivo = obtenerArchivoDesdeRegistro(getTableRow().getItem());
                boolean habilitado = archivo != null && Files.exists(archivo);
                boton.setDisable(!habilitado);
                setGraphic(habilitado ? boton : null);
            }
        });
    }

    private void configurarColumnaDescargar() {
        if (colDescargar == null) {
            return;
        }
        colDescargar.setCellFactory(col -> new TableCell<>() {
            private final Button boton = new Button("Descargar");

            {
                boton.setOnAction(e -> {
                    Auditoria registro = getTableView().getItems().get(getIndex());
                    descargarArchivo(registro);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Path archivo = obtenerArchivoDesdeRegistro(getTableRow().getItem());
                boolean habilitado = archivo != null && Files.exists(archivo);
                boton.setDisable(!habilitado);
                setGraphic(habilitado ? boton : null);
            }
        });
    }

    private void abrirArchivo(Auditoria registro) {
        if (registro == null) {
            mostrarAlerta("Selecciona un resumen válido.");
            return;
        }
        Path archivo = obtenerArchivoDesdeRegistro(registro);
        if (archivo == null) {
            mostrarAlerta("No hay archivo asociado al registro seleccionado.");
            return;
        }
        if (!Files.exists(archivo)) {
            mostrarAlerta("El archivo indicado ya no existe: " + archivo);
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(archivo.toFile());
            } else {
                mostrarAlerta("La apertura automática no está soportada en este sistema.");
            }
        } catch (IOException e) {
            mostrarAlerta("No se pudo abrir el archivo: " + e.getMessage());
        }
    }

    private void descargarArchivo(Auditoria registro) {
        Path archivo = obtenerArchivoDesdeRegistro(registro);
        if (archivo == null) {
            mostrarAlerta("No hay archivo asociado para descargar.");
            return;
        }
        if (!Files.exists(archivo)) {
            mostrarAlerta("El archivo indicado ya no existe: " + archivo);
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(archivo.getFileName().toString());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));
        File destino = fileChooser.showSaveDialog(obtenerVentana());
        if (destino != null) {
            try {
                Files.copy(archivo, destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
                mostrarAlerta("Archivo descargado en: " + destino.getAbsolutePath());
            } catch (IOException e) {
                mostrarAlerta("No se pudo guardar el archivo: " + e.getMessage());
            }
        }
    }

    private void reconstruirArbol(List<Auditoria> registros) {
        if (treeResumenes == null) {
            return;
        }
        TreeItem<ResumenTreeData> root = new TreeItem<>(new ResumenTreeData("Resúmenes", null, null, null));
        root.setExpanded(true);
        if (registros == null || registros.isEmpty()) {
            treeResumenes.setRoot(root);
            return;
        }

        Map<String, Map<Integer, EnumMap<ResumenTipo, List<Auditoria>>>> estructura = new TreeMap<>();
        for (Auditoria registro : registros) {
            ResumenTipo tipo = registro.getResumenTipo();
            if (tipo == null || tipo == ResumenTipo.TODOS) {
                continue;
            }
            String usuarioNombre = registro.getUsuario() == null || registro.getUsuario().isBlank()
                    ? "Sin usuario"
                    : registro.getUsuario();
            Integer anio = registro.getAnio();
            if (anio == null) {
                continue;
            }
            estructura
                    .computeIfAbsent(usuarioNombre, k -> new TreeMap<>(Comparator.reverseOrder()))
                    .computeIfAbsent(anio, k -> new EnumMap<>(ResumenTipo.class))
                    .computeIfAbsent(tipo, k -> new ArrayList<>())
                    .add(registro);
        }

        for (Map.Entry<String, Map<Integer, EnumMap<ResumenTipo, List<Auditoria>>>> usuarioEntry : estructura.entrySet()) {
            TreeItem<ResumenTreeData> usuarioItem = new TreeItem<>(ResumenTreeData.usuario(usuarioEntry.getKey()));
            usuarioItem.setExpanded(true);
            for (Map.Entry<Integer, EnumMap<ResumenTipo, List<Auditoria>>> anioEntry : usuarioEntry.getValue().entrySet()) {
                TreeItem<ResumenTreeData> anioItem = new TreeItem<>(ResumenTreeData.anio(anioEntry.getKey()));
                anioItem.setExpanded(true);
                for (ResumenTipo tipo : TIPOS_HIERARCHY) {
                    List<Auditoria> lista = anioEntry.getValue().get(tipo);
                    if (lista == null || lista.isEmpty()) {
                        continue;
                    }
                    lista.sort(Comparator.comparing(Auditoria::getFecha, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
                    TreeItem<ResumenTreeData> tipoItem = new TreeItem<>(ResumenTreeData.tipo(tipo));
                    tipoItem.setExpanded(true);
                    for (Auditoria registro : lista) {
                        Path archivo = registro.getArchivo();
                        String nombre = registro.getNombreArchivo();
                        tipoItem.getChildren().add(new TreeItem<>(ResumenTreeData.archivo(nombre, tipo, archivo, registro)));
                    }
                    anioItem.getChildren().add(tipoItem);
                }
                if (!anioItem.getChildren().isEmpty()) {
                    usuarioItem.getChildren().add(anioItem);
                }
            }
            if (!usuarioItem.getChildren().isEmpty()) {
                root.getChildren().add(usuarioItem);
            }
        }
        treeResumenes.setRoot(root);
    }

    private Integer getUsuarioSeleccionadoId() {
        if (cbUsuarios == null) {
            return null;
        }
        User seleccionado = cbUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null || seleccionado.getId() <= 0) {
            return null;
        }
        return seleccionado.getId();
    }

    private Path obtenerArchivoSeleccionado() {
        if (archivoSeleccionado != null) {
            return archivoSeleccionado;
        }
        Auditoria registro = tablaAuditoria != null ? tablaAuditoria.getSelectionModel().getSelectedItem() : null;
        return obtenerArchivoDesdeRegistro(registro);
    }

    private Path obtenerArchivoDesdeRegistro(Auditoria registro) {
        return registro != null ? registro.getArchivo() : null;
    }

    private void actualizarBotones() {
        Path archivo = obtenerArchivoSeleccionado();
        boolean habilitado = archivo != null && Files.exists(archivo);
        if (btnVer != null) {
            btnVer.setDisable(!habilitado);
        }
        if (btnDescargar != null) {
            btnDescargar.setDisable(!habilitado);
        }
    }

    private String formatearTipo(Auditoria registro) {
        if (registro == null) {
            return "";
        }
        ResumenTipo tipo = registro.getResumenTipo();
        return tipo != null ? tipo.getDisplayName() : registro.getAccion();
    }

    private String formatearDetalle(Auditoria registro) {
        return registro != null ? registro.getNombreArchivo() : "";
    }

    private void mostrarAlerta(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private Window obtenerVentana() {
        if (tablaAuditoria != null && tablaAuditoria.getScene() != null) {
            return tablaAuditoria.getScene().getWindow();
        }
        return btnDescargar != null ? btnDescargar.getScene().getWindow() : null;
    }

    private record ResumenTreeData(String etiqueta, ResumenTipo tipo, Path archivo, Auditoria registro) {
        static ResumenTreeData usuario(String nombre) {
            return new ResumenTreeData(nombre, null, null, null);
        }

        static ResumenTreeData anio(int anio) {
            return new ResumenTreeData(String.valueOf(anio), null, null, null);
        }

        static ResumenTreeData tipo(ResumenTipo tipo) {
            return new ResumenTreeData(tipo.getDisplayName(), tipo, null, null);
        }

        static ResumenTreeData archivo(String nombre, ResumenTipo tipo, Path archivo, Auditoria registro) {
            return new ResumenTreeData(nombre, tipo, archivo, registro);
        }

        @Override
        public String toString() {
            return etiqueta;
        }
    }
}
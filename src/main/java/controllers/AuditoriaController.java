package controllers;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import models.Auditoria;
import models.Role;
import models.User;
import util.AuditoriaUtil;
import util.ReporteUtil;
import util.SessionManager;
import util.UserService;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class AuditoriaController implements Initializable {

    @FXML private TableView<Auditoria> tablaAuditoria;
    @FXML private TableColumn<Auditoria, Integer> colId;
    @FXML private TableColumn<Auditoria, String> colUsuario;
    @FXML private TableColumn<Auditoria, String> colAccion;
    @FXML private TableColumn<Auditoria, String> colDetalle;
    @FXML private TableColumn<Auditoria, String> colFecha;
    @FXML private TableColumn<Auditoria, Void> colAbrir;
    @FXML private ChoiceBox<User> cbUsuarios;
    @FXML private DatePicker dpFechaInicio;
    @FXML private DatePicker dpFechaFin;
    @FXML private TextField txtTipo;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (!SessionManager.tienePermiso(Role.ADMIN)) {
            if (tablaAuditoria != null) {
                tablaAuditoria.setPlaceholder(new Label("Acceso restringido a administradores"));
            }
            return;
        }
        configurarColumnas();
        cargarUsuarios();
    }

    private void configurarColumnas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colAccion.setCellValueFactory(new PropertyValueFactory<>("accion"));
        colDetalle.setCellValueFactory(new PropertyValueFactory<>("detalle"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        configurarColumnaAbrir();
    }

    private void cargarUsuarios() {
        try {
            ObservableList<User> usuarios = UserService.listarUsuarios();
            User todos = new User(0, "Todos", "", Role.ADMIN);
            usuarios.add(0, todos);
            cbUsuarios.setItems(usuarios);
            cbUsuarios.setConverter(new javafx.util.StringConverter<>() {
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
            cbUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, old, user) -> aplicarFiltros());
            aplicarFiltros();
        } catch (Exception e) {
            mostrarAlerta("No se pudieron cargar los usuarios de auditoría: " + e.getMessage());
        }
    }

    private void configurarColumnaAbrir() {
        if (colAbrir == null) {
            return;
        }
        colAbrir.setCellFactory(col -> new TableCell<>() {
            private final Button boton = new Button("Abrir");

            {
                boton.setStyle("-fx-background-color: #118ab2; -fx-text-fill: white; -fx-font-weight: bold;");
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
                Auditoria registro = getTableRow().getItem();
                boolean habilitado = "RESUMEN_TURNO".equalsIgnoreCase(registro.getAccion())
                        && registro.getDetalle() != null && !registro.getDetalle().isBlank();
                boton.setDisable(!habilitado);
                setGraphic(habilitado ? boton : null);
            }
        });
    }

    private void abrirArchivo(Auditoria registro) {
        String detalle = registro.getDetalle();
        if (detalle == null || detalle.isBlank()) {
            mostrarAlerta("No hay archivo asociado al registro seleccionado.");
            return;
        }
        Path path = Path.of(detalle);
        if (!Files.exists(path)) {
            mostrarAlerta("El archivo indicado ya no existe: " + detalle);
            return;
        }
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(path.toFile());
            } else {
                mostrarAlerta("La apertura automática no está soportada en este sistema.");
            }
        } catch (Exception e) {
            mostrarAlerta("No se pudo abrir el archivo: " + e.getMessage());
        }
    }

    @FXML
    private void aplicarFiltros() {
        if (cbUsuarios == null) {
            return;
        }
        User seleccionado = cbUsuarios.getSelectionModel().getSelectedItem();
        int usuarioId = seleccionado != null ? seleccionado.getId() : 0;
        LocalDate inicio = dpFechaInicio != null ? dpFechaInicio.getValue() : null;
        LocalDate fin = dpFechaFin != null ? dpFechaFin.getValue() : null;
        String tipo = txtTipo != null ? txtTipo.getText() : null;
        ObservableList<Auditoria> registros = AuditoriaUtil.filtrarAcciones(usuarioId, inicio, fin, tipo);
        if (tablaAuditoria != null) {
            tablaAuditoria.setItems(registros);
        }
    }

    @FXML
    private void exportarReporte() {
        if (tablaAuditoria != null && tablaAuditoria.getItems() != null) {
            ReporteUtil.generarReporteAuditoria(tablaAuditoria.getItems());
        }
    }

    private void mostrarAlerta(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
package controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import models.Cliente;
import util.DatabaseUtil;
import javafx.scene.paint.Color;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ListaClientesController implements Initializable {

    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, String> colNombreCompleto;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colEstado;
    @FXML private TableColumn<Cliente, Void> colAcciones;
    @FXML private TextField txtBuscar;
    @FXML private Button btnLimpiar;
    @FXML private Label lblTitulo;

    private ObservableList<Cliente> clientesOriginales = FXCollections.observableArrayList();
    private Cliente clienteEditado;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarEstilosGlobales();
        configurarColumnas();
        cargarClientes();
        configurarBusqueda();

        clientesOriginales.setAll(tablaClientes.getItems());

        configurarFilas();
        ajustarAnchoColumnas();
        eliminarBarrasDesplazamiento();
    }

    private void configurarEstilosGlobales() {
        // Estilo para el título
        lblTitulo.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #4A6CF7;" +
                        "-fx-padding: 0 0 15px 0;"
        );

        // Estilo para la tabla
        tablaClientes.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-background-color: #ffffff;" +
                        "-fx-border-radius: 10px;" +
                        "-fx-background-radius: 10px;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);"
        );
    }

    private void configurarColumnas() {
        colNombreCompleto.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        // Columna Nombre Completo - Negrita y Centrado
        colNombreCompleto.setCellFactory(column -> new TableCell<Cliente, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER; "
                            + "-fx-font-weight: bold; "
                            + "-fx-text-fill: black; "
                            + "-fx-background-color: transparent;");
                }
            }
        });

        // Columna Teléfono - Negrita y Centrado
        colTelefono.setCellFactory(column -> new TableCell<Cliente, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER; "
                            + "-fx-font-weight: bold; "
                            + "-fx-text-fill: black; "
                            + "-fx-background-color: transparent;");
                }
            }
        });

        // Columna Estado - Indicador Rojo y Negrita
        colEstado.setCellFactory(column -> new TableCell<Cliente, String>() {
            private final StackPane container = new StackPane();
            private final Circle indicador = new Circle(5);
            private final Label texto = new Label();

            {
                container.setAlignment(Pos.CENTER);
                HBox caja = new HBox(5, indicador, texto);
                caja.setAlignment(Pos.CENTER);
                container.getChildren().add(caja);

                texto.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            }

            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setGraphic(null);
                } else {
                    texto.setText(estado);
                    if ("Activo".equals(estado)) {
                        indicador.setFill(Color.web("#28A745")); // Verde
                        texto.setStyle("-fx-text-fill: #28A745; -fx-font-weight: bold;");
                    } else {
                        indicador.setFill(Color.web("#DC3545")); // Rojo
                        texto.setStyle("-fx-text-fill: #DC3545; -fx-font-weight: bold;");
                    }
                    setGraphic(container);
                    setStyle("-fx-alignment: CENTER; "
                            + "-fx-background-color: transparent;");
                }
            }
        });

        // Configurar columna de acciones con ícono
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button();

            {
                // Estilo del botón editar
                btnEditar.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-font-size: 16px;" +
                                "-fx-cursor: hand;"
                );

                // Configurar ícono
                btnEditar.setText("✏️");
                btnEditar.setTooltip(new Tooltip("Editar cliente"));

                // Efecto hover
                btnEditar.setOnMouseEntered(e ->
                        btnEditar.setStyle("-fx-background-color: #e0f0ff; -fx-font-size: 16px; -fx-cursor: hand;")
                );
                btnEditar.setOnMouseExited(e ->
                        btnEditar.setStyle("-fx-background-color: transparent; -fx-font-size: 16px; -fx-cursor: hand;")
                );

                btnEditar.setOnAction(event -> {
                    Cliente cliente = getTableView().getItems().get(getIndex());
                    mostrarDialogoEdicion(cliente);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnEditar);
                }
            }
        });
    }

    private void eliminarBarrasDesplazamiento() {
        // Método 1: Estilo CSS para ocultar barras
        tablaClientes.setStyle(tablaClientes.getStyle() +
                " -fx-scroll-bar-policy: never;"
        );

        // Método 2: Eliminar mediante skin después de renderizar
        Platform.runLater(() -> {
            // Buscar y eliminar las barras de desplazamiento
            for (javafx.scene.Node node : tablaClientes.lookupAll(".scroll-bar")) {
                node.setVisible(false);
                node.setManaged(false);
            }

            // Eliminar el área del scrollbar
            for (javafx.scene.Node node : tablaClientes.lookupAll(".corner")) {
                node.setVisible(false);
                node.setManaged(false);
            }

            // Eliminar el recuadro de desplazamiento
            for (javafx.scene.Node node : tablaClientes.lookupAll(".scroll-pane")) {
                if (node instanceof javafx.scene.control.ScrollPane) {
                    ((ScrollPane) node).setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                    ((ScrollPane) node).setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                }
            }
        });
    }

    private void ajustarAnchoColumnas() {
        // Ajustar después de que la tabla se haya renderizado
        Platform.runLater(() -> {
            double anchoTotal = tablaClientes.getWidth();
            if (anchoTotal > 0) {
                // Distribución proporcional del espacio
                double anchoNombre = anchoTotal * 0.40;   // 40%
                double anchoTelefono = anchoTotal * 0.25;  // 25%
                double anchoEstado = anchoTotal * 0.16;    // 16%
                double anchoAcciones = anchoTotal * 0.16;  // 17%

                colNombreCompleto.setPrefWidth(anchoNombre);
                colTelefono.setPrefWidth(anchoTelefono);
                colEstado.setPrefWidth(anchoEstado);
                colAcciones.setPrefWidth(anchoAcciones);

                // Forzar actualización
                tablaClientes.requestLayout();
            }
        });

        // Listener para cambios de tamaño
        tablaClientes.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0) {
                double anchoTotal = newVal.doubleValue();
                colNombreCompleto.setPrefWidth(anchoTotal * 0.40);
                colTelefono.setPrefWidth(anchoTotal * 0.25);
                colEstado.setPrefWidth(anchoTotal * 0.20);
                colAcciones.setPrefWidth(anchoTotal * 0.15);
            }
        });
    }

    private void configurarBusqueda() {
        // Configurar placeholder para búsqueda
        txtBuscar.setPromptText("Buscar cliente...");
        txtBuscar.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-padding: 8px 15px;" +
                        "-fx-background-radius: 20px;" +
                        "-fx-border-radius: 20px;" +
                        "-fx-border-color: #ced4da;" +
                        "-fx-background-color: #ffffff;"
        );

        // Estilo para botón limpiar
        btnLimpiar.setStyle(
                "-fx-background-color: #6C757D;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8px 15px;" +
                        "-fx-background-radius: 20px;" +
                        "-fx-border-radius: 20px;" +
                        "-fx-cursor: hand;"
        );
        btnLimpiar.setOnMouseEntered(e ->
                btnLimpiar.setStyle(
                        "-fx-background-color: #5a6268;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 8px 15px;" +
                                "-fx-background-radius: 20px;" +
                                "-fx-border-radius: 20px;" +
                                "-fx-cursor: hand;"
                )
        );
        btnLimpiar.setOnMouseExited(e ->
                btnLimpiar.setStyle(
                        "-fx-background-color: #6C757D;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 8px 15px;" +
                                "-fx-background-radius: 20px;" +
                                "-fx-border-radius: 20px;" +
                                "-fx-cursor: hand;"
                )
        );

        // Listener para búsqueda en tiempo real
        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            filtrarClientes();
        });
    }

    private void configurarFilas() {
        tablaClientes.setRowFactory(tv -> {
            TableRow<Cliente> row = new TableRow<>();
            // Borde inferior para todas las filas
            row.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 0 0 1px 0;");

            row.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    // Mantener borde inferior incluso cuando está seleccionada
                    row.setStyle("-fx-background-color: #e6f2ff; "
                            + "-fx-border-color: #e0e0e0; "
                            + "-fx-border-width: 0 0 1px 0;");
                } else {
                    row.setStyle("-fx-background-color: #ffffff; "
                            + "-fx-border-color: #e0e0e0; "
                            + "-fx-border-width: 0 0 1px 0;");
                }
            });

            row.setOnMouseEntered(event -> {
                if (!row.isEmpty() && !row.isSelected()) {
                    row.setStyle("-fx-background-color: #e6f2ff; "
                            + "-fx-border-color: #e0e0e0; "
                            + "-fx-border-width: 0 0 1px 0;");
                }
            });

            row.setOnMouseExited(event -> {
                if (!row.isEmpty() && !row.isSelected()) {
                    row.setStyle("-fx-background-color: #ffffff; "
                            + "-fx-border-color: #e0e0e0; "
                            + "-fx-border-width: 0 0 1px 0;");
                }
            });

            return row;
        });
    }

    @FXML
    private void filtrarClientes() {
        String filtro = txtBuscar.getText().trim().toLowerCase();

        if (filtro.isEmpty()) {
            tablaClientes.setItems(clientesOriginales);
            return;
        }

        ObservableList<Cliente> filtrados = FXCollections.observableArrayList();
        for (Cliente cliente : clientesOriginales) {
            if (cliente.getNombreCompleto().toLowerCase().contains(filtro) ||
                    cliente.getTelefono().contains(filtro)) {
                filtrados.add(cliente);
            }
        }

        // RESULTADOS ORDENADOS
        filtrados.sort(Comparator.comparing(Cliente::getNombreCompleto, String.CASE_INSENSITIVE_ORDER));

        tablaClientes.setItems(filtrados);
    }

    @FXML
    private void limpiarFiltro() {
        txtBuscar.clear();
        tablaClientes.setItems(clientesOriginales);
    }

    private void mostrarDialogoEdicion(Cliente cliente) {
        try {
            // SOLUCIÓN CORRECTA:
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/confirmar_edicion_dialog.fxml"));
            VBox dialogContent = loader.load();

            // Obtener el controlador desde el loader
            ConfirmarEdicionDialogController controller = loader.getController();
            controller.setNombreCliente(cliente.getNombreCompleto());

            // Crear el diálogo
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(tablaClientes.getScene().getWindow());
            dialog.initStyle(StageStyle.TRANSPARENT);
            dialog.setScene(new Scene(dialogContent));

            // Animación de entrada
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), dialogContent);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            // Manejar botones
            controller.btnConfirmar.setOnAction(e -> {
                dialog.close();
                abrirVentanaEdicion(cliente);
            });

            controller.btnCancelar.setOnAction(e -> dialog.close());

            // Mostrar diálogo con animación
            dialog.show();
            fadeIn.play();

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo cargar el diálogo de confirmación");
        }
    }

    private void abrirVentanaEdicion(Cliente cliente) {
        try {
            this.clienteEditado = cliente;

            URL fxmlUrl = getClass().getResource("/fxml/editar_cliente.fxml");
            if (fxmlUrl == null) {
                throw new IOException("Archivo FXML no encontrado: /fxml/editar_cliente.fxml");
            }

            // Mensaje de depuración eliminado
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            EditarClienteController controller = loader.getController();
            if (controller == null) {
                throw new IOException("Controlador no inicializado");
            }
            controller.setCliente(cliente);

            Stage stage = new Stage();
            stage.setTitle("Editar Cliente");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            recargarClientes();

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir la ventana de edición:\n" + e.getMessage());
        }
    }

    private void aplicarEfectoExito(TableRow<Cliente> row) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(150), row);
        scale.setFromX(1);
        scale.setFromY(1);
        scale.setToX(1.03);
        scale.setToY(1.03);
        scale.setCycleCount(4); // 2 pulsos (ida y vuelta dos veces)
        scale.setAutoReverse(true);
        scale.play();
    }

    private void recargarClientes() {
        // Guardar el teléfono del cliente editado
        String telefonoEditado = clienteEditado != null ? clienteEditado.getTelefono() : null;

        // Limpiar y recargar la tabla una sola vez
        tablaClientes.getItems().clear();
        cargarClientes();
        clientesOriginales.setAll(tablaClientes.getItems());

        // Encontrar la fila del cliente editado
        if (telefonoEditado != null) {
            for (int i = 0; i < tablaClientes.getItems().size(); i++) {
                Cliente c = tablaClientes.getItems().get(i);
                if (c.getTelefono().equals(telefonoEditado)) {
                    int finalI = i;
                    // Esperar a que la tabla se renderice
                    Platform.runLater(() -> {
                        // Buscar la fila por índice
                        TableRow<Cliente> row = (TableRow<Cliente>) tablaClientes.lookup(".table-row-cell:" + finalI);
                        if (row != null) {
                            aplicarEfectoExito(row);
                        }
                    });
                    break;
                }
            }
        }

        // Limpiar referencia
        clienteEditado = null;
    }

    private void cargarClientes() {
        String sql = "SELECT nombres, apellidos, telefono, tipoMembresia, fecha_vencimiento FROM clientes WHERE activo = 1";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            ObservableList<Cliente> clientesTemp = FXCollections.observableArrayList();

            while (rs.next()) {
                Cliente cliente = new Cliente(
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("telefono"),
                        rs.getString("tipoMembresia"),
                        LocalDate.parse(rs.getString("fecha_vencimiento"))
                );
                clientesTemp.add(cliente);
            }

            // ORDENAMIENTO ALFABÉTICO
            clientesTemp.sort(Comparator.comparing(Cliente::getNombreCompleto, String.CASE_INSENSITIVE_ORDER));
            tablaClientes.getItems().setAll(clientesTemp);

        } catch (SQLException e) {
            mostrarAlerta("Error de Base de Datos", "No se pudieron cargar los clientes");
        }
    }

    @FXML
    private void handleVolver() {
        Stage stage = (Stage) tablaClientes.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleVerInactivos() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/lista_clientes_inactivos.fxml"));
            Stage stage = (Stage) tablaClientes.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo abrir inactivos");
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);

        // Estilo para la alerta
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: #ffffff;" +
                        "-fx-font-size: 14px;" +
                        "-fx-border-radius: 10px;" +
                        "-fx-background-radius: 10px;"
        );

        alert.showAndWait();
    }
}
package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import models.Cliente;
import util.DatabaseUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

public class ListaClientesInactivosController {

    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, String> colNombreCompleto;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colEstado;
    @FXML private TableColumn<Cliente, Void> colAcciones;
    @FXML private TextField txtBuscar;
    @FXML private Button btnLimpiar;
    @FXML private Label lblTitulo;

    private ObservableList<Cliente> clientesOriginales = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarEstilos();
        configurarColumnas();
        cargarClientes();
        configurarBusqueda();
        ajustarAnchoColumnas();
        eliminarBarrasDesplazamiento();
        configurarFilas();
    }

    private void configurarEstilos() {
        lblTitulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #4A6CF7; -fx-padding: 0 0 15px 0;");
        lblTitulo.setAlignment(Pos.CENTER);
        tablaClientes.setStyle("-fx-font-size: 14px; -fx-background-color: #ffffff; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
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
                    setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
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
                    setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
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
                    indicador.setFill(Color.web("#DC3545")); // Rojo
                    texto.setStyle("-fx-text-fill: #DC3545; -fx-font-weight: bold;");
                    setGraphic(container);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        // Columna Acciones - Íconos
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnActivar = new Button("🔄");
            private final Button btnEliminar = new Button("🗑️");
            private final HBox container = new HBox(10, btnActivar, btnEliminar);

            {
                container.setAlignment(Pos.CENTER);

                String estiloBase = "-fx-background-color: transparent; -fx-font-size: 16px; -fx-cursor: hand;";
                btnActivar.setStyle(estiloBase);
                btnEliminar.setStyle(estiloBase);

                btnActivar.setOnMouseEntered(e ->
                        btnActivar.setStyle("-fx-background-color: #e0f0ff; " + estiloBase)
                );
                btnActivar.setOnMouseExited(e ->
                        btnActivar.setStyle(estiloBase)
                );

                btnEliminar.setOnMouseEntered(e ->
                        btnEliminar.setStyle("-fx-background-color: #ffe0e0; " + estiloBase)
                );
                btnEliminar.setOnMouseExited(e ->
                        btnEliminar.setStyle(estiloBase)
                );

                btnActivar.setTooltip(new Tooltip("Reactivar cliente"));
                btnEliminar.setTooltip(new Tooltip("Eliminar cliente permanentemente"));

                btnActivar.setOnAction(event -> {
                    Cliente cliente = getTableView().getItems().get(getIndex());
                    activarCliente(cliente);
                });

                btnEliminar.setOnAction(event -> {
                    Cliente cliente = getTableView().getItems().get(getIndex());
                    eliminarCliente(cliente);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });
    }

    private void cargarClientes() {
        String sql = "SELECT nombres, apellidos, telefono, tipoMembresia, fecha_vencimiento FROM clientes WHERE activo = 0";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Cliente cliente = new Cliente(
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("telefono"),
                        rs.getString("tipoMembresia"),
                        LocalDate.parse(rs.getString("fecha_vencimiento"))
                );
                tablaClientes.getItems().add(cliente);
            }
            clientesOriginales.setAll(tablaClientes.getItems());
        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudieron cargar clientes inactivos");
        }
    }

    private void activarCliente(Cliente cliente) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/renovacion.fxml"));
            Parent root = loader.load();

            RenovacionController controller = loader.getController();
            controller.setClienteInactivo(cliente);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Reactivar Cliente");
            stage.showAndWait();

            recargarClientes();
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo abrir renovación");
        }
    }

    private void eliminarCliente(Cliente cliente) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Eliminación");
        confirm.setHeaderText("¿Eliminar cliente permanentemente?");
        confirm.setContentText("Esta acción borrará todos los registros asociados");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseUtil.getConnection()) {
                conn.setAutoCommit(false);

                String deletePagos = "DELETE FROM pagos WHERE cliente_id = (SELECT id FROM clientes WHERE telefono = ?)";
                try (PreparedStatement stmt = conn.prepareStatement(deletePagos)) {
                    stmt.setString(1, cliente.getTelefono());
                    stmt.executeUpdate();
                }

                String deleteCliente = "DELETE FROM clientes WHERE telefono = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteCliente)) {
                    stmt.setString(1, cliente.getTelefono());
                    stmt.executeUpdate();
                }

                conn.commit();
                recargarClientes();
            } catch (SQLException e) {
                mostrarAlerta("Error", "No se pudo eliminar cliente");
            }
        }
    }

    private void recargarClientes() {
        tablaClientes.getItems().clear();
        cargarClientes();
    }

    private void configurarBusqueda() {
        txtBuscar.setPromptText("Buscar cliente...");
        txtBuscar.setStyle("-fx-font-size: 14px; -fx-padding: 8px 15px; -fx-background-radius: 20px; -fx-border-radius: 20px; -fx-border-color: #ced4da; -fx-background-color: #ffffff;");

        btnLimpiar.setStyle("-fx-background-color: #6C757D; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 20px; -fx-border-radius: 20px; -fx-cursor: hand;");
        btnLimpiar.setOnMouseEntered(e -> btnLimpiar.setStyle("-fx-background-color: #5a6268; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 20px; -fx-border-radius: 20px; -fx-cursor: hand;"));
        btnLimpiar.setOnMouseExited(e -> btnLimpiar.setStyle("-fx-background-color: #6C757D; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 20px; -fx-border-radius: 20px; -fx-cursor: hand;"));

        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> filtrarClientes());
    }

    private void filtrarClientes() {
        String filtro = txtBuscar.getText().trim().toLowerCase();

        if (filtro.isEmpty()) {
            tablaClientes.setItems(clientesOriginales);
            return;
        }

        ObservableList<Cliente> filtrados = FXCollections.observableArrayList();
        for (Cliente cliente : clientesOriginales) {
            if (cliente.getNombreCompleto().toLowerCase().contains(filtro) || cliente.getTelefono().contains(filtro)) {
                filtrados.add(cliente);
            }
        }
        tablaClientes.setItems(filtrados);
    }

    @FXML
    private void limpiarFiltro() {
        txtBuscar.clear();
        tablaClientes.setItems(clientesOriginales);
    }

    private void configurarFilas() {
        tablaClientes.setRowFactory(tv -> {
            TableRow<Cliente> row = new TableRow<>();
            row.setStyle("-fx-background-radius: 5px;");

            row.setOnMouseEntered(event -> {
                if (!row.isEmpty()) {
                    row.setStyle("-fx-background-color: #e6f2ff; -fx-background-radius: 5px;");
                    Tooltip tooltip = new Tooltip(row.getItem().getTooltipText());
                    tooltip.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: #333; -fx-text-fill: white;");
                    Tooltip.install(row, tooltip);
                }
            });
            row.setOnMouseExited(event -> {
                if (!row.isEmpty()) {
                    row.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 5px;");
                }
            });
            return row;
        });
    }

    private void ajustarAnchoColumnas() {
        Platform.runLater(() -> {
            double anchoTotal = tablaClientes.getWidth();
            if (anchoTotal > 0) {
                colNombreCompleto.setPrefWidth(anchoTotal * 0.40);
                colTelefono.setPrefWidth(anchoTotal * 0.25);
                colEstado.setPrefWidth(anchoTotal * 0.16);
                colAcciones.setPrefWidth(anchoTotal * 0.17);
                tablaClientes.requestLayout();
            }
        });

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

    private void eliminarBarrasDesplazamiento() {
        tablaClientes.setStyle(tablaClientes.getStyle() + " -fx-scroll-bar-policy: never;");

        Platform.runLater(() -> {
            for (javafx.scene.Node node : tablaClientes.lookupAll(".scroll-bar")) {
                node.setVisible(false);
                node.setManaged(false);
            }
            for (javafx.scene.Node node : tablaClientes.lookupAll(".corner")) {
                node.setVisible(false);
                node.setManaged(false);
            }
            for (javafx.scene.Node node : tablaClientes.lookupAll(".scroll-pane")) {
                if (node instanceof javafx.scene.control.ScrollPane) {
                    ((ScrollPane) node).setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                    ((ScrollPane) node).setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                }
            }
        });
    }

    @FXML
    private void handleVolver() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/lista_clientes.fxml"));
            Stage stage = (Stage) tablaClientes.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo volver");
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
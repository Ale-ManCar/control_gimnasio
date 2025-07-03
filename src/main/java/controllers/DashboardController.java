package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableRow;
import javafx.scene.control.Button;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.scene.Node;
//import javafx.util.Callback;
import models.Cliente;
import util.DatabaseUtil;
import util.ReporteUtil;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private AnchorPane cardClientes;
    @FXML private AnchorPane cardPagos;
    @FXML private AnchorPane cardVencimientos;
    @FXML private TableView<Cliente> tablaClientesProximosAVencer;
    @FXML private Label lblMensaje;

    // Columnas definidas en FXML
    @FXML private TableColumn<Cliente, String> colCliente;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colVencimiento;
    @FXML private TableColumn<Cliente, Integer> colDiasRestantes;
    @FXML private TableColumn<Cliente, Void> colAlerta;
    @FXML private TableColumn<Cliente, Void> colAccion;

    private MetricCardController ctrlClientes;
    private MetricCardController ctrlPagos;
    private MetricCardController ctrlVencimientos;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // Configurar tabla sin barras de scroll
            configurarTablaSinScroll();

            // Configurar columnas
            colCliente.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
            colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
            colVencimiento.setCellValueFactory(new PropertyValueFactory<>("fecha_vencimiento"));
            colDiasRestantes.setCellValueFactory(new PropertyValueFactory<>("diasRestantes"));

            // Configurar columna de alerta
            colAlerta.setCellFactory(column -> new TableCell<Cliente, Void>() {
                private final Label warningIcon = new Label("⚠");

                {
                    warningIcon.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        Cliente cliente = getTableView().getItems().get(getIndex());
                        setGraphic(cliente.getDiasRestantes() <= 3 ? warningIcon : null);
                    }
                }
            });

            // Configurar columna de acción (botones)
            colAccion.setCellFactory(column -> new TableCell<Cliente, Void>() {
                private final Button btnRenovar = new Button("🔄 Renovar");

                {
                    // Estilo del botón
                    btnRenovar.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;"
                            + " -fx-padding: 5px 10px; -fx-background-radius: 5px;");

                    // Acción al hacer clic
                    btnRenovar.setOnAction(event -> {
                        Cliente cliente = getTableView().getItems().get(getIndex());
                        abrirRenovacionConCliente(cliente);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(btnRenovar);
                    }
                }
            });

            // Centrar contenido en todas las columnas
            centrarContenidoColumnas();

            // Inicializar tarjetas de métricas
            inicializarTarjetasMetricas();

            cargarDatosTarjetas();
            cargarClientesProximosAVencer();

        } catch (Exception e) {
            e.printStackTrace();
            lblMensaje.setText("Error al inicializar el panel.");
        }
    }

    private void configurarTablaSinScroll() {
        // Configurar política de redimensionamiento para eliminar barras de scroll
        tablaClientesProximosAVencer.setFixedCellSize(30);

        // Estilo para ocultar barras de scroll
        tablaClientesProximosAVencer.setStyle(
                "-fx-scroll-bar-policy-vertical: never;" +
                        "-fx-scroll-bar-policy-horizontal: never;"
        );

        // Ajustar política de redimensionamiento
        tablaClientesProximosAVencer.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void inicializarTarjetasMetricas() throws IOException {
        FXMLLoader loaderClientes = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneClientes = loaderClientes.load();
        ctrlClientes = loaderClientes.getController();
        ctrlClientes.setTitulo("Clientes Activos");
        paneClientes.prefWidthProperty().bind(cardClientes.widthProperty());
        paneClientes.prefHeightProperty().bind(cardClientes.heightProperty());
        cardClientes.getChildren().add(paneClientes);
        paneClientes.setOnMouseClicked(event -> abrirListaClientes());

        FXMLLoader loaderPagos = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane panePagos = loaderPagos.load();
        ctrlPagos = loaderPagos.getController();
        ctrlPagos.setTitulo("Pagos Recibidos");
        panePagos.prefWidthProperty().bind(cardPagos.widthProperty());
        panePagos.prefHeightProperty().bind(cardPagos.heightProperty());
        cardPagos.getChildren().add(panePagos);

        FXMLLoader loaderVencimientos = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane paneVencimientos = loaderVencimientos.load();
        ctrlVencimientos = loaderVencimientos.getController();
        ctrlVencimientos.setTitulo("Próximos a Vencer");
        paneVencimientos.prefWidthProperty().bind(cardVencimientos.widthProperty());
        paneVencimientos.prefHeightProperty().bind(cardVencimientos.heightProperty());
        cardVencimientos.getChildren().add(paneVencimientos);
    }

    private void centrarContenidoColumnas() {
        centrarColumna(colCliente);
        centrarColumna(colTelefono);
        centrarColumna(colVencimiento);
        centrarColumna(colDiasRestantes);
        colAlerta.setStyle("-fx-alignment: CENTER;");
        colAccion.setStyle("-fx-alignment: CENTER;");
    }

    private <T> void centrarColumna(TableColumn<Cliente, T> columna) {
        columna.setStyle("-fx-alignment: CENTER;");
        columna.setCellFactory(column -> new TableCell<Cliente, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    setAlignment(Pos.CENTER);
                    setStyle("-fx-text-fill: black;");
                }
            }
        });
    }

    private void abrirListaClientes() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/lista_clientes.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Lista de Clientes Activos");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al abrir lista de clientes");
        }
    }

    private void cargarDatosTarjetas() {
        try (Connection conn = DatabaseUtil.getConnection()) {
            // Clientes activos
            String sqlClientes = "SELECT COUNT(*) AS total FROM clientes WHERE activo = 1";
            try (PreparedStatement ps = conn.prepareStatement(sqlClientes);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ctrlClientes.setValor(rs.getString("total"));
                }
            }

            // Pagos recibidos
            double totalPagos = DatabaseUtil.obtenerTotalPagosDelMesActual();
            ctrlPagos.setValor(String.format("$ %.2f", totalPagos));

            // Próximos a vencer
            String sqlVencimientos = "SELECT COUNT(*) AS total FROM clientes WHERE fecha_vencimiento BETWEEN date('now') AND date('now', '+7 days')";
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

    private void abrirRenovacionConCliente(Cliente cliente) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/renovacion.fxml"));
            Parent root = loader.load();

            // Obtener el controlador y pasar el cliente
            RenovacionController controller = loader.getController();
            controller.precargarCliente(cliente);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Renovar Membresía");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al abrir renovación.");
        }
    }

    private void cargarClientesProximosAVencer() {
        ObservableList<Cliente> clientes = FXCollections.observableArrayList();
        String sql = "SELECT nombres, apellidos, telefono, fecha_vencimiento, " +
                "(julianday(fecha_vencimiento) - julianday(date('now'))) AS dias_restantes " +
                "FROM clientes " +
                "WHERE fecha_vencimiento BETWEEN date('now') AND date('now', '+7 days') " +
                "ORDER BY fecha_vencimiento";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            boolean hayClientes = false;
            while (rs.next()) {
                hayClientes = true;
                Cliente cliente = new Cliente(
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("telefono"),
                        LocalDate.parse(rs.getString("fecha_vencimiento"))
                );
                cliente.setDiasRestantes(rs.getInt("dias_restantes"));
                clientes.add(cliente);
            }

            tablaClientesProximosAVencer.setItems(clientes);
            configurarEstilosFilas();
            ajustarAlturaTabla(); // Ajustar altura dinámicamente

            lblMensaje.setText(hayClientes ? "" : "No hay clientes próximos a vencer en los próximos 7 días.");

        } catch (SQLException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al cargar clientes próximos a vencer.");
        }
    }

    private void ajustarAlturaTabla() {
        int filas = tablaClientesProximosAVencer.getItems().size();
        double alturaPorFila = 40; // Aumentado de 30 a 40
        double alturaCabecera = 40; // Aumentado de 30 a 40

        double alturaTotal = Math.max(150, (filas * alturaPorFila) + alturaCabecera);

        // Aplicar nueva altura
        tablaClientesProximosAVencer.setPrefHeight(alturaTotal);
    }

    private void configurarEstilosFilas() {
        tablaClientesProximosAVencer.setRowFactory(tv -> new TableRow<Cliente>() {
            @Override
            protected void updateItem(Cliente cliente, boolean empty) {
                super.updateItem(cliente, empty);
                if (cliente == null || empty) {
                    setStyle("");
                } else {
                    int dias = cliente.getDiasRestantes();
                    String baseStyle = "";

                    if (dias >= 5 && dias <= 7) {
                        baseStyle = "-fx-background-color: #e8f5e9;"; // Verde
                    } else if (dias >= 3 && dias <= 4) {
                        baseStyle = "-fx-background-color: #fff3e0;"; // Amarillo
                    } else if (dias >= 0 && dias <= 3) {
                        baseStyle = "-fx-background-color: #ffebee;"; // Rojo
                    } else if (dias < 0) {
                        baseStyle = "-fx-background-color: #ffcdd2;"; // Vencidos
                    }

                    setStyle(baseStyle + (isSelected() ?
                            " -fx-font-weight: bold; -fx-text-fill: black;" :
                            " -fx-text-fill: black;"));
                }
            }
        });
    }

    @FXML
    private void handleVerIngresosMensuales(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ingresos_mensuales.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Ingresos Mensuales Detallados");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al abrir ingresos mensuales");
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
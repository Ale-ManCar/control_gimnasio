package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import util.DatabaseUtil;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Map;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private AnchorPane cardActivos;
    @FXML private AnchorPane cardInactivos;
    @FXML private AnchorPane cardMembresias;
    @FXML private AnchorPane cardPorVencer;
    @FXML private Label lblMensaje;

    private MetricCardController ctrlActivos;
    private MetricCardController ctrlInactivos;
    private MetricCardController ctrlMembresias;
    private MetricCardController ctrlPorVencer;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            inicializarTarjetas();
            cargarDatos();
        } catch (IOException e) {
            lblMensaje.setText("Error al cargar tarjetas");
        }
    }

    private void inicializarTarjetas() throws IOException {
        ctrlActivos = cargarTarjeta(cardActivos, "Clientes Activos");
        ctrlInactivos = cargarTarjeta(cardInactivos, "Clientes Inactivos");
        ctrlMembresias = cargarTarjeta(cardMembresias, "Membresías del Mes");
        ctrlPorVencer = cargarTarjeta(cardPorVencer, "Próximos a Vencer");
    }

    private MetricCardController cargarTarjeta(AnchorPane contenedor, String titulo) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/metric_card.fxml"));
        Pane pane = loader.load();
        MetricCardController controller = loader.getController();
        controller.setTitulo(titulo);
        pane.prefWidthProperty().bind(contenedor.widthProperty());
        pane.prefHeightProperty().bind(contenedor.heightProperty());
        contenedor.getChildren().add(pane);
        return controller;
    }

    private void cargarDatos() {
        try {
            Map<String, Integer> stats = DatabaseUtil.getAdminStats();
            ctrlActivos.setValor(String.valueOf(stats.getOrDefault("clientes_activos", 0)));
            ctrlInactivos.setValor(String.valueOf(stats.getOrDefault("clientes_inactivos", 0)));
            ctrlMembresias.setValor(String.valueOf(stats.getOrDefault("membresias_mes", 0)));
            ctrlPorVencer.setValor(String.valueOf(stats.getOrDefault("por_vencer", 0)));
        } catch (SQLException e) {
            lblMensaje.setText("No se pudieron cargar las estadísticas");
        }
    }

    @FXML
    private void abrirAuditoria() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auditoria.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Auditoría");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            lblMensaje.setText("Error al abrir auditoría");
        }
    }
}
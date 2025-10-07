package util;

import controllers.MetricCardController;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.sql.SQLException;
/**
 * Servicio para gestionar datos del dashboard del administrador.
 */
public class DashboardService {

    /**
     * Carga una tarjeta métrica en el contenedor indicado.
     */
    public static MetricCardController cargarTarjeta(AnchorPane contenedor, String titulo) throws IOException {
        FXMLLoader loader = new FXMLLoader(DashboardService.class.getResource("/fxml/components/metric_card.fxml"));
        AnchorPane pane = loader.load();
        AnchorPane.setTopAnchor(pane, 0.0);
        AnchorPane.setRightAnchor(pane, 0.0);
        AnchorPane.setBottomAnchor(pane, 0.0);
        AnchorPane.setLeftAnchor(pane, 0.0);
        MetricCardController controller = loader.getController();
        controller.setTitulo(titulo);
        contenedor.getChildren().setAll(pane);
        return controller;
    }

    /**
     * Obtiene las métricas del dashboard del administrador.
     */
    public static AdminMetrics obtenerMetricasAdmin() throws SQLException {
        int clientesActivos = DatabaseUtil.contarClientesActivos();
        int coaches = DatabaseUtil.contarCoaches();
        double totalPagos = DatabaseUtil.obtenerTotalPagosDelMesActual();
        double totalVentas = DatabaseUtil.obtenerTotalVentasDelMes();
        double totalEgresos = DatabaseUtil.obtenerTotalEgresosDelMes();
        double ingresos = (totalPagos + totalVentas) - totalEgresos;
        return new AdminMetrics(clientesActivos, ingresos, coaches);
    }

    /**
     * Contenedor para métricas del dashboard.
     */
    public static class AdminMetrics {
        private final int clientesActivos;
        private final double ingresos;
        private final int coaches;

        public AdminMetrics(int clientesActivos, double ingresos, int coaches) {
            this.clientesActivos = clientesActivos;
            this.ingresos = ingresos;
            this.coaches = coaches;
        }

        public int getClientesActivos() { return clientesActivos; }
        public double getIngresos() { return ingresos; }
        public int getCoaches() { return coaches; }
    }
}
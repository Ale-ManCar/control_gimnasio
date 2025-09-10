package util;

import controllers.MetricCardController;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * Servicio para gestionar datos del dashboard del administrador.
 */
public class DashboardService {

    /**
     * Carga una tarjeta métrica en el contenedor indicado.
     */
    public static MetricCardController cargarTarjeta(AnchorPane contenedor, String titulo) throws IOException {
        FXMLLoader loader = new FXMLLoader(DashboardService.class.getResource("/fxml/components/metric_card.fxml"));
        Pane pane = loader.load();
        MetricCardController controller = loader.getController();
        controller.setTitulo(titulo);
        pane.prefWidthProperty().bind(contenedor.widthProperty());
        pane.prefHeightProperty().bind(contenedor.heightProperty());
        contenedor.getChildren().add(pane);
        return controller;
    }

    /**
     * Obtiene las métricas del dashboard del administrador.
     */
    public static AdminMetrics obtenerMetricasAdmin() throws SQLException {
        Map<String, Integer> stats = DatabaseUtil.getAdminStats();
        double totalPagos = DatabaseUtil.obtenerTotalPagosDelMesActual();
        double totalVentas = DatabaseUtil.obtenerTotalVentasDelMes();
        double totalEgresos = DatabaseUtil.obtenerTotalEgresosDelMes();
        double ingresos = (totalPagos + totalVentas) - totalEgresos;
        return new AdminMetrics(
                stats.getOrDefault("clientes_activos", 0),
                stats.getOrDefault("clientes_morosos", 0),
                ingresos,
                stats.getOrDefault("por_vencer", 0)
        );
    }

    /**
     * Contenedor para métricas del dashboard.
     */
    public static class AdminMetrics {
        private final int clientesActivos;
        private final int morosos;
        private final double ingresos;
        private final int porVencer;

        public AdminMetrics(int clientesActivos, int morosos, double ingresos, int porVencer) {
            this.clientesActivos = clientesActivos;
            this.morosos = morosos;
            this.ingresos = ingresos;
            this.porVencer = porVencer;
        }

        public int getClientesActivos() { return clientesActivos; }
        public int getMorosos() { return morosos; }
        public double getIngresos() { return ingresos; }
        public int getPorVencer() { return porVencer; }
    }
}
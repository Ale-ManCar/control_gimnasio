package util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EstadoClienteService {

    public static void iniciarActualizacionDiaria() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            actualizarEstadosClientes();
        }, 0, 1, TimeUnit.DAYS);
    }

    private static void actualizarEstadosClientes() {
        String sql = "UPDATE clientes SET activo = 0 WHERE date(fecha_vencimiento) < date('now')";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error actualizando estados: " + e.getMessage());
        }
    }
}
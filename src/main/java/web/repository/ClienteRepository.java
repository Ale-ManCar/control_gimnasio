package web.repository;

import util.DatabaseUtil;
import web.dto.ClienteDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ClienteRepository {
    private static final String LISTADO_SQL =
            "SELECT id, nombres, apellidos, telefono, telefono_visible, tipoMembresia, " +
                    "fechaInicio, fecha_vencimiento, monto_pago, activo FROM clientes ORDER BY nombres, apellidos";

    public List<ClienteDto> findAll() {
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(LISTADO_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            List<ClienteDto> clientes = new ArrayList<>();
            while (resultSet.next()) {
                clientes.add(mapRow(resultSet));
            }
            return clientes;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo obtener el listado de clientes", e);
        }
    }

    private ClienteDto mapRow(ResultSet resultSet) throws SQLException {
        LocalDate fechaInicio = parseDate(resultSet.getString("fechaInicio"));
        LocalDate fechaVencimiento = parseDate(resultSet.getString("fecha_vencimiento"));
        return new ClienteDto(
                resultSet.getInt("id"),
                resultSet.getString("nombres"),
                resultSet.getString("apellidos"),
                resultSet.getString("telefono"),
                resultSet.getString("telefono_visible"),
                resultSet.getString("tipoMembresia"),
                fechaInicio,
                fechaVencimiento,
                resultSet.getDouble("monto_pago"),
                resultSet.getBoolean("activo")
        );
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value);
    }
}

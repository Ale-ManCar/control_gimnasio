package web.dto;

import java.time.LocalDate;

public record ClienteDto(
        int id,
        String nombres,
        String apellidos,
        String telefono,
        String telefonoVisible,
        String tipoMembresia,
        LocalDate fechaInicio,
        LocalDate fechaVencimiento,
        double montoPago,
        boolean activo
) {
}

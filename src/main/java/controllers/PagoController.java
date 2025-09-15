package controllers;

import java.sql.SQLException;

import util.AuditoriaUtil;
import util.DatabaseUtil;
import util.SessionManager;

public class PagoController {
    private PagoController() {
    }

    public static void anularPago(int pagoId) throws SQLException {
        DatabaseUtil.anularPago(pagoId);
        int usuarioId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 0;
        AuditoriaUtil.registrarAccion(usuarioId, "ANULAR_PAGO", "Pago " + pagoId);
    }
}
package util;

import models.Role;
import models.User;

public class PermisoUtil {
    private PermisoUtil() {}

    public static void registrarAccesoDenegado(Role requerido) {
        User usuario = SessionManager.getCurrentUser();
        int usuarioId = usuario != null ? usuario.getId() : 0;
        String detalle = "Rol requerido: " + requerido + ", rol actual: " +
                (usuario != null ? usuario.getRole() : "N/A");
        AuditoriaUtil.registrarAccion(usuarioId, "ACCESO_DENEGADO", detalle);
    }
}
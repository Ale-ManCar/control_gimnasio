package util;

import models.User;
import models.Role;

public class SessionManager {
    private static User currentUser;
    private static int turnoId = -1;

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
        turnoId = -1;
    }

    public static int getTurnoId() {
        return turnoId;
    }

    public static void setTurnoId(int id) {
        turnoId = id;
    }

    public static boolean tienePermiso(Role requerido) {
        boolean permitido = currentUser != null && currentUser.getRole() == requerido;
        if (!permitido) {
            PermisoUtil.registrarAccesoDenegado(requerido);
        }
        return permitido;
    }
}
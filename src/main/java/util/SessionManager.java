package util;

import models.User;
import models.Role;
import util.DatabaseUtil;
import util.UserService;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class SessionManager {
    private static User currentUser;
    private static int turnoId = -1;
    private static LocalDateTime lastActivity;

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
        turnoId = -1;
        lastActivity = null;
    }

    public static int getTurnoId() {
        return turnoId;
    }

    public static void setTurnoId(int id) {
        turnoId = id;
    }

    public static boolean login(String username, String password, Role role) {
        User user = DatabaseUtil.obtenerUsuario(username, password);
        if (user != null && user.getRole() == role) {
            setCurrentUser(user);
            actualizarActividad();
            try {
                UserService.actualizarLastLogin(user.getId());
                user.setLastLogin(java.time.LocalDateTime.now());
                UserService.registrarActividad(user, "Login");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public static boolean tienePermiso(Role requerido) {
        boolean permitido = currentUser != null && currentUser.getRole() == requerido;
        if (!permitido) {
            PermisoUtil.registrarAccesoDenegado(requerido);
        }
        return permitido;
    }

    public static void actualizarActividad() {
        lastActivity = LocalDateTime.now();
    }

    public static LocalDateTime getLastActivity() {
        return lastActivity;
    }
}
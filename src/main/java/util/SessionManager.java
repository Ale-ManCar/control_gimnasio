package util;

import models.Usuario;

public class SessionManager {
    private static Usuario usuarioActual;

    public static void setUsuarioActual(Usuario usuario) {
        usuarioActual = usuario;
    }

    public static Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public static String getRol() {
        return usuarioActual != null ? usuarioActual.getRol() : null;
    }

    public static boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(getRol());
    }

    public static void clear() {
        usuarioActual = null;
    }
}
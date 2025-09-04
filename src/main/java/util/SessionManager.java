package util;

import models.User;

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
}
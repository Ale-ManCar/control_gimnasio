package util;

import javafx.collections.ObservableList;
import models.User;

import java.sql.SQLException;

public class UserService {

    public static void crearUsuario(User user) throws SQLException {
        DatabaseUtil.crearUsuario(user);
    }

    public static void editarUsuario(User user) throws SQLException {
        DatabaseUtil.actualizarUsuario(user);
    }

    public static void eliminarUsuario(int userId) throws SQLException {
        DatabaseUtil.eliminarUsuario(userId);
    }

    public static ObservableList<User> listarUsuarios() throws SQLException {
        return DatabaseUtil.listarUsuarios();
    }

    public static void registrarActividad(User user, String accion) throws SQLException {
        user.incrementarAcciones();
        DatabaseUtil.registrarAccion(user.getId(), accion);
    }

    public static void actualizarLastLogin(int userId) throws SQLException {
        DatabaseUtil.actualizarLastLogin(userId);
    }
}
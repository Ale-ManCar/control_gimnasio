package models;

public class User {
    private int id;
    private String username;
    private String password;
    private Role role;
    private java.time.LocalDateTime lastLogin;
    private int accionesRealizadas;

    public User(int id, String username, String password, Role role) {
        this(id, username, password, role, null, 0);
    }

    public User(int id, String username, String password, Role role, java.time.LocalDateTime lastLogin, int accionesRealizadas) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.lastLogin = lastLogin;
        this.accionesRealizadas = accionesRealizadas;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
    public java.time.LocalDateTime getLastLogin() { return lastLogin; }
    public int getAccionesRealizadas() { return accionesRealizadas; }

    public void setLastLogin(java.time.LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public void incrementarAcciones() {
        this.accionesRealizadas++;
    }
}
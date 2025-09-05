package models;

public class AuditoriaUsuario {
    private final int id;
    private final String usuario;
    private final String accion;
    private final String fecha;

    public AuditoriaUsuario(int id, String usuario, String accion, String fecha) {
        this.id = id;
        this.usuario = usuario;
        this.accion = accion;
        this.fecha = fecha;
    }

    public int getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getAccion() {
        return accion;
    }

    public String getFecha() {
        return fecha;
    }
}
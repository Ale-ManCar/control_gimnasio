package models;

public class Auditoria {
    private final int id;
    private final String usuario;
    private final String accion;
    private final String detalle;
    private final String timestamp;

    public Auditoria(int id, String usuario, String accion, String detalle, String timestamp) {
        this.id = id;
        this.usuario = usuario;
        this.accion = accion;
        this.detalle = detalle;
        this.timestamp = timestamp;
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

    public String getDetalle() {
        return detalle;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
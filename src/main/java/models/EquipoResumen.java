package models;

public class EquipoResumen {
    private int equipoId;
    private String nombre;
    private String tipo;
    private int cantidadInicial;
    private int altas;
    private int bajas;
    private int cantidadFinal;
    private String fechaUltimoCambio;

    public EquipoResumen() {
    }

    public EquipoResumen(int equipoId, String nombre, String tipo, int cantidadInicial, int altas, int bajas, int cantidadFinal,
                         String fechaUltimoCambio) {
        this.equipoId = equipoId;
        this.nombre = nombre;
        this.tipo = tipo;
        this.cantidadInicial = cantidadInicial;
        this.altas = altas;
        this.bajas = bajas;
        this.cantidadFinal = cantidadFinal;
        this.fechaUltimoCambio = fechaUltimoCambio;
    }

    public int getEquipoId() {
        return equipoId;
    }

    public void setEquipoId(int equipoId) {
        this.equipoId = equipoId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getCantidadInicial() {
        return cantidadInicial;
    }

    public void setCantidadInicial(int cantidadInicial) {
        this.cantidadInicial = cantidadInicial;
    }

    public int getAltas() {
        return altas;
    }

    public void setAltas(int altas) {
        this.altas = altas;
    }

    public int getBajas() {
        return bajas;
    }

    public void setBajas(int bajas) {
        this.bajas = bajas;
    }

    public int getCantidadFinal() {
        return cantidadFinal;
    }

    public void setCantidadFinal(int cantidadFinal) {
        this.cantidadFinal = cantidadFinal;
    }

    public int getVariacion() {
        return altas - bajas;
    }

    public String getFechaUltimoCambio() {
        return fechaUltimoCambio;
    }

    public void setFechaUltimoCambio(String fechaUltimoCambio) {
        this.fechaUltimoCambio = fechaUltimoCambio;
    }
}

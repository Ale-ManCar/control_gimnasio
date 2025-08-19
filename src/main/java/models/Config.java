package models;

public class Config {
    private double planBasico;
    private double planPremium;
    private int umbralStock;
    private String plantillaBienvenida;
    private String rutaReportes;
    private String rutaAdjuntos;

    public double getPlanBasico() {
        return planBasico;
    }

    public void setPlanBasico(double planBasico) {
        this.planBasico = planBasico;
    }

    public double getPlanPremium() {
        return planPremium;
    }

    public void setPlanPremium(double planPremium) {
        this.planPremium = planPremium;
    }

    public int getUmbralStock() {
        return umbralStock;
    }

    public void setUmbralStock(int umbralStock) {
        this.umbralStock = umbralStock;
    }

    public String getPlantillaBienvenida() {
        return plantillaBienvenida;
    }

    public void setPlantillaBienvenida(String plantillaBienvenida) {
        this.plantillaBienvenida = plantillaBienvenida;
    }

    public String getRutaReportes() {
        return rutaReportes;
    }

    public void setRutaReportes(String rutaReportes) {
        this.rutaReportes = rutaReportes;
    }

    public String getRutaAdjuntos() {
        return rutaAdjuntos;
    }

    public void setRutaAdjuntos(String rutaAdjuntos) {
        this.rutaAdjuntos = rutaAdjuntos;
    }
}
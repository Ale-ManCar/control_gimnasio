package models;

public class Turno {
    private int id;
    private int usuario_id;
    private String fecha_inicio;
    private String fecha_fin;
    private String stock_inicial;
    private String stock_final;
    private double ingresos_ventas;
    private double ingresos_clientes;
    private String resumen_generado;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUsuario_id() { return usuario_id; }
    public void setUsuario_id(int usuario_id) { this.usuario_id = usuario_id; }

    public String getFecha_inicio() { return fecha_inicio; }
    public void setFecha_inicio(String fecha_inicio) { this.fecha_inicio = fecha_inicio; }

    public String getFecha_fin() { return fecha_fin; }
    public void setFecha_fin(String fecha_fin) { this.fecha_fin = fecha_fin; }

    public String getStock_inicial() { return stock_inicial; }
    public void setStock_inicial(String stock_inicial) { this.stock_inicial = stock_inicial; }

    public String getStock_final() { return stock_final; }
    public void setStock_final(String stock_final) { this.stock_final = stock_final; }

    public double getIngresos_ventas() { return ingresos_ventas; }
    public void setIngresos_ventas(double ingresos_ventas) { this.ingresos_ventas = ingresos_ventas; }

    public double getIngresos_clientes() { return ingresos_clientes; }
    public void setIngresos_clientes(double ingresos_clientes) { this.ingresos_clientes = ingresos_clientes; }

    public String getResumenGenerado() { return resumen_generado; }
    public void setResumenGenerado(String resumen_generado) { this.resumen_generado = resumen_generado; }
}
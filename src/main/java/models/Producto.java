package models;

public class Producto {
    private int id;
    private String nombre;
    private int stock; // Para PACA: unidades | Para KG/LB: número de scoops/servicios
    private double precioVenta; // Precio por unidad/scoop
    private String tipo; // PACA, KG, LB
    private double precioCompra; // Precio de compra (PACA: por paca | KG/LB: por envase completo)
    private int unidadesPorPaca; // Solo para tipo PACA
    private double pesoTotal; // Para KG/LB: peso total del envase en kg/lb
    private double pesoScoop; // Para KG/LB: peso por scoop en gramos
    private int umbral;

    public Producto() {}

    // Constructor completo
    public Producto(String nombre, int stock, double precioVenta, String tipo,
                    double precioCompra, int unidadesPorPaca, double pesoTotal, double pesoScoop) {
        this.nombre = nombre;
        this.stock = stock;
        this.precioVenta = precioVenta;
        this.tipo = tipo;
        this.precioCompra = precioCompra;
        this.unidadesPorPaca = unidadesPorPaca;
        this.pesoTotal = pesoTotal;
        this.pesoScoop = pesoScoop;
        this.umbral = 0;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public double getPrecio() { return precioVenta; }
    public void setPrecio(double precioVenta) { this.precioVenta = precioVenta; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public double getPrecioCompra() { return precioCompra; }
    public void setPrecioCompra(double precioCompra) { this.precioCompra = precioCompra; }
    public int getUnidadesPorPaca() { return unidadesPorPaca; }
    public void setUnidadesPorPaca(int unidadesPorPaca) { this.unidadesPorPaca = unidadesPorPaca; }
    public double getPesoTotal() { return pesoTotal; }
    public void setPesoTotal(double pesoTotal) { this.pesoTotal = pesoTotal; }
    public double getPesoScoop() { return pesoScoop; }
    public void setPesoScoop(double pesoScoop) { this.pesoScoop = pesoScoop; }
    public int getUmbral() { return umbral; }
    public void setUmbral(int umbral) { this.umbral = umbral; }

    // Método para calcular ganancia por unidad/scoop
    public double getGananciaPorUnidad() {
        if (tipo.equals("PACA")) {
            double costoPorUnidad = precioCompra / unidadesPorPaca;
            return precioVenta - costoPorUnidad;
        } else { // KG o LB
            double totalScoops = calcularTotalScoops();
            if (totalScoops > 0) {
                double costoPorScoop = precioCompra / totalScoops;
                return precioVenta - costoPorScoop;
            }
            return 0;
        }
    }

    // Calcula el número total de scoops/servicios
    public int calcularTotalScoops() {
        if (tipo.equals("KG")) {
            return (int) ((pesoTotal * 1000) / pesoScoop); // Convertir kg a gramos
        } else if (tipo.equals("LB")) {
            return (int) ((pesoTotal * 453.592) / pesoScoop); // Convertir lb a gramos
        }
        return stock; // Para PACA
    }

    // Método para actualizar el stock basado en peso (solo KG/LB)
    public void actualizarStockDesdePeso() {
        if (tipo.equals("KG") || tipo.equals("LB")) {
            this.stock = calcularTotalScoops();
        }
    }

    // Representación textual para debugging
    @Override
    public String toString() {
        return String.format(
                "Producto [nombre=%s, tipo=%s, stock=%d, precioVenta=%.2f, precioCompra=%.2f, umbral=%d]",
                nombre, tipo, stock, precioVenta, precioCompra, umbral
        );
    }
}
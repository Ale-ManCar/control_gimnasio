package util;

import models.Producto;

public final class StockAlertUtil {

    private StockAlertUtil() {
    }

    public enum StockLevel {
        OPTIMO,
        PREVENCION,
        CRITICO
    }

    public static class StockStatus {
        private final StockLevel level;
        private final int stockActual;
        private final int umbral;
        private final int objetivo;
        private final int puntoMedio;

        private StockStatus(StockLevel level, int stockActual, int umbral, int objetivo, int puntoMedio) {
            this.level = level;
            this.stockActual = stockActual;
            this.umbral = umbral;
            this.objetivo = objetivo;
            this.puntoMedio = puntoMedio;
        }

        public StockLevel getLevel() {
            return level;
        }

        public int getStockActual() {
            return stockActual;
        }

        public int getUmbral() {
            return umbral;
        }

        public int getObjetivo() {
            return objetivo;
        }

        public int getPuntoMedio() {
            return puntoMedio;
        }

        public String getTooltipText() {
            String situacion;
            switch (level) {
                case OPTIMO -> situacion = "Inventario saludable";
                case PREVENCION -> situacion = "Inventario en observación";
                default -> situacion = "Inventario crítico";
            }
            return String.format(
                    "%s\nActual: %d | Punto medio: %d\nUmbral: %d | Objetivo: %d",
                    situacion,
                    stockActual,
                    puntoMedio,
                    umbral,
                    objetivo
            );
        }

        public String getSuggestedColor() {
            return switch (level) {
                case OPTIMO -> "#d0f0c0";
                case PREVENCION -> "#fff3cd";
                default -> "#f8d7da";
            };
        }
    }

    public static StockStatus evaluate(Producto producto) {
        if (producto == null) {
            return new StockStatus(StockLevel.PREVENCION, 0, 0, 0, 0);
        }
        return evaluate(producto.getStock(), producto.getUmbral(), producto.getStockObjetivo());
    }

    public static StockStatus evaluate(int stock, int umbral, int objetivo) {
        int umbralSeguro = Math.max(0, umbral);
        int objetivoSeguro = Math.max(umbralSeguro, objetivo);
        int puntoMedio;
        if (objetivoSeguro == umbralSeguro) {
            puntoMedio = umbralSeguro;
        } else {
            puntoMedio = (int) Math.ceil((umbralSeguro + objetivoSeguro) / 2.0);
        }

        StockLevel nivel;
        if (stock < umbralSeguro) {
            nivel = StockLevel.CRITICO;
        } else if (stock >= puntoMedio) {
            nivel = StockLevel.OPTIMO;
        } else {
            nivel = StockLevel.PREVENCION;
        }

        return new StockStatus(nivel, stock, umbralSeguro, objetivoSeguro, puntoMedio);
    }
}
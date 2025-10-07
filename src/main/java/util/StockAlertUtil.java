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
        private final int stockInicial;
        private final int puntoMedio;

        private StockStatus(StockLevel level, int stockActual, int umbral, int stockInicial, int puntoMedio) {
            this.level = level;
            this.stockActual = stockActual;
            this.umbral = umbral;
            this.stockInicial = stockInicial;
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

        public int getStockInicial() {
            return stockInicial;
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
            String detalleBase = String.format(
                    "%s\nActual: %d | Punto medio: %d\nUmbral: %d",
                    situacion,
                    stockActual,
                    puntoMedio,
                    umbral
            );
            if (stockInicial > 0) {
                return detalleBase + String.format(" | Stock inicial: %d", stockInicial);
            }
            return detalleBase;
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
        return evaluate(producto.getStock(), producto.getUmbral(), producto.getStockInicial());
    }

    public static StockStatus evaluate(int stock, int umbral, int stockInicial) {
        int umbralSeguro = Math.max(0, umbral);
        int inicialSeguro;
        if (stockInicial > 0) {
            inicialSeguro = Math.max(umbralSeguro, stockInicial);
        } else {
            inicialSeguro = Math.max(umbralSeguro, stock);
        }
        int puntoMedio = (int) Math.ceil((umbralSeguro + inicialSeguro) / 2.0);

        StockLevel nivel;
        if (stock < umbralSeguro) {
            nivel = StockLevel.CRITICO;
        } else if (stock >= puntoMedio) {
            nivel = StockLevel.OPTIMO;
        } else {
            nivel = StockLevel.PREVENCION;
        }

        return new StockStatus(nivel, stock, umbralSeguro, stockInicial, puntoMedio);
    }
}
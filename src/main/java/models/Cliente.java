package models;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Cliente {
    private final StringProperty nombres;
    private final StringProperty apellidos;
    private final StringProperty telefono;
    private final StringProperty fecha_vencimiento;
    private final StringProperty tipoMembresia;
    private final IntegerProperty diasRestantes = new SimpleIntegerProperty(0);
    private final BooleanProperty presenteHoy = new SimpleBooleanProperty(false);

    public Cliente(String nombres, String apellidos, String telefono, String tipoMembresia, LocalDate fecha_vencimiento) {
        this.nombres = new SimpleStringProperty(nombres);
        this.apellidos = new SimpleStringProperty(apellidos);
        this.telefono = new SimpleStringProperty(telefono);
        this.tipoMembresia = new SimpleStringProperty(tipoMembresia);
        this.fecha_vencimiento = new SimpleStringProperty(fecha_vencimiento.toString());
        setDiasRestantes();
    }

    public Cliente(String nombres, String apellidos, String telefono, LocalDate fecha_vencimiento) {
        this(nombres, apellidos, telefono, "No definido", fecha_vencimiento);
    }

    public Cliente(String nombres, String telefono, LocalDate fecha_vencimiento) {
        this(nombres, "", telefono, "No definido", fecha_vencimiento);
    }

    public StringProperty nombresProperty() { return nombres; }
    public StringProperty apellidosProperty() { return apellidos; }
    public StringProperty telefonoProperty() { return telefono; }
    public StringProperty fechaVencimientoProperty() { return fecha_vencimiento; }
    public StringProperty tipoMembresiaProperty() { return tipoMembresia; }
    public IntegerProperty diasRestantesProperty() { return diasRestantes; }
    public BooleanProperty presenteHoyProperty() { return presenteHoy; }

    public String getNombres() { return nombres.get(); }
    public String getApellidos() { return apellidos.get(); }
    public String getTelefono() { return telefono.get(); }
    public String getTipoMembresia() { return tipoMembresia.get(); }
    public String getFecha_vencimiento() { return fecha_vencimiento.get(); }
    public int getDiasRestantes() { return diasRestantes.get(); }
    public boolean isPresenteHoy() { return presenteHoy.get(); }

    public String getEstado() {
        if (fecha_vencimiento.get() == null || fecha_vencimiento.get().isEmpty())
            return "Inactivo";

        LocalDate vencimiento = LocalDate.parse(fecha_vencimiento.get());
        LocalDate hoy = LocalDate.now();

        // Calcular días desde el vencimiento
        long diasDesdeVencimiento = ChronoUnit.DAYS.between(vencimiento, hoy);

        if (diasDesdeVencimiento < 0) {
            // El cliente aún no ha vencido
            return "Activo";
        } else if (diasDesdeVencimiento <= 15) {
            // Período de gracia de 14 días
            return "Activo";
        } else {
            // Más de 14 días desde el vencimiento
            return "Inactivo";
        }
    }

    public String getNombreCompleto() {
        return getNombres() + " " + getApellidos();
    }

    public void setDiasRestantes() {
        LocalDate fechaVenc = this.getFecha_vencimientoDate();
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), fechaVenc);

        if (dias == 0) {
            this.diasRestantes.set(0);
        } else if (dias > 0) {
            this.diasRestantes.set((int) dias);
        } else {
            this.diasRestantes.set(-1);
        }
    }

    public void setDiasRestantes(int dias) {
        this.diasRestantes.set(dias);
    }

    public void setPresenteHoy(boolean presente) {
        this.presenteHoy.set(presente);
    }

    public void setTipoMembresia(String tipo) {
        this.tipoMembresia.set(tipo);
    }

    public LocalDate getFecha_vencimientoDate() {
        return LocalDate.parse(fecha_vencimiento.get());
    }

    public String getTooltipText() {
        LocalDate vencimiento = getFecha_vencimientoDate();
        LocalDate hoy = LocalDate.now();
        long dias = ChronoUnit.DAYS.between(hoy, vencimiento);

        String infoDias;
        if (dias >= 0) {
            // Antes del vencimiento
            infoDias = "Días restantes: " + dias;
        } else {
            // Después del vencimiento (período de gracia)
            long diasGraciaRestantes = 15 + dias; // dias es negativo, así que sumamos
            if (diasGraciaRestantes > 0) {
                infoDias = "Días de gracia restantes: " + diasGraciaRestantes;
            } else {
                infoDias = "Período de gracia terminado";
            }
        }

        return "Nombre: " + getNombres() + " " + getApellidos() + "\n" +
                "Teléfono: " + getTelefono() + "\n" +
                "Membresía: " + getTipoMembresia() + "\n" +
                "Vence: " + getFecha_vencimiento() + "\n" +
                infoDias;
    }
}
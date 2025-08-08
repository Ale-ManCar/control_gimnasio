package models;

import javafx.beans.property.IntegerProperty;
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

    public String getNombres() { return nombres.get(); }
    public String getApellidos() { return apellidos.get(); }
    public String getTelefono() { return telefono.get(); }
    public String getTipoMembresia() { return tipoMembresia.get(); }
    public String getFecha_vencimiento() { return fecha_vencimiento.get(); }
    public int getDiasRestantes() { return diasRestantes.get(); }

    public String getEstado() {
        if (fecha_vencimiento.get() == null || fecha_vencimiento.get().isEmpty())
            return "Inactivo";

        LocalDate vencimiento = LocalDate.parse(fecha_vencimiento.get());
        long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), vencimiento);

        return diasRestantes >= 0 ? "Activo" : "Inactivo";
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

    public void setTipoMembresia(String tipo) {
        this.tipoMembresia.set(tipo);
    }

    public LocalDate getFecha_vencimientoDate() {
        return LocalDate.parse(fecha_vencimiento.get());
    }

    public String getTooltipText() {
        return "Nombre: " + getNombres() + " " + getApellidos() + "\n" +
                "Teléfono: " + getTelefono() + "\n" +
                "Membresía: " + getTipoMembresia() + "\n" +
                "Vence: " + getFecha_vencimiento() + "\n" +
                "Días restantes: " + getDiasRestantes();
    }
}
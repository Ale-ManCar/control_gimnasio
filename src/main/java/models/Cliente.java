package models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

public class Cliente {
    private final StringProperty nombres;
    private final StringProperty telefono;
    private final StringProperty fechaVencimiento;
    private final StringProperty tipoMembresia;
    private final IntegerProperty diasRestantes;

    // Constructor con tipo de membresía
    public Cliente(String nombres, String telefono, String tipoMembresia, LocalDate fechaVencimiento) {
        this.nombres = new SimpleStringProperty(nombres);
        this.telefono = new SimpleStringProperty(telefono);
        this.tipoMembresia = new SimpleStringProperty(tipoMembresia);
        this.fechaVencimiento = new SimpleStringProperty(fechaVencimiento.toString());
        this.diasRestantes = new SimpleIntegerProperty(LocalDate.now().until(fechaVencimiento).getDays());
    }

    // Constructor alternativo sin tipoMembresia
    public Cliente(String nombres, String telefono, LocalDate fechaVencimiento) {
        this(nombres, telefono, "No definida", fechaVencimiento);
    }

    // Getters para JavaFX
    public StringProperty nombresProperty() { return nombres; }
    public StringProperty telefonoProperty() { return telefono; }
    public StringProperty fechaVencimientoProperty() { return fechaVencimiento; }
    public StringProperty tipoMembresiaProperty() { return tipoMembresia; }
    public IntegerProperty diasRestantesProperty() { return diasRestantes; }

    // Getters normales
    public String getTelefono() { return telefono.get(); }
    public String getNombres() { return nombres.get(); }
    public String getTipoMembresia() { return tipoMembresia.get(); }
    public String getFechaVencimiento() { return fechaVencimiento.get(); }
    public int getDiasRestantes() { return diasRestantes.get(); }
}

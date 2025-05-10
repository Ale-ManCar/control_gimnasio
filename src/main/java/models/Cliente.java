package models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

public class Cliente {
    private final StringProperty nombres;
    private final StringProperty telefono;
    private final StringProperty fechaVencimiento;

    // Constructor
    public Cliente(String nombres, String telefono, LocalDate fechaVencimiento) {
        this.nombres = new SimpleStringProperty(nombres);
        this.telefono = new SimpleStringProperty(telefono);
        this.fechaVencimiento = new SimpleStringProperty(fechaVencimiento.toString());
    }

    // Getters para propiedades JavaFX
    public StringProperty nombresProperty() { return nombres; }
    public StringProperty telefonoProperty() { return telefono; }
    public StringProperty fechaVencimientoProperty() { return fechaVencimiento; }
}
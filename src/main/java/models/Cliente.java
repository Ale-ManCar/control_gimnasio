package models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

public class Cliente {
    private final StringProperty nombres;
    private final StringProperty telefono;
    private final StringProperty fecha_vencimiento;
    private final StringProperty tipoMembresia;
    private final IntegerProperty diasRestantes;

    // Constructor con tipo de membresía
    public Cliente(String nombres, String telefono, String tipoMembresia, LocalDate fecha_vencimiento) {
        this.nombres = new SimpleStringProperty(nombres);
        this.telefono = new SimpleStringProperty(telefono);
        this.tipoMembresia = new SimpleStringProperty(tipoMembresia);
        this.fecha_vencimiento = new SimpleStringProperty(fecha_vencimiento.toString());
        this.diasRestantes = new SimpleIntegerProperty(LocalDate.now().until(fecha_vencimiento).getDays());
    }

    // Constructor alternativo sin tipoMembresia
    public Cliente(String nombres, String telefono, LocalDate fecha_vencimiento) {
        this(nombres, telefono, "No definida", fecha_vencimiento);
    }

    // Getters para JavaFX
    public StringProperty nombresProperty() { return nombres; }
    public StringProperty telefonoProperty() { return telefono; }
    public StringProperty fechaVencimientoProperty() { return fecha_vencimiento; }
    public StringProperty tipoMembresiaProperty() { return tipoMembresia; }
    public IntegerProperty diasRestantesProperty() { return diasRestantes; }

    // Getters normales
    public String getTelefono() { return telefono.get(); }
    public String getNombres() { return nombres.get(); }
    public String getTipoMembresia() { return tipoMembresia.get(); }
    public String getFecha_vencimiento() { return fecha_vencimiento.get(); }
    public int getDiasRestantes() { return diasRestantes.get(); }

    // ✅ Nuevo método para trabajar con fechas como LocalDate
    public LocalDate getFecha_vencimientoDate() {
        return LocalDate.parse(fecha_vencimiento.get());
    }
}
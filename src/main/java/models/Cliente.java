package models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

public class Cliente {
    private final StringProperty nombres;
    private final StringProperty apellidos;
    private final StringProperty telefono;
    private final StringProperty fecha_vencimiento;
    private final StringProperty tipoMembresia;
    private final IntegerProperty diasRestantes = new SimpleIntegerProperty(0);

    // Constructor completo con apellidos
    public Cliente(String nombres, String apellidos, String telefono, String tipoMembresia, LocalDate fecha_vencimiento) {
        this.nombres = new SimpleStringProperty(nombres);
        this.apellidos = new SimpleStringProperty(apellidos);
        this.telefono = new SimpleStringProperty(telefono);
        this.tipoMembresia = new SimpleStringProperty(tipoMembresia);
        this.fecha_vencimiento = new SimpleStringProperty(fecha_vencimiento.toString());
        this.diasRestantes.set(LocalDate.now().until(fecha_vencimiento).getDays());
    }

    // Constructor alternativo con apellidos pero sin tipoMembresia
    public Cliente(String nombres, String apellidos, String telefono, LocalDate fecha_vencimiento) {
        this(nombres, apellidos, telefono, "No definido", fecha_vencimiento);
    }

    // Constructor básico (mantenido para compatibilidad)
    public Cliente(String nombres, String telefono, LocalDate fecha_vencimiento) {
        this(nombres, "", telefono, "No definido", fecha_vencimiento);
    }

    // Getters para propiedades JavaFX
    public StringProperty nombresProperty() { return nombres; }
    public StringProperty apellidosProperty() { return apellidos; }
    public StringProperty telefonoProperty() { return telefono; }
    public StringProperty fechaVencimientoProperty() { return fecha_vencimiento; }
    public StringProperty tipoMembresiaProperty() { return tipoMembresia; }
    public IntegerProperty diasRestantesProperty() { return diasRestantes; }

    // Getters normales
    public String getNombres() { return nombres.get(); }
    public String getApellidos() { return apellidos.get(); }
    public String getTelefono() { return telefono.get(); }
    public String getTipoMembresia() { return tipoMembresia.get(); }
    public String getFecha_vencimiento() { return fecha_vencimiento.get(); }
    public int getDiasRestantes() { return diasRestantes.get(); }

    // Método para obtener nombre completo
    public String getNombreCompleto() {
        return getNombres() + " " + getApellidos();
    }

    // Método para establecer días restantes
    public void setDiasRestantes(int dias) {
        this.diasRestantes.set(dias);
    }

    // Método para obtener fecha como LocalDate
    public LocalDate getFecha_vencimientoDate() {
        return LocalDate.parse(fecha_vencimiento.get());
    }
}
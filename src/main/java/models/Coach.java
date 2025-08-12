package models;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

public class Coach {
    private final IntegerProperty id;
    private final StringProperty nombres;
    private final StringProperty apellidos;
    private final StringProperty area;
    private final StringProperty telefono;
    private final StringProperty fotoPath;

    public Coach(int id, String nombres, String apellidos, String area, String telefono, String fotoPath) {
        this.id = new SimpleIntegerProperty(id);
        this.nombres = new SimpleStringProperty(nombres);
        this.apellidos = new SimpleStringProperty(apellidos);
        this.area = new SimpleStringProperty(area);
        this.telefono = new SimpleStringProperty(telefono);
        this.fotoPath = new SimpleStringProperty(fotoPath);
    }

    public Coach(String nombres, String apellidos, String area, String telefono, String fotoPath) {
        this(-1, nombres, apellidos, area, telefono, fotoPath);
    }

    public int getId() { return id.get(); }
    public String getNombres() { return nombres.get(); }
    public String getApellidos() { return apellidos.get(); }
    public String getArea() { return area.get(); }
    public String getTelefono() { return telefono.get(); }
    public String getFotoPath() { return fotoPath.get(); }

    public IntegerProperty idProperty() { return id; }
    public StringProperty nombresProperty() { return nombres; }
    public StringProperty apellidosProperty() { return apellidos; }
    public StringProperty areaProperty() { return area; }
    public StringProperty telefonoProperty() { return telefono; }
    public StringProperty fotoPathProperty() { return fotoPath; }

    public String getNombreCompleto() {
        return getNombres() + " " + getApellidos();
    }
}
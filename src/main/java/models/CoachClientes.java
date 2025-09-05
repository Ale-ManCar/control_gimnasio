package models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class CoachClientes {
    private final StringProperty coach;
    private final IntegerProperty clientes;

    public CoachClientes(String coach, int clientes) {
        this.coach = new SimpleStringProperty(coach);
        this.clientes = new SimpleIntegerProperty(clientes);
    }

    public String getCoach() { return coach.get(); }
    public int getClientes() { return clientes.get(); }

    public StringProperty coachProperty() { return coach; }
    public IntegerProperty clientesProperty() { return clientes; }
}
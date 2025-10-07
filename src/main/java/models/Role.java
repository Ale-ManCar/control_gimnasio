package models;

public enum Role {
    ADMIN,
    RECEPCIONISTA,
    COACH;

    public static Role fromString(String value) {
        for (Role role : values()) {
            if (role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Rol no reconocido: " + value);
    }
}
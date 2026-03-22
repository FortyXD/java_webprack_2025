package ru.javaprac.bank.entity;

public enum ClientType {
    LEGAL_ENTITY("legal entity"),
    NATURAL_PERSON("natural person");

    private final String dbValue;

    ClientType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static ClientType fromDb(String v) {
        for (ClientType t : values()) {
            if (t.dbValue.equals(v)) return t;
        }
        throw new IllegalArgumentException("Unknown client_type: " + v);
    }
}

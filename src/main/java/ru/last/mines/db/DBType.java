package ru.last.mines.db;

import java.util.Locale;

public enum DBType {
    MEMORY, H2, SQLITE, MYSQL, MARIADB, POSTGRESQL;

    public static DBType parse(String raw) {
        if (raw == null) return H2;
        String s = raw.trim().toUpperCase(Locale.ROOT).replace("-", "").replace("_", "");
        for (DBType t : values()) {
            if (t.name().equals(s)) return t;
        }
        return null;
    }
}

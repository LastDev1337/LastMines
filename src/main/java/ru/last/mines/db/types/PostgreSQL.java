package ru.last.mines.db.types;

public final class PostgreSQL extends SqlProvider {
    public PostgreSQL(String host, int port, String name, String user, String pass, String prefix) {
        super("org.postgresql.Driver", "jdbc:postgresql://" + host + ":" + port + "/" + name, user, pass, 6, prefix);
    }

    @Override
    protected String upsertSql(String table, String[] keys, String ow, String add) {
        return onConflict(table, keys, ow, add);
    }
}

package ru.last.mines.db.types;

public final class SQLite extends SqlProvider {
    public SQLite(String file, String prefix) {
        super("org.sqlite.JDBC", "jdbc:sqlite:" + file, null, null, 1, prefix);
    }

    @Override
    protected String upsertSql(String table, String[] keys, String ow, String add) {
        return onConflict(table, keys, ow, add);
    }
}

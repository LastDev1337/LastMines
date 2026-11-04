package ru.last.mines.db.types;

/**
 * MySQL. Используется драйвер MariaDB Connector/J (он работает и с MySQL по протоколу),
 * поэтому в libraries только один драйвер для двух баз.
 */
public final class MySQL extends SqlProvider {
    public MySQL(String host, int port, String name, String user, String pass, String prefix) {
        super("org.mariadb.jdbc.Driver", "jdbc:mariadb://" + host + ":" + port + "/" + name, user, pass, 6, prefix);
    }

    @Override
    protected String upsertSql(String table, String[] keys, String ow, String add) {
        return onDuplicateKey(table, keys, ow, add);
    }
}

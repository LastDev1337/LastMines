package ru.last.mines.db.types;

/** H2 (файл, один сервер). Своего "вставить или прибавить" в одной команде не использует: запасной путь UPDATE + INSERT. */
public final class H2 extends SqlProvider {
    public H2(String file, String prefix) {
        super("org.h2.Driver", "jdbc:h2:file:" + file, null, null, 4, prefix);
    }

    @Override
    protected String upsertSql(String table, String[] keys, String ow, String add) {
        return null;
    }
}

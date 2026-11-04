package ru.last.mines.db;

import dev.by1337.yaml.YamlMap;
import ru.last.mines.LastMines;
import ru.last.mines.db.types.H2;
import ru.last.mines.db.types.MariaDB;
import ru.last.mines.db.types.Memory;
import ru.last.mines.db.types.MySQL;
import ru.last.mines.db.types.PostgreSQL;
import ru.last.mines.db.types.SQLite;
import ru.last.mines.utils.time.TimeUtils;

import java.io.File;

public final class DBManager {

    public record Settings(boolean enable, int flushSeconds, int topRefreshSeconds, int topSize) { }

    private final LastMines plugin;
    private Settings settings = new Settings(true, 5, 45, 100);

    public DBManager(LastMines plugin) {
        this.plugin = plugin;
    }

    public Settings settings() {
        return settings;
    }

    public DBProvider connect() {
        try {
            plugin.getConfigManager().extractLangResource("sql.yml");
            File file = new File(plugin.getDataFolder(), "sql.yml");
            YamlMap root = YamlMap.load(file);

            YamlMap st = root.get("stats").asYamlMap().orDefault(new YamlMap());
            settings = new Settings(
                    st.get("enable").asBool(true),
                    Math.max(1, TimeUtils.parseToSeconds(st.get("flush_interval").asString("5s"))),
                    Math.max(5, TimeUtils.parseToSeconds(st.get("top_refresh").asString("45s"))),
                    Math.max(10, Math.min(1000, st.get("top_size").asInt(100))));
            if (!settings.enable()) {
                plugin.getLogger().info("Статистика добычи выключена в sql.yml (stats.enable: false).");
                return null;
            }

            DBType type = DBType.parse(root.get("driver").asString("H2"));
            if (type == null) {
                plugin.getLogger().severe("sql.yml: неизвестный driver '" + root.get("driver").asString("") + "' (Memory, H2, SQLite, MySQL, MariaDB, PostgreSQL). Статистика выключена.");
                return null;
            }
            String prefix = root.get("table_prefix").asString("lastmines");
            String data = plugin.getDataFolder().getAbsolutePath() + File.separator;

            DBProvider provider = switch (type) {
                case MEMORY -> new Memory();
                case H2 -> new H2(data + root.get("h2.name").asString("LastMines"), prefix);
                case SQLITE -> new SQLite(data + root.get("sqlite.name").asString("LastMines") + ".db", prefix);
                case MYSQL -> network(root, "mysql", 3306, prefix, false);
                case MARIADB -> network(root, "mariadb", 3306, prefix, true);
                case POSTGRESQL -> network(root, "postgresql", 5432, prefix, null);
            };
            provider.init();
            plugin.getLogger().info("Статистика добычи: хранилище " + type + ".");
            return provider;
        } catch (Throwable t) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Не удалось подключить хранилище статистики (см. sql.yml). Статистика выключена.", t);
            return null;
        }
    }

    private DBProvider network(YamlMap root, String section, int defaultPort, String prefix, Boolean mariadb) {
        YamlMap m = root.get(section).asYamlMap().orDefault(new YamlMap());
        String host = m.get("host").asString("localhost");
        int port = defaultPort;
        int colon = host.lastIndexOf(':');
        if (colon > 0) {
            try {
                port = Integer.parseInt(host.substring(colon + 1).trim());
                host = host.substring(0, colon);
            } catch (NumberFormatException ignored) { }
        }
        String name = m.get("name").asString("lastmines");
        String user = m.get("user").asString("");
        String pass = m.get("pass").asString("");
        if (mariadb == null) return new PostgreSQL(host, port, name, user, pass, prefix);
        return mariadb ? new MariaDB(host, port, name, user, pass, prefix) : new MySQL(host, port, name, user, pass, prefix);
    }
}

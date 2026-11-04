package ru.last.mines.db.types;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Material;
import ru.last.mines.db.DBProvider;
import ru.last.mines.stats.Delta;
import ru.last.mines.stats.PlayerStats;
import ru.last.mines.stats.TopEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class SqlProvider implements DBProvider {

    private static final int SCHEMA_VERSION = 1;

    protected final HikariDataSource pool;
    private final String players;
    private final String blocks;
    private final String meta;

    protected SqlProvider(String driverClass, String url, String user, String pass, int poolSize, String prefix) {
        HikariConfig cfg = new HikariConfig();
        cfg.setDriverClassName(driverClass);
        cfg.setJdbcUrl(url);
        if (user != null) cfg.setUsername(user);
        if (pass != null) cfg.setPassword(pass);
        cfg.setMaximumPoolSize(poolSize);
        cfg.setPoolName("LastMines-" + getClass().getSimpleName());
        cfg.setConnectionTimeout(10_000);
        this.pool = new HikariDataSource(cfg);
        String p = prefix.replaceAll("[^A-Za-z0-9_]", "");
        this.players = p + "_players";
        this.blocks = p + "_blocks";
        this.meta = p + "_meta";
    }

    protected abstract String upsertSql(String table, String[] keys, String ow, String add);

    @Override
    public void init() throws SQLException {
        try (Connection c = pool.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS " + players
                    + " (uuid VARCHAR(36) NOT NULL PRIMARY KEY, name VARCHAR(32) NOT NULL, total BIGINT NOT NULL DEFAULT 0)");
            st.execute("CREATE TABLE IF NOT EXISTS " + blocks
                    + " (uuid VARCHAR(36) NOT NULL, material VARCHAR(64) NOT NULL, amount BIGINT NOT NULL DEFAULT 0,"
                    + " PRIMARY KEY (uuid, material))");
            st.execute("CREATE TABLE IF NOT EXISTS " + meta + " (k VARCHAR(32) NOT NULL PRIMARY KEY, v VARCHAR(64) NOT NULL)");

            int version = readVersion(c);
            if (version < 1) {
                st.execute("CREATE INDEX " + players + "_total ON " + players + " (total)");
                st.execute("CREATE INDEX " + blocks + "_material ON " + blocks + " (material, amount)");
                writeVersion(c, 1);
            }
        }
    }

    private int readVersion(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT v FROM " + meta + " WHERE k = 'schema'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Integer.parseInt(rs.getString(1)) : 0;
        }
    }

    private void writeVersion(Connection c, int version) throws SQLException {
        upsertOverwrite(c, meta, "k", "schema", "v", String.valueOf(version));
    }

    @Override
    public void flush(Map<UUID, Delta> batch) throws SQLException {
        try (Connection c = pool.getConnection()) {
            boolean auto = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                for (Map.Entry<UUID, Delta> e : batch.entrySet()) {
                    String uuid = e.getKey().toString();
                    Delta d = e.getValue();
                    upsertAdd(c, players, new String[]{"uuid"}, new Object[]{uuid}, "name", d.name, "total", d.total);
                    for (Map.Entry<Material, Long> b : d.blocks.entrySet()) {
                        upsertAdd(c, blocks, new String[]{"uuid", "material"}, new Object[]{uuid, b.getKey().name()}, null, null, "amount", b.getValue());
                    }
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(auto);
            }
        }
    }

    private void upsertAdd(Connection c, String table, String[] keys, Object[] keyVals, String ow, Object owVal, String add, long delta) throws SQLException {
        String sql = upsertSql(table, keys, ow, add);
        if (sql != null) {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                int i = 1;
                for (Object k : keyVals) ps.setObject(i++, k);
                if (ow != null) ps.setObject(i++, owVal);
                ps.setLong(i, delta);
                ps.executeUpdate();
            }
            return;
        }
        StringBuilder where = new StringBuilder();
        for (int i = 0; i < keys.length; i++) where.append(i == 0 ? "" : " AND ").append(keys[i]).append(" = ?");
        String set = (ow != null ? ow + " = ?, " : "") + add + " = " + add + " + ?";
        int updated;
        try (PreparedStatement ps = c.prepareStatement("UPDATE " + table + " SET " + set + " WHERE " + where)) {
            int i = 1;
            if (ow != null) ps.setObject(i++, owVal);
            ps.setLong(i++, delta);
            for (Object k : keyVals) ps.setObject(i++, k);
            updated = ps.executeUpdate();
        }
        if (updated == 0) {
            String cols = String.join(", ", keys) + (ow != null ? ", " + ow : "") + ", " + add;
            String marks = "?" + ", ?".repeat(keys.length - 1 + (ow != null ? 1 : 0) + 1);
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO " + table + " (" + cols + ") VALUES (" + marks + ")")) {
                int i = 1;
                for (Object k : keyVals) ps.setObject(i++, k);
                if (ow != null) ps.setObject(i++, owVal);
                ps.setLong(i, delta);
                ps.executeUpdate();
            }
        }
    }

    private void upsertOverwrite(Connection c, String table, String keyCol, String key, String valCol, String val) throws SQLException {
        int updated;
        try (PreparedStatement ps = c.prepareStatement("UPDATE " + table + " SET " + valCol + " = ? WHERE " + keyCol + " = ?")) {
            ps.setString(1, val);
            ps.setString(2, key);
            updated = ps.executeUpdate();
        }
        if (updated == 0) {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO " + table + " (" + keyCol + ", " + valCol + ") VALUES (?, ?)")) {
                ps.setString(1, key);
                ps.setString(2, val);
                ps.executeUpdate();
            }
        }
    }

    @Override
    public PlayerStats load(UUID uuid, String fallbackName) throws SQLException {
        PlayerStats stats = new PlayerStats(uuid, fallbackName);
        String name = fallbackName;
        long total = 0;
        Map<Material, Long> map = new EnumMap<>(Material.class);
        try (Connection c = pool.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT name, total FROM " + players + " WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        name = rs.getString(1);
                        total = rs.getLong(2);
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT material, amount FROM " + blocks + " WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Material m = Material.matchMaterial(rs.getString(1));
                        if (m != null) map.put(m, rs.getLong(2));
                    }
                }
            }
        }
        stats.set(name, total, map);
        return stats;
    }

    @Override
    public List<TopEntry> top(Material material, int limit) throws SQLException {
        String sql = material == null
                ? "SELECT uuid, name, total FROM " + players + " WHERE total > 0 ORDER BY total DESC LIMIT ?"
                : "SELECT b.uuid, p.name, b.amount FROM " + blocks + " b JOIN " + players + " p ON p.uuid = b.uuid"
                + " WHERE b.material = ? AND b.amount > 0 ORDER BY b.amount DESC LIMIT ?";
        List<TopEntry> out = new ArrayList<>();
        try (Connection c = pool.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            if (material != null) ps.setString(i++, material.name());
            ps.setInt(i, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new TopEntry(UUID.fromString(rs.getString(1)), rs.getString(2), rs.getLong(3)));
                }
            }
        }
        return out;
    }

    @Override
    public UUID findUuid(String name) throws SQLException {
        try (Connection c = pool.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT uuid FROM " + players + " WHERE LOWER(name) = LOWER(?) ORDER BY total DESC LIMIT 1")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? UUID.fromString(rs.getString(1)) : null;
            }
        }
    }

    @Override
    public void close() {
        pool.close();
    }

    protected static String onConflict(String table, String[] keys, String ow, String add) {
        String cols = String.join(", ", keys) + (ow != null ? ", " + ow : "") + ", " + add;
        String marks = "?" + ", ?".repeat(keys.length - 1 + (ow != null ? 1 : 0) + 1);
        String update = (ow != null ? ow + " = excluded." + ow + ", " : "") + add + " = " + table + "." + add + " + excluded." + add;
        return "INSERT INTO " + table + " (" + cols + ") VALUES (" + marks + ") ON CONFLICT (" + String.join(", ", keys) + ") DO UPDATE SET " + update;
    }

    protected static String onDuplicateKey(String table, String[] keys, String ow, String add) {
        String cols = String.join(", ", keys) + (ow != null ? ", " + ow : "") + ", " + add;
        String marks = "?" + ", ?".repeat(keys.length - 1 + (ow != null ? 1 : 0) + 1);
        String update = (ow != null ? ow + " = VALUES(" + ow + "), " : "") + add + " = " + add + " + VALUES(" + add + ")";
        return "INSERT INTO " + table + " (" + cols + ") VALUES (" + marks + ") ON DUPLICATE KEY UPDATE " + update;
    }
}

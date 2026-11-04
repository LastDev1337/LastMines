package ru.last.mines.db.types;

import org.bukkit.Material;
import ru.last.mines.db.DBProvider;
import ru.last.mines.stats.Delta;
import ru.last.mines.stats.PlayerStats;
import ru.last.mines.stats.TopEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Хранилище в памяти: для тестов и мелких серверов, после выключения данные пропадают. */
public final class Memory implements DBProvider {

    private final Map<UUID, PlayerStats> data = new ConcurrentHashMap<>();

    @Override
    public void init() { }

    @Override
    public void flush(Map<UUID, Delta> batch) {
        batch.forEach((uuid, delta) -> {
            PlayerStats stats = data.computeIfAbsent(uuid, u -> new PlayerStats(u, delta.name));
            stats.addDelta(delta);
            stats.set(delta.name, stats.total(), stats.blocksCopy());
        });
    }

    @Override
    public PlayerStats load(UUID uuid, String fallbackName) {
        PlayerStats stored = data.get(uuid);
        PlayerStats copy = new PlayerStats(uuid, stored != null ? stored.name() : fallbackName);
        if (stored != null) copy.set(stored.name(), stored.total(), stored.blocksCopy());
        return copy;
    }

    @Override
    public List<TopEntry> top(Material material, int limit) {
        List<TopEntry> out = new ArrayList<>();
        for (PlayerStats s : data.values()) {
            long value = material == null ? s.total() : s.amount(material);
            if (value > 0) out.add(new TopEntry(s.uuid, s.name(), value));
        }
        out.sort(Comparator.comparingLong(TopEntry::value).reversed());
        return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
    }

    @Override
    public UUID findUuid(String name) {
        for (PlayerStats s : data.values()) {
            if (s.name().toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT))) return s.uuid;
        }
        return null;
    }

    @Override
    public void close() { }
}

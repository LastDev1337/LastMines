package ru.last.mines.db;

import org.bukkit.Material;
import ru.last.mines.stats.Delta;
import ru.last.mines.stats.PlayerStats;
import ru.last.mines.stats.TopEntry;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DBProvider {

    void init() throws Exception;

    void flush(Map<UUID, Delta> batch) throws Exception;

    PlayerStats load(UUID uuid, String fallbackName) throws Exception;

    List<TopEntry> top(Material material, int limit) throws Exception;

    UUID findUuid(String name) throws Exception;

    void close();
}

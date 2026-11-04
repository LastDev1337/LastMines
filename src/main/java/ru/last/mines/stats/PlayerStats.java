package ru.last.mines.stats;

import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerStats {
    public final UUID uuid;
    private String name;
    private long total;
    private final Map<Material, Long> blocks = new EnumMap<>(Material.class);

    public PlayerStats(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public synchronized String name() { return name; }
    public synchronized long total() { return total; }
    public synchronized long amount(Material material) { return blocks.getOrDefault(material, 0L); }
    public synchronized Map<Material, Long> blocksCopy() { return new EnumMap<>(blocks.isEmpty() ? new EnumMap<>(Material.class) : blocks); }

    public synchronized void set(String name, long total, Map<Material, Long> blocks) {
        this.name = name;
        this.total = total;
        this.blocks.clear();
        this.blocks.putAll(blocks);
    }

    public synchronized void add(Material material) {
        total++;
        blocks.merge(material, 1L, Long::sum);
    }

    public synchronized void addDelta(Delta delta) {
        total += delta.total;
        delta.blocks.forEach((m, v) -> blocks.merge(m, v, Long::sum));
    }
}

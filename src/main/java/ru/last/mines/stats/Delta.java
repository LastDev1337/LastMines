package ru.last.mines.stats;

import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

public final class Delta {
    public String name;
    public long total;
    public final Map<Material, Long> blocks = new EnumMap<>(Material.class);

    public Delta(String name) {
        this.name = name;
    }

    public void add(Material material) {
        total++;
        blocks.merge(material, 1L, Long::sum);
    }

    public Delta merge(Delta other) {
        total += other.total;
        other.blocks.forEach((m, v) -> blocks.merge(m, v, Long::sum));
        return this;
    }
}

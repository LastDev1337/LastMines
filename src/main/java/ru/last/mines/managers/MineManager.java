package ru.last.mines.managers;

import org.bukkit.Bukkit;
import dev.by1337.yaml.YamlMap;
import ru.last.mines.api.events.*;
import ru.last.mines.models.*;
import ru.last.mines.LastMines;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MineManager {
    private final LastMines plugin;
    private final Map<String, Mine> mines = new HashMap<>();

    public MineManager(LastMines plugin) { this.plugin = plugin; }

    public void loadAll() {
        mines.clear();
        File folder = new File(plugin.getDataFolder(), "mines");
        if (!folder.exists() || folder.listFiles() == null || Objects.requireNonNull(folder.listFiles()).length == 0) {
            if (!folder.exists()) folder.mkdirs();
            try {
                plugin.saveResource("mines/blocks.yml", true);
                plugin.saveResource("mines/rarity.yml", true);
            } catch (Exception e) { plugin.getDebugLogger().error("Не удалось создать стандартные шахты", e); }
        }

        File[] files = folder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                YamlMap map = YamlMap.load(file);
                Mine mine = new Mine(plugin, file.getName().replace(".yml", ""), map);
                mines.put(mine.getId(), mine);
                Bukkit.getPluginManager().callEvent(new MineLoadEvent(mine));
                plugin.getDebugLogger().info("Загружена шахта: " + mine.getId());
            } catch (Exception e) { plugin.getDebugLogger().error("Не удалось загрузить шахту " + file.getName(), e); }
        }
    }

    public void unloadAll() {
        for (Mine mine : mines.values()) {
            Bukkit.getPluginManager().callEvent(new MineUnloadEvent(mine));
            mine.stopTasks();
            mine.deleteHologram();
        }
        mines.clear();
    }

    public Mine getMine(String id) { return mines.get(id); }
    public Map<String, Mine> getMines() { return mines; }
}

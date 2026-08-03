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
                plugin.getConfigManager().extractLangResource("mines/blocks.yml");
                plugin.getConfigManager().extractLangResource("mines/rarity.yml");
            } catch (Exception e) { plugin.getDebugger().error("Не удалось создать стандартные шахты", e); }
        }

        File[] files = folder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            loadFile(file, 0);
        }
    }

    private void loadFile(File file, int attempt) {
        try {
            YamlMap map = YamlMap.load(file);
            String worldName = map.get("world").asString("world");

            if (Bukkit.getWorld(worldName) == null) {
                if (attempt >= 10) {
                    plugin.getDebugger().error("Мир '" + worldName + "' для шахты " + file.getName() + " так и не загрузился за 10 секунд, шахта пропущена.");
                    return;
                }
                plugin.getDebugger().warn("Мир '" + worldName + "' для шахты " + file.getName() + " ещё не загружен, повтор через 1 сек. (" + (attempt + 1) + "/10)...");
                Bukkit.getScheduler().runTaskLater(plugin, () -> loadFile(file, attempt + 1), 20L);
                return;
            }

            Mine mine = new Mine(plugin, file.getName().replace(".yml", ""), map);
            mines.put(mine.getId(), mine);
            Bukkit.getPluginManager().callEvent(new MineLoadEvent(mine));
            plugin.getDebugger().info("Загружена шахта: " + mine.getId());
        } catch (Exception e) { plugin.getDebugger().error("Не удалось загрузить шахту " + file.getName(), e); }
    }

    public void reloadOne(String id) {
        Mine old = mines.remove(id);
        if (old != null) {
            Bukkit.getPluginManager().callEvent(new MineUnloadEvent(old));
            old.stopTasks();
            old.deleteHologram();
        }
        File file = new File(plugin.getDataFolder(), "mines" + File.separator + id + ".yml");
        if (file.exists()) loadFile(file, 0);
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

package ru.last.mines.holograms;

import org.bukkit.Bukkit;
import ru.last.mines.holograms.providers.*;
import ru.last.mines.models.*;
import ru.last.mines.LastMines;

import java.util.HashMap;
import java.util.Map;

public class HologramManager {
    private final LastMines plugin;
    private final Map<String, HologramProvider> providers = new HashMap<>();

    public HologramManager(LastMines plugin) {
        this.plugin = plugin;

        if (Bukkit.getPluginManager().getPlugin("DecentHolograms") != null) {
            providers.put("decentholograms", new DecentHoloProvider());
        }

        try {
            Class.forName("org.bukkit.entity.TextDisplay");
            providers.put("vanilla", new VanillaHoloProvider());

            if (Bukkit.getPluginManager().getPlugin("FancyHolograms") != null) {
                providers.put("fancyholograms", new FancyHoloProvider());
            }
        } catch (ClassNotFoundException ignored) {
            // nope
        }
    }

    private HologramProvider getProvider(String name) {
        return providers.get(name.toLowerCase());
    }

    public void create(Mine mine) {
        if (mine.isHoloEnable()) return;
        String providerName = mine.getHoloProvider();
        HologramProvider provider = getProvider(providerName);

        if (provider == null && (providerName.equalsIgnoreCase("vanilla") || providerName.equalsIgnoreCase("fancyholograms"))) {
            plugin.getDebugLogger().warn("Шахта " + mine.getId() + " использует голограммы " + providerName + ", но они не поддерживаются на этой версии игры (требуется 1.19.4+)! Автоматически переключаемся на DecentHolograms.");
            providerName = "decentholograms";
            provider = getProvider(providerName);
        }

        if (provider != null) {
            provider.create(mine);
        } else {
            plugin.getDebugLogger().error("Шахта " + mine.getId() + " не смогла создать голограмму: провайдер " + providerName + " не найден!");
        }
    }

    public void update(Mine mine) {
        if (mine.isHoloEnable()) return;
        String providerName = mine.getHoloProvider();
        HologramProvider provider = getProvider(providerName);
        if (provider == null && (providerName.equalsIgnoreCase("vanilla") || providerName.equalsIgnoreCase("fancyholograms"))) {
            providerName = "decentholograms";
            provider = getProvider(providerName);
        }
        if (provider != null) {
            provider.update(mine);
        }
    }

    public void delete(Mine mine) {
        String providerName = mine.getHoloProvider();
        HologramProvider provider = getProvider(providerName);
        if (provider == null && (providerName.equalsIgnoreCase("vanilla") || providerName.equalsIgnoreCase("fancyholograms"))) {
            providerName = "decentholograms";
            provider = getProvider(providerName);
        }
        if (provider != null) {
            provider.delete(mine);
        }
    }

    public void unloadAll() {
        HologramProvider vanilla = providers.get("vanilla");
        if (vanilla != null) {
            vanilla.removeAll();
        }
        for (Mine mine : plugin.getMineManager().getMines().values()) {
            delete(mine);
        }
    }
}

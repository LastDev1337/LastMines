package ru.last.mines.holograms;

import dev.laststudio.lib.api.LLibAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import ru.last.mines.holograms.providers.*;
import ru.last.mines.mine.Mine;
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
        }
    }

    /**
     * Голограмма - это сущность в мире: на Folia с ней можно работать только в потоке региона,
     * где она стоит. Поэтому любая операция уходит в планировщик региона точки голограммы.
     */
    private void onRegion(Mine mine, Runnable action) {
        Location loc = mine.getHoloOffset();
        if (loc == null || loc.getWorld() == null || !plugin.isEnabled()) {
            action.run();
            return;
        }
        LLibAPI.get().scheduler().region(plugin, loc, action);
    }

    private HologramProvider getProvider(String name) { return providers.get(name.toLowerCase()); }

    public void create(Mine mine) {
        if (!mine.isHoloEnable()) return;
        String providerName = mine.getHoloProvider();
        HologramProvider provider = getProvider(providerName);

        if (provider == null && (providerName.equalsIgnoreCase("vanilla") || providerName.equalsIgnoreCase("fancyholograms"))) {
            plugin.getDebugger().warn("Шахта " + mine.getId() + " использует голограммы " + providerName + ", но они не поддерживаются на этой версии игры (требуется 1.21.5+)! Автоматически переключаемся на DecentHolograms.");
            providerName = "decentholograms";
            provider = getProvider(providerName);
        }

        if (provider != null) {
            HologramProvider chosen = provider;
            onRegion(mine, () -> chosen.create(mine));
        } else {
            plugin.getDebugger().error("Шахта " + mine.getId() + " не смогла создать голограмму: провайдер " + providerName + " не найден!");
        }
    }

    public void update(Mine mine) {
        if (!mine.isHoloEnable()) return;
        String providerName = mine.getHoloProvider();
        HologramProvider provider = getProvider(providerName);
        if (provider == null && (providerName.equalsIgnoreCase("vanilla") || providerName.equalsIgnoreCase("fancyholograms"))) {
            providerName = "decentholograms";
            provider = getProvider(providerName);
        }
        if (provider != null) {
            HologramProvider chosen = provider;
            onRegion(mine, () -> chosen.update(mine));
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
            HologramProvider chosen = provider;
            onRegion(mine, () -> chosen.delete(mine));
        }
    }

    /**
     * Выключение плагина: планировщик уже не принимает задачи, поэтому удаляем напрямую.
     * На Folia из чужого потока это запрещено - но голограммы создаются непостоянными (setPersistent(false)),
     * и при остановке сервера всё равно не сохраняются, так что ошибку можно проглотить.
     */
    public void unloadAll() {
        try {
            HologramProvider vanilla = providers.get("vanilla");
            if (vanilla != null) {
                vanilla.removeAll();
            }
            for (Mine mine : plugin.getMineManager().getMines().values()) {
                HologramProvider provider = getProvider(mine.getHoloProvider());
                if (provider != null) provider.delete(mine);
            }
        } catch (Throwable ignored) {
            // Folia: см. комментарий выше
        }
    }
}

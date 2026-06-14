package ru.last.mines.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import dev.by1337.bmenu.BMenu;
import dev.by1337.bmenu.io.FileWatcher;
import dev.by1337.bmenu.loader.MenuSubLoader;
import dev.by1337.bmenu.menu.Menu;
import ru.last.mines.LastMines;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuiManager {
    private final LastMines plugin;
    private MenuSubLoader subLoader;
    private FileWatcher fileWatcher;
    private final Map<UUID, String> viewingMines = new HashMap<>();

    public GuiManager(LastMines plugin) { this.plugin = plugin; }

    public Map<UUID, String> getViewingMines() { return viewingMines; }

    public void register() {

        File menuDir = new File(plugin.getDataFolder(), "menus");
        if (!menuDir.exists()) {
            menuDir.mkdirs();
        }

        File defaultMenu = new File(menuDir, "main.yml");
        if (!defaultMenu.exists()) {
            plugin.saveResource("menus/main.yml", false);
            plugin.saveResource("menus/blocks.yml", false);
            plugin.saveResource("menus/rarities.yml", false);
            plugin.saveResource("menus/holograms.yml", false);
            plugin.saveResource("menus/actions.yml", false);
            plugin.saveResource("menus/permissions.yml", false);
            plugin.saveResource("menus/online.yml", false);
            plugin.saveResource("menus/all_mines.yml", false);
        }

        GuiProviderRegistry.register();

        subLoader = new MenuSubLoader(menuDir, plugin, BMenu.menuLoader());
        BMenu.menuLoader().registerSubLoader(plugin, subLoader);
        subLoader.loadMenus();

        fileWatcher = new FileWatcher(menuDir, this::onFileChange);
        fileWatcher.startWatching();
    }

    private void onFileChange(Path path) {
        if (!plugin.isEnabled()) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (subLoader != null) {
                    subLoader.reload();
                    plugin.getDebugLogger().info("GUI конфигурации перезагружены!");
                }
            } catch (Exception ex) { plugin.getDebugLogger().error("Ошибка при перезагрузке GUI", ex); }
        });
    }

    public void reload() { if (subLoader != null) subLoader.reload(); }

    public void unregister() {
        if (subLoader == null) return;
        BMenu.menuLoader().unregisterSubLoader(plugin);
        if (fileWatcher != null) {
            fileWatcher.stopWatching();
        }
    }
    
    public void openMenu(Player player, String mineId) {
        if (subLoader == null) return;
        try {
            viewingMines.put(player.getUniqueId(), mineId);
            Menu menu = BMenu.menuLoader().create("lastmines:main", player, null);
            menu.addArgument("MINE_ID", mineId);
            
            try {
                Class<?> resolverClass = Class.forName("dev.by1337.plc.PlaceholderResolver");
                Object proxy = Proxy.newProxyInstance(
                    plugin.getClass().getClassLoader(),
                    new Class[]{resolverClass},
                    (proxy1, method, args1) -> {
                        if (method.getName().equals("resolve")) {
                            if ("MINE_ID".equals(args1[0])) return mineId;
                            return null;
                        } else if (method.getName().equals("has")) {
                            return "MINE_ID".equals(args1[0]);
                        }
                        return null;
                    }
                );
                menu.getClass().getMethod("addPlaceholderResolver", resolverClass).invoke(menu, proxy);
            } catch (Exception ignore) {}
            
            menu.open();
        } catch (Exception e) {
            plugin.getDebugLogger().error("Failed to open main menu", e);
            player.sendMessage("§cНе удалось открыть меню автошахты. Проверьте консоль.");
        }
    }
}

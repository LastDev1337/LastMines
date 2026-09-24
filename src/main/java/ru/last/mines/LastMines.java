package ru.last.mines;

import dev.by1337.cmd.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import ru.last.mines.api.LastMinesAPIImpl;
import ru.last.mines.api.LastMinesProvider;
import ru.last.mines.commands.MainCommand;
import ru.last.mines.config.ConfigManager;
import ru.last.mines.debug.Debugger;
import ru.last.mines.gui.GuiManager;
import ru.last.mines.holograms.HologramManager;
import ru.last.mines.hooks.PlaceHook;
import ru.last.mines.listeners.Listeners;
import ru.last.mines.listeners.impl.*;
import ru.last.mines.managers.ActionManager;
import ru.last.mines.managers.BlockCatalogManager;
import ru.last.mines.managers.MineManager;

public class LastMines extends JavaPlugin {

    private static LastMines instance;
    private ConfigManager configManager;
    
    private MineManager mineManager;
    private ActionManager actionManager;
    private BlockCatalogManager blockCatalogManager;
    private HologramManager hologramManager;
    private Debugger debugger;
    private GuiManager guiManager;
    private Command<CommandSender> rootCommand;

    private static final int[] MIN_VERSION = {21};
    private static final int[] MAX_VERSION = {26, 3};

    @Override
    public void onEnable() {
        getLogger().info("enabling...");
        if (!isSupportedVersion()) {
            getLogger().severe("Plugin supported only 1.21 - 26.3 version. Current version (" + getServer().getBukkitVersion() + ") not supported. Plugin disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        actionManager = new ActionManager(this);
        actionManager.registerBMenuActions();
        instance = this;

        this.debugger = new Debugger();

        LastMinesProvider.setApi(new LastMinesAPIImpl(this));

        this.configManager = new ConfigManager(this);
        this.configManager.loadAll();
        if (this.configManager.getMainConfig() == null) {
            return;
        }

        this.blockCatalogManager = new BlockCatalogManager(this);
        this.blockCatalogManager.load();

        this.hologramManager = new HologramManager(this);
        
        this.mineManager = new MineManager(this);
        int delay = this.configManager.getMainConfig().getTimeLoadAllMines();
        if (delay > 0) {
            getServer().getScheduler().runTaskLater(this, () -> mineManager.loadAll(), delay * 20L);
        } else {
            this.mineManager.loadAll();
        }

        try {
            MainCommand.register(this);
        } catch (Exception e) {
            getLogger().severe("Failed to register /lastmines command: " + e.getMessage());
        }

        try {
            Listeners.register();
            getDebugger().info("Listeners registred successfully!");
        } catch (Exception e) {
            getLogger().severe("Listeners not registred for error: ");
            e.printStackTrace();
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceHook(this).register();
            getDebugger().info("PlaceholderAPI hooked!");
        }

        if (getServer().getPluginManager().isPluginEnabled("BMenu")) {
            try {
                this.guiManager = new GuiManager(this);
                this.guiManager.register();
            } catch (Throwable t) {
                this.guiManager = null;
                getLogger().warning("BMenu not registred for GUI Managers: " + t);
            }
        } else {
            this.guiManager = null;
            getLogger().warning("BMenu not found! GUI dont worked");
        }

        new UpdateChecker(this).check();

        Metrics metrics = new Metrics(this, 31925);
        metrics.addCustomChart(new SimplePie("chart_id", () -> "LastMines"));

        getLogger().info("enabling successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("disabling...");

        MainCommand.unregister(this);
        if (this.mineManager != null) { this.mineManager.unloadAll(); }
        if (this.hologramManager != null) { this.hologramManager.unloadAll(); }
        if (this.guiManager != null) { this.guiManager.unregister(); }

        getLogger().info("disabling successfully!");
    }

    private static int[] parseVersion(String raw) {
        String v = raw.startsWith("1.") ? raw.substring(2) : raw;
        String[] parts = v.split("\\.");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String digits = parts[i].replaceAll("[^0-9].*", "");
            if (digits.isEmpty()) return null;
            result[i] = Integer.parseInt(digits);
        }
        return result;
    }

    private static int compareVersions(int[] a, int[] b) {
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int av = i < a.length ? a[i] : 0;
            int bv = i < b.length ? b[i] : 0;
            if (av != bv) return Integer.compare(av, bv);
        }
        return 0;
    }

    private boolean isSupportedVersion() {
        String bukkitVersion = getServer().getBukkitVersion();
        String mcVersion = bukkitVersion.split("-")[0];
        int[] parsed = parseVersion(mcVersion);
        if (parsed == null) {
            getLogger().warning("Unable to detect the server version (" + bukkitVersion + "), skipping the compatibility check.");
            return true;
        }
        return compareVersions(parsed, MIN_VERSION) >= 0 && compareVersions(parsed, MAX_VERSION) <= 0;
    }

    public static LastMines get() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public MineManager getMineManager() { return mineManager; }
    public ActionManager getActionManager() { return actionManager; }
    public BlockCatalogManager getBlockCatalogManager() { return blockCatalogManager; }
    public HologramManager getHologramManager() { return hologramManager; }
    public Debugger getDebugger() { return debugger; }
    public GuiManager getGuiManager() { return guiManager; }
    public Command<CommandSender> getRootCommand() { return rootCommand; }
    public void setRootCommand(Command<CommandSender> rootCommand) { this.rootCommand = rootCommand; }
}

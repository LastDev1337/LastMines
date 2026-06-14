package ru.last.mines;

import org.bukkit.Bukkit;

import org.bukkit.plugin.java.JavaPlugin;
import ru.last.mines.commands.*;
import ru.last.mines.config.*;
import ru.last.mines.debug.*;
import ru.last.mines.gui.*;
import ru.last.mines.hooks.*;
import ru.last.mines.listeners.*;
import ru.last.mines.managers.*;
import ru.last.mines.api.*;
import ru.last.mines.holograms.*;
import ru.last.mines.listeners.MineEventListener;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;

import java.util.Objects;

public class LastMines extends JavaPlugin {

    private static LastMines instance;
    private ConfigManager configManager;
    
    private MineManager mineManager;
    private ru.last.mines.managers.ActionManager actionManager;
    private HologramManager hologramManager;
    private DebugLogger debugLogger;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        actionManager = new ActionManager(this);
        actionManager.registerBMenuActions();
        instance = this;
        getLogger().info("enabling...");

        this.debugLogger = new DebugLogger();

        LastMinesProvider.setApi(new LastMinesAPIImpl(this));

        this.configManager = new ConfigManager(this);
        this.configManager.loadAll();

        this.hologramManager = new HologramManager(this);
        
        this.mineManager = new MineManager(this);
        int delay = this.configManager.getMainConfig().getTimeLoadAllMines();
        if (delay > 0) {
            getServer().getScheduler().runTaskLater(this, () -> mineManager.loadAll(), delay * 20L);
        } else {
            this.mineManager.loadAll();
        }

        Objects.requireNonNull(getCommand("lastmines")).setExecutor(new MainCommand(this));
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new MineEventListener(this), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceHook(this).register();
        }
        
        this.guiManager = new ru.last.mines.gui.GuiManager(this);
        this.guiManager.register();
        
        new UpdateChecker(this).check();

        Metrics metrics = new Metrics(this, 31925);
        metrics.addCustomChart(new SimplePie("chart_id", () -> "My value"));

        getLogger().info("enabling successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("disabling...");
        if (this.mineManager != null) { this.mineManager.unloadAll(); }
        if (this.hologramManager != null) { this.hologramManager.unloadAll(); }
        if (this.guiManager != null) { this.guiManager.unregister(); }
        getLogger().info("disabling successfully!");
    }

    public static LastMines getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public MineManager getMineManager() { return mineManager; }
    public ActionManager getActionManager() { return actionManager; }
    public HologramManager getHologramManager() { return hologramManager; }
    public DebugLogger getDebugLogger() { return debugLogger; }
    public GuiManager getGuiManager() { return guiManager; }
}

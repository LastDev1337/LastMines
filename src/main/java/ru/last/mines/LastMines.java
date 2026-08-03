package ru.last.mines;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
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
import ru.last.mines.listeners.BlockListener;
import ru.last.mines.listeners.MineEventListener;
import ru.last.mines.listeners.chance.ChanceInputListener;
import ru.last.mines.listeners.drop.DragDropListener;
import ru.last.mines.listeners.drops.BlockDropsListener;
import ru.last.mines.listeners.limit.LimitInputListener;
import ru.last.mines.listeners.perm.PermInputListener;
import ru.last.mines.listeners.text.TextInputListener;
import ru.last.mines.managers.ActionManager;
import ru.last.mines.managers.BlockCatalogManager;
import ru.last.mines.managers.MineManager;

import java.lang.reflect.Field;
import java.util.List;

public class LastMines extends JavaPlugin {

    private static LastMines instance;
    private ConfigManager configManager;
    
    private MineManager mineManager;
    private ActionManager actionManager;
    private BlockCatalogManager blockCatalogManager;
    private HologramManager hologramManager;
    private Debugger debugger;
    private GuiManager guiManager;
    private ChanceInputListener chanceInputListener;
    private LimitInputListener limitInputListener;
    private PermInputListener permInputListener;
    private BlockDropsListener blockDropsListener;
    private TextInputListener textInputListener;
    private MainCommand mainCommand;

    private static final int[] MIN_VERSION = {21};
    private static final int[] MAX_VERSION = {26, 2};

    @Override
    public void onEnable() {
        if (!isSupportedVersion()) {
            getLogger().severe("Plugin supported only 1.21 - 26.2 version. Current version (" + getServer().getBukkitVersion() + ") not supported. Plugin disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        actionManager = new ActionManager(this);
        actionManager.registerBMenuActions();
        instance = this;
        getLogger().info("enabling...");

        this.debugger = new Debugger();

        LastMinesProvider.setApi(new LastMinesAPIImpl(this));

        this.configManager = new ConfigManager(this);
        this.configManager.loadAll();

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

        mainCommand = new MainCommand(this);
        Command bukkitCommand = new Command("lastmines", "LastMines main command", "/lastmines", List.of("mines")) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                return mainCommand.onCommand(sender, this, label, args);
            }
            @Override
            public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
                return mainCommand.onTabComplete(sender, this, alias, args);
            }
        };
        try {
            Field commandMapField = getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (org.bukkit.command.CommandMap) commandMapField.get(getServer());
            commandMap.register(getName(), bukkitCommand);
        } catch (Exception e) {
            getLogger().severe("Failed to register command via reflection: " + e.getMessage());
        }
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new MineEventListener(this), this);
        
        this.chanceInputListener = new ChanceInputListener();
        this.limitInputListener = new LimitInputListener();
        this.permInputListener = new PermInputListener();
        getServer().getPluginManager().registerEvents(this.chanceInputListener, this);
        getServer().getPluginManager().registerEvents(this.limitInputListener, this);
        getServer().getPluginManager().registerEvents(this.permInputListener, this);
        getServer().getPluginManager().registerEvents(new DragDropListener(), this);

        this.blockDropsListener = new BlockDropsListener();
        getServer().getPluginManager().registerEvents(this.blockDropsListener, this);

        this.textInputListener = new TextInputListener();
        getServer().getPluginManager().registerEvents(this.textInputListener, this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceHook(this).register();
        }
        
        if (getServer().getPluginManager().getPlugin("BMenu") != null) {
            this.guiManager = new GuiManager(this);
            this.guiManager.register();
        }

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

    /**
     Парсер версий с 1.21 до 26.2 чтобы не было конфликтов
     */
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
            getLogger().warning("Не удалось распознать версию сервера (" + bukkitVersion + "), пропускаю проверку совместимости.");
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
    
    public MainCommand getMainCommand() { return mainCommand; }
    public ChanceInputListener getChanceInputListener() { return chanceInputListener; }
    public LimitInputListener getLimitInputListener() { return limitInputListener; }
    public PermInputListener getPermInputListener() { return permInputListener; }
    public BlockDropsListener getBlockDropsListener() { return blockDropsListener; }
    public TextInputListener getTextInputListener() { return textInputListener; }
}

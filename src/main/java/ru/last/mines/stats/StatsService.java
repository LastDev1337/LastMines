package ru.last.mines.stats;

import dev.laststudio.lib.api.LLibAPI;
import dev.laststudio.lib.api.scheduler.SchedulerService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.last.mines.LastMines;
import ru.last.mines.db.DBManager;
import ru.last.mines.db.DBProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class StatsService implements Listener {

    private static final String ALL = "*";

    private final LastMines plugin;
    private final DBManager dbManager;
    private volatile DBProvider provider;

    private final Map<UUID, Delta> pending = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();
    private final Map<String, List<TopEntry>> topCache = new ConcurrentHashMap<>();
    private final Set<String> topKeys = ConcurrentHashMap.newKeySet();
    private final List<ScheduledTask> tasks = new ArrayList<>();

    public StatsService(LastMines plugin) {
        this.plugin = plugin;
        this.dbManager = new DBManager(plugin);
    }

    public void start() {
        provider = dbManager.connect();
        if (provider == null) return;
        topKeys.add(ALL);
        SchedulerService s = LLibAPI.get().scheduler();
        DBManager.Settings cfg = dbManager.settings();
        long flushTicks = cfg.flushSeconds() * 20L;
        long topTicks = cfg.topRefreshSeconds() * 20L;
        tasks.add(s.asyncTimer(plugin, this::flushNow, flushTicks, flushTicks));
        tasks.add(s.asyncTimer(plugin, this::refresh, 20L, topTicks));
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (Player p : Bukkit.getOnlinePlayers()) {
            touchName(p);
            s.async(plugin, () -> loadPlayer(p.getUniqueId(), p.getName()));
        }
    }

    public void stop() {
        for (ScheduledTask t : tasks) t.cancel();
        tasks.clear();
        if (provider != null) {
            flushNow();
            provider.close();
            provider = null;
        }
    }

    public boolean enabled() {
        return provider != null;
    }

    public void record(Player player, Material material) {
        if (provider == null) return;
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        pending.compute(uuid, (k, d) -> {
            Delta delta = d != null ? d : new Delta(name);
            delta.name = name;
            delta.add(material);
            return delta;
        });
        PlayerStats stats = cache.get(uuid);
        if (stats != null) stats.add(material);
    }

    public synchronized void flushNow() {
        DBProvider db = provider;
        if (db == null || pending.isEmpty()) return;
        Map<UUID, Delta> batch = new HashMap<>();
        for (UUID uuid : new ArrayList<>(pending.keySet())) {
            Delta d = pending.remove(uuid);
            if (d != null) batch.put(uuid, d);
        }
        if (batch.isEmpty()) return;
        try {
            db.flush(batch);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Не удалось записать статистику в базу, повтор позже.", e);
            batch.forEach((uuid, d) -> pending.merge(uuid, d, Delta::merge));
        }
    }

    private void touchName(Player player) {
        String name = player.getName();
        pending.compute(player.getUniqueId(), (k, d) -> {
            Delta delta = d != null ? d : new Delta(name);
            delta.name = name;
            return delta;
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        touchName(player);
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        LLibAPI.get().scheduler().async(plugin, () -> loadPlayer(uuid, name));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cache.remove(event.getPlayer().getUniqueId());
    }

    private void loadPlayer(UUID uuid, String name) {
        DBProvider db = provider;
        if (db == null) return;
        try {
            PlayerStats stats = db.load(uuid, name);
            Delta unsaved = pending.get(uuid);
            if (unsaved != null) stats.addDelta(unsaved);
            cache.put(uuid, stats);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Не удалось загрузить статистику игрока " + name, e);
        }
    }

    private void refresh() {
        DBProvider db = provider;
        if (db == null) return;
        int size = dbManager.settings().topSize();
        for (String key : new ArrayList<>(topKeys)) {
            loadTop(db, key, size);
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            loadPlayer(p.getUniqueId(), p.getName());
        }
    }

    private List<TopEntry> loadTop(DBProvider db, String key, int size) {
        try {
            List<TopEntry> list = db.top(ALL.equals(key) ? null : Material.valueOf(key), size);
            topCache.put(key, list);
            return list;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Не удалось обновить топ статистики (" + key + ")", e);
            return topCache.getOrDefault(key, List.of());
        }
    }

    public List<TopEntry> cachedTop(Material material) {
        String key = material == null ? ALL : material.name();
        List<TopEntry> list = topCache.get(key);
        if (list != null) return list;
        if (provider != null && topKeys.add(key)) {
            LLibAPI.get().scheduler().async(plugin, () -> {
                DBProvider db = provider;
                if (db != null) loadTop(db, key, dbManager.settings().topSize());
            });
        }
        return List.of();
    }

    public PlayerStats cachedStats(UUID uuid) {
        return cache.get(uuid);
    }

    public List<TopEntry> topBlocking(Material material) {
        String key = material == null ? ALL : material.name();
        List<TopEntry> list = topCache.get(key);
        if (list != null) return list;
        DBProvider db = provider;
        if (db == null) return List.of();
        topKeys.add(key);
        return loadTop(db, key, dbManager.settings().topSize());
    }

    public PlayerStats lookup(String name) {
        DBProvider db = provider;
        if (db == null) return null;
        Player online = Bukkit.getPlayerExact(name);
        try {
            if (online != null) {
                PlayerStats cached = cache.get(online.getUniqueId());
                if (cached != null) return cached;
                loadPlayer(online.getUniqueId(), online.getName());
                return cache.get(online.getUniqueId());
            }
            UUID uuid = db.findUuid(name);
            if (uuid == null) return null;
            PlayerStats stats = db.load(uuid, name);
            Delta unsaved = pending.get(uuid);
            if (unsaved != null) stats.addDelta(unsaved);
            return stats;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Не удалось получить статистику игрока " + name, e);
            return null;
        }
    }

    public void reply(CommandSender sender, Runnable action) {
        SchedulerService s = LLibAPI.get().scheduler();
        if (sender instanceof Player player) s.entity(plugin, player, action);
        else s.global(plugin, action);
    }

    public void async(Runnable action) {
        LLibAPI.get().scheduler().async(plugin, action);
    }

    public int topSize() {
        return dbManager.settings().topSize();
    }
}

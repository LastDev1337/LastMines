package ru.last.mines.mine;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import dev.laststudio.lib.api.LLibAPI;
import dev.laststudio.lib.api.action.ActionPlaceholders;
import dev.laststudio.lib.api.scheduler.SchedulerService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import ru.last.mines.actions.MineAction;
import ru.last.mines.actions.MineActionParser;
import ru.last.mines.LastMines;
import ru.last.mines.api.events.*;
import ru.last.mines.hooks.WEHook;
import ru.last.mines.mine.models.*;
import ru.last.mines.utils.*;
import ru.last.mines.utils.time.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Mine {
    private final LastMines plugin;
    private final YamlMap sourceMap;
    private final String id;
    private final String name;
    private final World world;
    
    private final MineMode mode;

    private final MineShape shape;
    private volatile int resetPercent;
    private Material filler = Material.STONE;
    private final Object weRegion;
    private final List<int[]> regionPoints;

    private final Location pos1;
    private final Location pos2;

    private boolean enable;

    private final List<MineBlock> blocks = new ArrayList<>();
    private final List<MineRarity> rarities = new ArrayList<>();

    private final Map<Integer, List<MineAction>> updateActions = new ConcurrentHashMap<>();
    private final List<MineAction> resetActions = new CopyOnWriteArrayList<>();
    private final List<MineAction> breakActions = new CopyOnWriteArrayList<>();
    private List<String> rawActions = new ArrayList<>();
    
    private boolean permEnable;
    private String permValue;
    private List<String> permMessages;

    private boolean enchantEnable;
    private final Map<String, Integer> requiredEnchants = new LinkedHashMap<>();
    private final List<String> enchantFailActions;

    private boolean onlineEnable;
    private int onlineMin;
    private int onlineMax;

    private boolean tpEnable;
    private Location tpPos;

    private boolean holoEnable;
    private String holoProvider;
    private Location holoOffset;
    private List<String> holoTexts;
    private final YamlMap holoMap;

    private int resetTime;
    private boolean stopped;
    private ScheduledTask task;
    private volatile int timeLeft;
    
    private MineRarity currentRarity;
    private MineRarity nextRarity;
    
    private final java.util.Set<Long> playerPlaced = ConcurrentHashMap.newKeySet();
    private final AtomicInteger physicalBlocksCount = new AtomicInteger();
    private volatile int maxPhysicalBlocksCount = 0;

    private static class PeriodicAction {
        int interval;
        boolean repeat;
        int initialDuration;
        
        int durationLeft;
        MineAction action;
        int counter;
        boolean active;
    }
    private final List<PeriodicAction> periodicActions = new CopyOnWriteArrayList<>();

    public Mine(LastMines plugin, String id, YamlMap map) {
        this.plugin = plugin;
        this.sourceMap = map;
        this.id = id;
        this.name = map.get("name").asString(id);
        this.world = Bukkit.getWorld(map.get("world").asString("world"));
        
        String modeStr = map.get("mode").asString("BLOCKS").toUpperCase();
        MineMode parsedMode = MineMode.BLOCKS;
        try { parsedMode = MineMode.valueOf(modeStr); } catch (Exception ignored) {}
        this.mode = parsedMode;
        
        boolean hasPositions = map.has("positions");
        YamlMap posMap = map.get("positions").asYamlMap().orDefault(new YamlMap());
        MineShape parsedShape;
        try { parsedShape = MineShape.fromString(posMap.get("shape").asString("cuboid")); }
        catch (Exception ignored) { parsedShape = MineShape.CUBOID; }
        this.shape = parsedShape;

        List<String> shapePoints = getListOrEmpty(posMap.get("points")).stream().map(v -> YamlValue.wrap(v).asString("")).toList();
        if (this.shape == MineShape.CUBOID) {
            if (!hasPositions) {
                // шахта без области (например, копия): работает только как набор настроек, пока не задана область
                this.pos1 = null;
                this.pos2 = null;
            } else {
                this.pos1 = parseLocation(world, !shapePoints.isEmpty() ? shapePoints.get(0) : "0;100;0");
                this.pos2 = parseLocation(world, shapePoints.size() > 1 ? shapePoints.get(1) : "5;105;5");
            }
            this.weRegion = null;
        } else {
            Object region = WEHook.buildRegion(world, this.shape, shapePoints);
            this.weRegion = region;
            if (region != null) {
                int[] min = WEHook.minPoint(region);
                int[] max = WEHook.maxPoint(region);
                this.pos1 = new Location(world, min[0], min[1], min[2]);
                this.pos2 = new Location(world, max[0], max[1], max[2]);
            } else {
                plugin.getLogger().warning("[LastMines] Не удалось построить фигуру '" + this.shape + "' для шахты " + id + " (требуется WorldEdit). Шахта не будет работать корректно.");
                this.pos1 = new Location(world, 0, 100, 0);
                this.pos2 = new Location(world, 0, 100, 0);
            }
        }
        this.regionPoints = this.weRegion != null ? WEHook.points(this.weRegion) : Collections.emptyList();

        this.resetPercent = Math.max(0, Math.min(99, map.get("reset_percent").asInt(0)));
        Material parsedFiller = Material.matchMaterial(map.get("filler").asString("stone").replace("minecraft:", "").toUpperCase());
        this.filler = parsedFiller != null && parsedFiller.isBlock() ? parsedFiller : Material.STONE;

        if (this.mode == MineMode.BLOCKS || this.mode == MineMode.ALL_BLOCKS) {
            for (Object blockValRaw : getListOrEmpty(map.get("blocks"))) {
                YamlMap blockMap = YamlValue.wrap(blockValRaw).asYamlMap().getOrThrow();
                MineBlock parsed = parseBlock(blockMap);
                if (parsed != null) blocks.add(parsed);
            }
        } else {
            for (Object rarityValRaw : getListOrEmpty(map.get("rarity"))) {
                YamlMap rarityMap = YamlValue.wrap(rarityValRaw).asYamlMap().getOrThrow();
                String rId = rarityMap.get("id").asString("");
                double rChance = rarityMap.get("chance").asDouble(100.0);
                String rName = rarityMap.get("name").asString(rId).replace("&", "§");
                String rIcon = rarityMap.has("icon") ? rarityMap.get("icon").asString(null) : null;

                List<MineBlock> rBlocks = new ArrayList<>();
                for (Object blockValRaw : getListOrEmpty(rarityMap.get("blocks"))) {
                    YamlMap blockMap = YamlValue.wrap(blockValRaw).asYamlMap().getOrThrow();
                    MineBlock parsed = parseBlock(blockMap);
                    if (parsed != null) rBlocks.add(parsed);
                }
                rarities.add(new MineRarity(rId, rChance, rName, rBlocks, rIcon));
            }
            if (!rarities.isEmpty()) {
                this.currentRarity = rollRarity();
                this.nextRarity = rollRarity();
            }
        }
        
        reparseActions(getListOrEmpty(map.get("actions")).stream().map(v -> YamlValue.wrap(v).asString("")).toList());

        YamlMap permMap = map.get("permissions").asYamlMap().orDefault(new YamlMap());
        this.permEnable = permMap.get("enable").asBool(false);
        this.permValue = permMap.get("value").asString("");
        this.permMessages = getListOrEmpty(permMap.get("messages")).stream().map(v -> YamlValue.wrap(v).asString("")).toList();

        YamlMap enchantMap = map.get("enchant_requirements").asYamlMap().orDefault(new YamlMap());
        this.enchantEnable = enchantMap.get("enable").asBool(false);
        YamlMap enchListMap = enchantMap.get("enchantments").asYamlMap().orDefault(new YamlMap());
        for (String key : enchListMap.getRaw().keySet()) {
            requiredEnchants.put(key.toLowerCase(Locale.ROOT), enchListMap.get(key).asInt(1));
        }
        List<String> defaultEnchantActions = List.of("[message] &cВаша кирка недостаточно зачарована для этой автошахты!");
        List<String> parsedEnchantActions = getListOrEmpty(enchantMap.get("actions")).stream().map(v -> YamlValue.wrap(v).asString("")).toList();
        this.enchantFailActions = parsedEnchantActions.isEmpty() ? defaultEnchantActions : parsedEnchantActions;

        YamlMap onlineMap = map.get("online").asYamlMap().orDefault(new YamlMap());
        this.onlineEnable = onlineMap.get("enable").asBool(false);
        this.onlineMin = onlineMap.get("min").asInt(0);
        this.onlineMax = onlineMap.get("max").asInt(1000);
        
        YamlMap tpMap = map.get("teleport").asYamlMap().orDefault(new YamlMap());
        this.tpEnable = tpMap.get("enable").asBool(false);
        this.tpPos = parseLocation(world, tpMap.get("pos").asString("0;100;0"));
        
        this.holoMap = map.get("hologram").asYamlMap().orDefault(new YamlMap());
        this.holoEnable = holoMap.get("enable").asBool(false);
        this.holoProvider = holoMap.get("provider").asString("Vanilla");
        if (holoMap.has("offset")) {
            YamlValue offsetVal = holoMap.get("offset");
            if (offsetVal.getRaw() instanceof java.util.Map) {
                YamlMap offsetMap = offsetVal.asYamlMap().getOrThrow();
                double x = offsetMap.get("x").asDouble(0.0);
                double y = offsetMap.get("y").asDouble(0.0);
                double z = offsetMap.get("z").asDouble(0.0);
                this.holoOffset = new Location(world, x, y, z);
            } else {
                this.holoOffset = parseLocation(world, offsetVal.asString("0;0;0"));
            }
        } else {
            this.holoOffset = new Location(world, 0, 100, 0);
        }
        this.holoTexts = getListOrEmpty(holoMap.get("texts")).stream().map(v -> YamlValue.wrap(v).asString("")).toList();

        this.resetTime = TimeUtils.parseToSeconds(map.get("reset_time").asString("5m"));
        this.timeLeft = this.resetTime;
        this.stopped = map.get("stopped").asBool(false);
        this.enable = map.get("enable").asBool(true);

        startTasks();
        createHologram();
        resetMine();
    }

    public void reparseActions(List<String> raw) {
        this.rawActions = new ArrayList<>(raw);
        periodicActions.clear();
        updateActions.clear();
        resetActions.clear();
        breakActions.clear();

        Pattern p = Pattern.compile("(?i)^(?:\\[update[^]]*]\\s*)?(\\[actionbar]|\\[message]|\\[title]|\\[sound])\\s*(?:\\[radius:-?\\d+]\\s*)?(\\d+)\\s+(true|false)\\s+(.*)");

        for (String act : raw) {
            Matcher m = p.matcher(act);
            if (m.matches()) {
                PeriodicAction pa = new PeriodicAction();
                boolean repeat = Boolean.parseBoolean(m.group(3));
                int val = Integer.parseInt(m.group(2));

                if (repeat) {
                    pa.interval = val;
                    pa.repeat = true;
                    pa.initialDuration = -1;
                } else {
                    pa.interval = 1;
                    pa.repeat = false;
                    pa.initialDuration = val;
                }
                pa.durationLeft = pa.initialDuration;
                pa.counter = pa.interval;
                pa.active = true;

                String toRemove = m.group(2) + " " + m.group(3) + " ";
                int idx = act.indexOf(toRemove);
                String periodicText = idx != -1 ? act.substring(0, idx) + act.substring(idx + toRemove.length()) : act;
                MineActionParser.Parsed parsed = MineActionParser.parse(periodicText, plugin.getLogger());
                if (parsed == null) continue;
                pa.action = parsed.action();
                periodicActions.add(pa);
                continue;
            }

            MineActionParser.Parsed parsed = MineActionParser.parse(act, plugin.getLogger());
            if (parsed == null) continue;
            switch (parsed.when()) {
                case UPDATE_RESET -> resetActions.add(parsed.action());
                case UPDATE_AT -> updateActions.computeIfAbsent(parsed.seconds(), k -> new CopyOnWriteArrayList<>()).add(parsed.action());
                case BREAK -> breakActions.add(parsed.action());
                case NONE -> {}
            }
        }
    }

    private MineBlock parseBlock(YamlMap blockMap) {
        String blockId = blockMap.get("id").asString("minecraft:stone").replace("minecraft:", "").toUpperCase();
        Material material = Material.matchMaterial(blockId);
        if (material == null) {
            plugin.getLogger().warning("[LastMines] Неизвестный материал '" + blockId + "' в шахте " + id + ", блок пропущен.");
            return null;
        }
        double chance = blockMap.has("chance") ? blockMap.get("chance").asDouble(100.0) : 0.0;
        int min = -1, max = -1;

        if (mode == MineMode.ALL_BLOCKS && blockMap.has("count")) {
            int count = Math.max(0, blockMap.get("count").asInt(0));
            List<DropItem> countDrops = getListOrEmpty(blockMap.get("drop")).stream()
                    .map(this::parseDropItem).filter(Objects::nonNull).toList();
            return new MineBlock(material, 0.0, count, count, countDrops);
        }
        
        if (blockMap.has("limit")) {
            if (blockMap.has("chance")) {
                plugin.getLogger().warning("[LastMines] У блока " + blockId + " в шахте " + id + " указан и chance, и limit! Используется chance, limit проигнорирован.");
            } else {
                YamlValue limitVal = blockMap.get("limit");
                YamlMap lMap = null;
                try { lMap = limitVal.asYamlMap().orDefault(null); } catch (Exception ignored) {}
                
                if (lMap != null) {
                    min = lMap.get("min").asInt(1);
                    max = lMap.get("max").asInt(1);
                } else {
                    String str = limitVal.asString(null);
                    if (str != null && str.contains(";")) {
                        String[] split = str.split(";");
                        if (split.length >= 2) {
                            try {
                                min = Integer.parseInt(split[0].trim());
                                max = Integer.parseInt(split[1].trim());
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        } else if (!blockMap.has("chance")) {
            chance = 100.0;
        }

        List<DropItem> drops = getListOrEmpty(blockMap.get("drop")).stream()
                .map(this::parseDropItem)
                .filter(Objects::nonNull)
                .toList();
        return new MineBlock(material, chance, min, max, drops);
    }

    private DropItem parseDropItem(Object raw) {
        YamlValue val = YamlValue.wrap(raw);
        if (val.getRaw() instanceof java.util.Map) {
            YamlMap dropMap = val.asYamlMap().getOrThrow();
            String matName = dropMap.get("id").asString("").replace("minecraft:", "").toUpperCase();
            Material mat = Material.matchMaterial(matName);
            if (mat == null) return null;
            double chance = dropMap.get("chance").asDouble(100.0);
            boolean fortune = dropMap.get("fortune").asBool(false);
            return new DropItem(mat, chance, fortune);
        }
        String matName = val.asString("").replace("minecraft:", "").toUpperCase();
        Material mat = Material.matchMaterial(matName);
        return mat == null ? null : new DropItem(mat, 100.0, false);
    }

    private Map<String, Object> serializeBlock(MineBlock mb) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", mb.material() != null ? mb.material().name().toLowerCase(Locale.ROOT) : "stone");
        if (mb.min() >= 0 && mb.max() >= mb.min()) {
            Map<String, Object> limit = new LinkedHashMap<>();
            limit.put("min", mb.min());
            limit.put("max", mb.max());
            out.put("limit", limit);
        } else {
            out.put("chance", mb.chance());
        }
        List<Map<String, Object>> dropList = new ArrayList<>();
        for (DropItem di : mb.drops()) {
            Map<String, Object> dm = new LinkedHashMap<>();
            dm.put("id", di.material().name().toLowerCase(Locale.ROOT));
            dm.put("chance", di.chance());
            dm.put("fortune", di.fortune());
            dropList.add(dm);
        }
        out.put("drop", dropList);
        return out;
    }

    private void set(String path, Object value) {
        try {
            sourceMap.set(path, value);
        } catch (IllegalStateException e) {
            String root = path.split("\\.")[0];
            sourceMap.set(root, null);
            sourceMap.set(path, value);
        }
    }

    public void save() {
        try {
            if (mode == MineMode.BLOCKS || mode == MineMode.ALL_BLOCKS) {
                List<Map<String, Object>> blockList = new ArrayList<>();
                for (MineBlock mb : blocks) blockList.add(serializeBlock(mb));
                set("blocks", blockList);
            } else if (mode == MineMode.RARITY) {
                List<Map<String, Object>> rarityList = new ArrayList<>();
                for (MineRarity r : rarities) {
                    Map<String, Object> rm = new LinkedHashMap<>();
                    rm.put("id", r.id());
                    rm.put("chance", r.chance());
                    rm.put("name", r.name());
                    if (r.icon() != null) rm.put("icon", r.icon());
                    List<Map<String, Object>> blockList = new ArrayList<>();
                    for (MineBlock mb : r.blocks()) blockList.add(serializeBlock(mb));
                    rm.put("blocks", blockList);
                    rarityList.add(rm);
                }
                set("rarity", rarityList);
            }

            set("hologram.texts", holoTexts);
            set("hologram.enable", holoEnable);
            set("hologram.provider", holoProvider);
            if (holoOffset != null) {
                set("hologram.offset", holoOffset.getX() + ";" + holoOffset.getY() + ";" + holoOffset.getZ());
            }
            set("permissions.enable", permEnable);
            set("permissions.value", permValue);
            set("permissions.messages", permMessages);
            set("reset_time", resetTime);
            set("reset_percent", resetPercent);
            set("filler", filler.name().toLowerCase(Locale.ROOT));
            set("stopped", stopped);
            set("enable", enable);
            set("enchant_requirements.enable", enchantEnable);
            set("enchant_requirements.enchantments", new LinkedHashMap<>(requiredEnchants));
            set("online.enable", onlineEnable);
            set("online.min", onlineMin);
            set("online.max", onlineMax);
            set("teleport.enable", tpEnable);
            if (tpPos != null) {
                set("teleport.pos", tpPos.getX() + ";" + tpPos.getY() + ";" + tpPos.getZ() + ";" + tpPos.getYaw() + ";" + tpPos.getPitch());
            }
            set("actions", rawActions);

            File file = new File(plugin.getDataFolder(), "mines" + File.separator + id + ".yml");
            Files.writeString(file.toPath(), sourceMap.saveToString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Не удалось сохранить шахту " + id, e);
        }
    }


    private MineRarity rollRarity() {
        if (rarities.isEmpty()) return null;
        double total = rarities.stream().mapToDouble(MineRarity::chance).sum();
        double r = Math.random() * total;
        double current = 0;
        for (MineRarity rarity : rarities) {
            current += rarity.chance();
            if (r <= current) return rarity;
        }
        return rarities.getFirst();
    }

    private List<Object> getListOrEmpty(YamlValue node) {
        if (node != null && node.getRaw() instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        return Collections.emptyList();
    }

    private Location parseLocation(World w, String s) {
        String[] split = s.split("[;,]");
        if (split.length < 3) return new Location(w, 0, 100, 0);
        try {
            float yaw = split.length >= 4 ? Float.parseFloat(split[3]) : 0f;
            float pitch = split.length >= 5 ? Float.parseFloat(split[4]) : 0f;
            return new Location(w, Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]), yaw, pitch);
        } catch (Exception e) { return new Location(w, 0, 100, 0); }
    }

    public void startTasks() {
        if (task != null) task.cancel();
        task = scheduler().globalTimer(plugin, this::tick, 20L, 20L);
    }

    private volatile ResetTask resetTask;

    public void stopTasks() {
        if (task != null) task.cancel();
        ResetTask running = resetTask;
        if (running != null) running.cancel();
    }

    private static SchedulerService scheduler() {
        return LLibAPI.get().scheduler();
    }
    
    private void tick() {
        if (!enable) return;
        if (stopped) {
            updateHologram();
            return;
        }
        if (onlineEnable) {
            int online = Bukkit.getOnlinePlayers().size();
            if (online < onlineMin || online > onlineMax) return;
        }

        for (PeriodicAction pa : periodicActions) {
            if (!pa.active) continue;

            pa.counter--;
            if (pa.counter <= 0) {
                executeAction(pa.action, null, null);

                if (pa.repeat) {
                    pa.counter = pa.interval;
                } else {
                    pa.durationLeft--;
                    if (pa.durationLeft <= 0) {
                        pa.active = false;
                    } else {
                        pa.counter = pa.interval;
                    }
                }
            }
        }

        timeLeft--;
        if (updateActions.containsKey(timeLeft)) {
            for (MineAction act : updateActions.get(timeLeft)) {
                executeAction(act, null, null);
            }
        }
        
        if (timeLeft <= 0) {
            forceUpdate();
        }
        
        updateHologram();
    }
    
    public boolean forceUpdate() {
        if (isResetting) return false;

        if (tpEnable && tpPos != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (isInMine(p.getLocation())) {
                    scheduler().teleport(p, tpPos);
                }
            }
        }
        if (mode == MineMode.RARITY) {
            MineRarity oldRarity = currentRarity;
            currentRarity = nextRarity;
            nextRarity = rollRarity();
            Bukkit.getPluginManager().callEvent(new MineUpdateRarityEvent(this, oldRarity, currentRarity));
        }
        return resetMine();
    }
    
    public String replacePlaceholders(String str) {
        if (str == null) return null;
        if (mode == MineMode.RARITY) {
            str = str.replace("%rarity:this%", currentRarity != null ? currentRarity.name() : "");
            str = str.replace("%rarity_current%", currentRarity != null ? currentRarity.name() : "");
            str = str.replace("%rarity:next%", nextRarity != null ? nextRarity.name() : "");
            str = str.replace("%rarity_next%", nextRarity != null ? nextRarity.name() : "");
        } else {
            str = str.replace("%rarity:this%", "");
            str = str.replace("%rarity_current%", "");
            str = str.replace("%rarity:next%", "");
            str = str.replace("%rarity_next%", "");
        }
        str = str.replace("{MINE_ID}", id);
        str = str.replace("{BLOCKS_LEFT}", String.valueOf(physicalBlocksCount.get()));
        str = str.replace("{BLOCKS_MAX}", String.valueOf(maxPhysicalBlocksCount));
        
        str = str.replace("%time%", TimeFormatter.format(timeLeft * 1000L, "default"));
        str = str.replace("%time_detail%", TimeFormatter.format(timeLeft * 1000L, "detail"));
        str = str.replace("%time:detail%", TimeFormatter.format(timeLeft * 1000L, "detail"));
        str = str.replace("%time_clock%", TimeFormatter.format(timeLeft * 1000L, "clock"));
        str = str.replace("%time:clock%", TimeFormatter.format(timeLeft * 1000L, "clock"));
        
        return str;
    }

    /** Запустить действия шахты для нужных игроков. breaker/material заполнены только для [break]. */
    public void executeAction(MineAction action, Player breaker, Material material) {
        Collection<? extends org.bukkit.command.CommandSender> targets = action.global()
                ? List.of(Bukkit.getConsoleSender())
                : selectPlayers(action, breaker);
        if (targets.isEmpty()) return;
        LLibAPI.get().actions().execute(plugin, targets, action.parsed(), placeholders(breaker, material));
    }

    /** Действия [break] / [break:МАТЕРИАЛ] для добытого блока. */
    public void runBreakActions(Player breaker, Material material) {
        for (MineAction action : breakActions) {
            if (action.material() == null || action.material() == material) {
                executeAction(action, breaker, material);
            }
        }
    }

    /** Строки без тега "когда" (сообщения о правах, о зачарованиях) - только одному игроку. */
    public void runPlayerActions(Player player, List<String> rawActions) {
        for (String raw : rawActions) {
            MineActionParser.Parsed parsed = MineActionParser.parse(raw, plugin.getLogger());
            if (parsed == null) continue;
            LLibAPI.get().actions().execute(plugin, List.of(player), parsed.action().parsed(), placeholders(player, null));
        }
    }

    private ActionPlaceholders placeholders(Player breaker, Material material) {
        boolean papi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        return (target, text) -> {
            String t = replacePlaceholders(text).replace("%mine%", id);
            t = t.replace("%breaker%", breaker != null ? breaker.getName() : "");
            if (material != null) t = t.replace("%material%", material.name());
            if (target instanceof Player p) {
                t = t.replace("%player%", p.getName());
                if (papi) t = PlaceholderAPI.setPlaceholders(p, t);
            } else {
                t = t.replace("%player%", breaker != null ? breaker.getName() : "");
            }
            return t;
        };
    }

    private Collection<? extends Player> selectPlayers(MineAction action, Player breaker) {
        switch (action.target()) {
            case SELF:
                return breaker == null ? List.of() : List.of(breaker);
            case MINE:
                return Bukkit.getOnlinePlayers().stream().filter(p -> distanceToMine(p.getLocation()) == 0).toList();
            case RADIUS:
                return Bukkit.getOnlinePlayers().stream().filter(p -> distanceToMine(p.getLocation()) <= action.radius()).toList();
            default:
                return Bukkit.getOnlinePlayers();
        }
    }

    /**
     * Расстояние от точки до ближайшей ГРАНИЦЫ коробки шахты (0 - внутри). Чужой мир - бесконечность.
     * ponytail: getLocation() читается из глобального потока; на Folia это гонка с потоком игрока,
     * но для выбора получателей сообщений безвредно.
     */
    private double distanceToMine(Location loc) {
        if (pos1 == null || pos2 == null || world == null || !loc.getWorld().equals(world)) return Double.MAX_VALUE;
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        double dx = Math.max(minX - loc.getX(), Math.max(0, loc.getX() - maxX));
        double dy = Math.max(minY - loc.getY(), Math.max(0, loc.getY() - maxY));
        double dz = Math.max(minZ - loc.getZ(), Math.max(0, loc.getZ() - maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private volatile boolean isResetting = false;

    public boolean isResetting() { return isResetting; }

    public boolean resetMine() {
        if (!enable) return false;
        if (pos1 == null || pos2 == null || world == null) return false;
        if (isResetting) return false;
        if (shape != MineShape.CUBOID && (weRegion == null || regionPoints.isEmpty())) return false;

        MinePreResetEvent preResetEvent = new MinePreResetEvent(this);
        Bukkit.getPluginManager().callEvent(preResetEvent);
        if (preResetEvent.isCancelled()) return false;

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());

        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        int XY1 = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        int totalCells = shape == MineShape.CUBOID ? XY1 : regionPoints.size();

        List<MineBlock> targetBlocks = getCurrentBlocks();
        boolean exact = mode == MineMode.ALL_BLOCKS;
        if (!exact && targetBlocks.isEmpty()) return false;

        isResetting = true;
        playerPlaced.clear();
        physicalBlocksCount.set(0);

        for (PeriodicAction pa : periodicActions) {
            pa.active = true;
            pa.durationLeft = pa.initialDuration;
            pa.counter = pa.interval;
        }

        java.util.function.IntFunction<Material> source = exact
                ? buildExactSource(targetBlocks, totalCells)
                : buildChanceSource(targetBlocks, totalCells);

        ResetTask task = shape == MineShape.CUBOID
                ? ResetTask.cuboid(plugin, scheduler(), world, minX, minY, minZ, maxX, maxY, maxZ, source, this::finishReset)
                : ResetTask.points(plugin, scheduler(), world, regionPoints, source, this::finishReset);
        resetTask = task;
        task.start();
        return true;
    }

    /** Режимы BLOCKS/RARITY: шанс каждого блока + точные лимиты min..max для отдельных блоков. */
    private java.util.function.IntFunction<Material> buildChanceSource(List<MineBlock> targetBlocks, int totalCells) {
        double totalChance = targetBlocks.stream().mapToDouble(MineBlock::chance).sum();
        Map<Integer, Material> limitedPlacements = new HashMap<>();
        Random rnd = new Random();

        for (MineBlock mb : targetBlocks) {
            if (mb.min() >= 0 && mb.max() >= mb.min()) {
                int count = mb.min() + (mb.max() > mb.min() ? rnd.nextInt(mb.max() - mb.min() + 1) : 0);
                for (int i = 0; i < count; i++) {
                    int attempts = 0;
                    while (attempts < 50) {
                        int rIdx = rnd.nextInt(totalCells);
                        if (!limitedPlacements.containsKey(rIdx)) {
                            limitedPlacements.put(rIdx, mb.material());
                            break;
                        }
                        attempts++;
                    }
                }
            }
        }
        return index -> {
            Material limited = limitedPlacements.get(index);
            return limited != null ? limited : getRandomBlock(targetBlocks, totalChance);
        };
    }

    /**
     * Режим ALL_BLOCKS: админ задаёт ТОЧНОЕ количество каждого блока, остаток занимает filler.
     * Хранится компактно: палитра материалов + short[] номеров палитры на каждую ячейку
     * (2 байта на ячейку, а не список объектов), затем Fisher-Yates перемешивание.
     * Если сумма больше объёма - лишнее обрезается (с конца списка) и в лог пишется предупреждение.
     */
    private java.util.function.IntFunction<Material> buildExactSource(List<MineBlock> targetBlocks, int totalCells) {
        List<Material> palette = new ArrayList<>();
        palette.add(filler);
        short[] cells = new short[totalCells];
        int next = 0;
        long wanted = 0;
        for (MineBlock mb : targetBlocks) {
            int count = exactCount(mb);
            wanted += count;
            if (count <= 0 || palette.size() >= Short.MAX_VALUE) continue;
            int fit = Math.min(count, totalCells - next);
            if (fit <= 0) continue;
            palette.add(mb.material());
            short idx = (short) (palette.size() - 1);
            java.util.Arrays.fill(cells, next, next + fit, idx);
            next += fit;
        }
        if (wanted > totalCells) {
            plugin.getLogger().warning("[LastMines] Шахта " + id + " (ALL_BLOCKS): сумма блоков " + wanted
                    + " больше объёма " + totalCells + ", лишние " + (wanted - totalCells) + " обрезаны.");
        }
        Random rnd = new Random();
        for (int i = totalCells - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            short tmp = cells[i];
            cells[i] = cells[j];
            cells[j] = tmp;
        }
        Material[] table = palette.toArray(new Material[0]);
        return index -> table[cells[index]];
    }

    private static int exactCount(MineBlock mb) {
        return Math.max(0, mb.max() >= 0 ? mb.max() : Math.max(0, mb.min()));
    }

    /** Сколько ячеек занято блоками ALL_BLOCKS (для подсказок в меню и сообщений). */
    public long getAllBlocksUsed() {
        long sum = 0;
        for (MineBlock mb : blocks) sum += exactCount(mb);
        return sum;
    }

    /** Объём области шахты в ячейках (0, если области нет). */
    public long getVolume() {
        if (pos1 == null || pos2 == null) return 0;
        if (shape != MineShape.CUBOID) return regionPoints.size();
        return (long) (Math.abs(pos1.getBlockX() - pos2.getBlockX()) + 1)
                * (Math.abs(pos1.getBlockY() - pos2.getBlockY()) + 1)
                * (Math.abs(pos1.getBlockZ() - pos2.getBlockZ()) + 1);
    }

    /** Блок поставил игрок: он не считается добычей шахты (иначе "поставил-сломал" = бесконечные дропы, деньги и статистика). */
    public void markPlaced(org.bukkit.block.Block block) {
        playerPlaced.add(blockKey(block));
    }

    /** true, если ломается блок, который поставил игрок (метка снимается). */
    public boolean consumePlaced(org.bukkit.block.Block block) {
        return playerPlaced.remove(blockKey(block));
    }

    private static long blockKey(org.bukkit.block.Block b) {
        return ((long) (b.getX() & 0x3FFFFFF) << 38) | ((long) (b.getZ() & 0x3FFFFFF) << 12) | ((b.getY() + 2048) & 0xFFFL);
    }

    /** Сброс по проценту: вызывается после каждого добытого блока. */
    public void checkPercentReset() {
        if (resetPercent <= 0 || isResetting || maxPhysicalBlocksCount <= 0) return;
        if ((long) physicalBlocksCount.get() * 100L <= (long) maxPhysicalBlocksCount * resetPercent) {
            scheduler().global(plugin, this::forceUpdate);
        }
    }

    /** Вызывается из глобального потока, когда все части сброса готовы. */
    private void finishReset(int placedBlocks) {
        physicalBlocksCount.set(placedBlocks);
        maxPhysicalBlocksCount = placedBlocks;
        isResetting = false;

        for (MineAction act : resetActions) {
            executeAction(act, null, null);
        }
        timeLeft = resetTime;
        updateHologram();

        Bukkit.getPluginManager().callEvent(new MineResetEvent(this));
    }

    public List<MineBlock> getCurrentBlocks() {
        if (mode == MineMode.BLOCKS || mode == MineMode.ALL_BLOCKS) {
            return blocks;
        } else if (mode == MineMode.RARITY && currentRarity != null) { return currentRarity.blocks(); }
        return Collections.emptyList();
    }
    
    private Material getRandomBlock(List<MineBlock> blocks, double totalChance) {
        double r = Math.random() * totalChance;
        double current = 0;
        for (MineBlock mb : blocks) {
            current += mb.chance();
            if (r <= current) {
                return mb.material();
            }
        }
        return Material.STONE;
    }

    public void createHologram() {
        if (!holoEnable || !enable) return;
        plugin.getHologramManager().create(this);
    }

    public void updateHologram() {
        if (!holoEnable || !enable) return;
        plugin.getHologramManager().update(this);
    }
    
    public void deleteHologram() {
        plugin.getHologramManager().delete(this);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public World getWorld() { return world; }
    public MineMode getMode() { return mode; }
    public int getTimeLeft() { return timeLeft; }
    public void setTimeLeft(int timeLeft) { this.timeLeft = timeLeft; }
    public Location getHoloOffset() { return holoOffset; }
    public void setHoloOffset(Location loc) { this.holoOffset = loc; }
    public List<String> getHoloTexts() { return holoTexts.stream().map(this::replacePlaceholders).toList(); }
    public List<String> getRawHoloTexts() { return holoTexts; }
    public String getHoloProvider() { return holoProvider; }
    public void setHoloProvider(String provider) { this.holoProvider = provider; }
    public void setHoloEnable(boolean b) { this.holoEnable = b; }

    public boolean isOnlineEnable() { return onlineEnable; }
    public void setOnlineEnable(boolean b) { this.onlineEnable = b; }
    public int getOnlineMin() { return onlineMin; }
    public void setOnlineMin(int min) { this.onlineMin = Math.max(0, min); }
    public int getOnlineMax() { return onlineMax; }
    public void setOnlineMax(int max) { this.onlineMax = Math.max(0, max); }

    public List<String> getRawActions() { return rawActions; }


    public int getPhysicalBlocksCount() { return physicalBlocksCount.get(); }
    public int getMaxPhysicalBlocksCount() { return maxPhysicalBlocksCount; }
    public void decrementPhysicalBlocks() { physicalBlocksCount.updateAndGet(v -> v > 0 ? v - 1 : 0); }

    public void setHoloTexts(List<String> list) { this.holoTexts = list; }

    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    
    public Location getTpPos() { return tpPos; }
    public void setTpPos(Location loc) { this.tpPos = loc; }
    public boolean isTpEnable() { return tpEnable; }
    public void setTpEnable(boolean b) { this.tpEnable = b; }
    public int getResetTime() { return resetTime; }
    public void setResetTime(int seconds) {
        this.resetTime = Math.max(1, seconds);
        if (timeLeft > this.resetTime) timeLeft = this.resetTime;
    }
    public int getResetPercent() { return resetPercent; }
    public void setResetPercent(int percent) { this.resetPercent = Math.max(0, Math.min(99, percent)); }
    public Material getFiller() { return filler; }
    public void setFiller(Material filler) { this.filler = filler; }
    public boolean isStopped() { return stopped; }
    public void setStopped(boolean stopped) { this.stopped = stopped; }

    public boolean isEnable() { return enable; }
    public void setEnable(boolean enable) { this.enable = enable; }

    public MineShape getShape() { return shape; }

    public List<MineRarity> getRarities() { return rarities; }

    public boolean setNextRarity(String rarityId) {
        if (mode != MineMode.RARITY) return false;
        for (MineRarity r : rarities) {
            if (r.id().equalsIgnoreCase(rarityId)) {
                this.nextRarity = r;
                return true;
            }
        }
        return false;
    }

    public boolean isHoloEnable() { return holoEnable; }
    public YamlMap getHoloMap() { return holoMap; }
    public boolean isPermEnable() { return permEnable; }
    public void setPermEnable(boolean b) { this.permEnable = b; }
    public String getPermValue() { return permValue; }
    public void setPermValue(String s) { this.permValue = s; }
    public List<String> getPermMessages() { return permMessages; }
    public void setPermMessages(List<String> list) { this.permMessages = list; }

    public boolean isEnchantEnable() { return enchantEnable; }
    public void setEnchantEnable(boolean b) { this.enchantEnable = b; }
    public Map<String, Integer> getRequiredEnchants() { return requiredEnchants; }
    public List<String> getEnchantFailActions() { return enchantFailActions; }

    @SuppressWarnings("deprecation")
    public boolean meetsEnchantRequirements(ItemStack tool) {
        if (!enchantEnable || requiredEnchants.isEmpty()) return true;
        for (Map.Entry<String, Integer> entry : requiredEnchants.entrySet()) {
            Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(entry.getKey()));
            if (ench == null) continue;
            int level = tool != null ? tool.getEnchantmentLevel(ench) : 0;
            if (level < entry.getValue()) return false;
        }
        return true;
    }

    public boolean isInMine(Location loc) {
        if (loc.getWorld() != world) return false;
        if (pos1 == null || pos2 == null) return false;
        if (shape != MineShape.CUBOID) {
            return weRegion != null && WEHook.contains(weRegion, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        return loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
               loc.getBlockY() >= minY && loc.getBlockY() <= maxY &&
               loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ;
    }
}

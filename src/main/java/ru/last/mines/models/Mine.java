package ru.last.mines.models;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;
import ru.last.mines.LastMines;
import ru.last.mines.api.events.*;
import ru.last.mines.utils.Sounds;
import ru.last.mines.utils.time.TimeUtils;
import ru.last.mines.utils.ColorUtils;
import ru.last.mines.utils.time.TimeFormatter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Mine {
    private final LastMines plugin;
    private final YamlMap sourceMap;
    private final String id;
    private final String name;
    private final World world;
    
    private final MineMode mode;
    
    private final Location pos1;
    private final Location pos2;
    
    private final List<MineBlock> blocks = new ArrayList<>();
    private final List<MineRarity> rarities = new ArrayList<>();

    private final Map<Integer, List<String>> updateActions = new HashMap<>();
    private final List<String> resetActions = new ArrayList<>();
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
    private BukkitTask task;
    private int timeLeft;
    
    private MineRarity currentRarity;
    private MineRarity nextRarity;
    
    private int physicalBlocksCount = 0;
    private int maxPhysicalBlocksCount = 0;

    private static class PeriodicAction {
        int interval;
        boolean repeat;
        int initialDuration;
        
        int durationLeft;
        String action;
        int counter;
        boolean active;
    }
    private final List<PeriodicAction> periodicActions = new ArrayList<>();

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
        
        YamlMap posMap = map.get("positions").asYamlMap().getOrThrow();
        this.pos1 = parseLocation(world, posMap.get("one").asString("0;100;0"));
        this.pos2 = parseLocation(world, posMap.get("two").asString("5;105;5"));
        
        if (this.mode == MineMode.BLOCKS) {
            for (Object blockValRaw : getListOrEmpty(map.get("blocks"))) {
                YamlMap blockMap = YamlValue.wrap(blockValRaw).asYamlMap().getOrThrow();
                blocks.add(parseBlock(blockMap));
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
                    rBlocks.add(parseBlock(blockMap));
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

        startTasks();
        createHologram();
        resetMine();
    }

    /**
     * Репарсер всех старых экшенов в новые
     */
    public void reparseActions(List<String> raw) {
        this.rawActions = new ArrayList<>(raw);
        periodicActions.clear();
        updateActions.clear();
        resetActions.clear();

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
                if (idx != -1) {
                    pa.action = act.substring(0, idx) + act.substring(idx + toRemove.length());
                } else {
                    pa.action = act;
                }
                periodicActions.add(pa);
                continue;
            }

            if (act.contains("[update:")) {
                try {
                    String timeStr = act.substring(act.indexOf("[update:") + 8, act.indexOf("]"));
                    int time = TimeUtils.parseToSeconds(timeStr);
                    updateActions.computeIfAbsent(time, k -> new ArrayList<>()).add(act);
                } catch (Exception ignored) {}
            } else if (act.contains("[update]")) { resetActions.add(act); }
        }
    }

    private MineBlock parseBlock(YamlMap blockMap) {
        String blockId = blockMap.get("id").asString("minecraft:stone").replace("minecraft:", "").toUpperCase();
        double chance = blockMap.has("chance") ? blockMap.get("chance").asDouble(100.0) : 0.0;
        int min = -1, max = -1;
        
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
        return new MineBlock(Material.matchMaterial(blockId), chance, min, max, drops);
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

    public void save() {
        if (mode == MineMode.BLOCKS) {
            List<Map<String, Object>> blockList = new ArrayList<>();
            for (MineBlock mb : blocks) blockList.add(serializeBlock(mb));
            sourceMap.set("blocks", blockList);
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
            sourceMap.set("rarity", rarityList);
        }

        sourceMap.set("hologram.texts", holoTexts);
        sourceMap.set("hologram.enable", holoEnable);
        sourceMap.set("hologram.provider", holoProvider);
        if (holoOffset != null) {
            sourceMap.set("hologram.offset", holoOffset.getX() + ";" + holoOffset.getY() + ";" + holoOffset.getZ());
        }
        sourceMap.set("permissions.enable", permEnable);
        sourceMap.set("permissions.value", permValue);
        sourceMap.set("permissions.messages", permMessages);
        sourceMap.set("reset_time", resetTime);
        sourceMap.set("stopped", stopped);
        sourceMap.set("enchant_requirements.enable", enchantEnable);
        sourceMap.set("enchant_requirements.enchantments", new LinkedHashMap<>(requiredEnchants));
        sourceMap.set("online.enable", onlineEnable);
        sourceMap.set("online.min", onlineMin);
        sourceMap.set("online.max", onlineMax);
        sourceMap.set("teleport.enable", tpEnable);
        if (tpPos != null) {
            sourceMap.set("teleport.pos", tpPos.getX() + ";" + tpPos.getY() + ";" + tpPos.getZ() + ";" + tpPos.getYaw() + ";" + tpPos.getPitch());
        }
        sourceMap.set("actions", rawActions);

        File file = new File(plugin.getDataFolder(), "mines" + File.separator + id + ".yml");
        try {
            Files.writeString(file.toPath(), sourceMap.saveToString(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            plugin.getDebugger().error("Не удалось сохранить шахту " + id, e);
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
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private BukkitTask resetTask;

    public void stopTasks() {
        if (task != null) task.cancel();
        if (resetTask != null) resetTask.cancel();
    }
    
    private void tick() {
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
                executeAction(pa.action);

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
            for (String act : updateActions.get(timeLeft)) {
                executeAction(act);
            }
        }
        
        if (timeLeft <= 0) {
            forceUpdate();
        }
        
        updateHologram();
    }
    
    public void forceUpdate() {
        if (tpEnable && tpPos != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (isInMine(p.getLocation())) {
                    p.teleport(tpPos);
                }
            }
        }
        if (mode == MineMode.RARITY) {
            MineRarity oldRarity = currentRarity;
            currentRarity = nextRarity;
            nextRarity = rollRarity();
            Bukkit.getPluginManager().callEvent(new MineUpdateRarityEvent(this, oldRarity, currentRarity));
        }
        resetMine();
        for (String act : resetActions) {
            executeAction(act);
        }
        timeLeft = resetTime;
        
        updateHologram();
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
        str = str.replace("{BLOCKS_LEFT}", String.valueOf(physicalBlocksCount));
        str = str.replace("{BLOCKS_MAX}", String.valueOf(maxPhysicalBlocksCount));
        
        str = str.replace("%time%", TimeFormatter.format(timeLeft * 1000L, "default"));
        str = str.replace("%time_detail%", TimeFormatter.format(timeLeft * 1000L, "detail"));
        str = str.replace("%time:detail%", TimeFormatter.format(timeLeft * 1000L, "detail"));
        str = str.replace("%time_clock%", TimeFormatter.format(timeLeft * 1000L, "clock"));
        str = str.replace("%time:clock%", TimeFormatter.format(timeLeft * 1000L, "clock"));
        
        return str;
    }

    @SuppressWarnings("deprecation")
    private void executeAction(String action) {
        action = replacePlaceholders(action);
        
        int radius = -2;
        if (action.contains("[radius:")) {
            try {
                int start = action.indexOf("[radius:");
                int end = action.indexOf("]", start);
                String radStr = action.substring(start + 8, end);
                radius = Integer.parseInt(radStr);
                action = action.replace("[radius:" + radStr + "]", "").trim();
            } catch (Exception ignored) {}
        }
        
        final int finalRadius = radius;
        Collection<? extends Player> targetPlayers = Bukkit.getOnlinePlayers();
        if (finalRadius > -2 && pos1 != null && pos2 != null) {
            int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
            int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
            int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
            int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
            int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
            int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

            targetPlayers = targetPlayers.stream().filter(p -> {
                if (!p.getWorld().equals(world)) return false;
                Location loc = p.getLocation();
                double dx = Math.max(minX - loc.getX(), Math.max(0, loc.getX() - maxX));
                double dy = Math.max(minY - loc.getY(), Math.max(0, loc.getY() - maxY));
                double dz = Math.max(minZ - loc.getZ(), Math.max(0, loc.getZ() - maxZ));
                double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
                
                if (finalRadius == -1) {
                    return distance == 0; // AABB
                }
                
                return distance <= finalRadius;
            }).toList();
        }

        String cleanAction = action.replaceAll("\\[update(:[^]]+)?]\\s*", "");
        if (cleanAction.startsWith("[message]")) {
            String msg = cleanAction.replaceFirst("\\[message]\\s*", "");
            targetPlayers.forEach(p -> {
                String parsedMsg = msg;
                if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    parsedMsg = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, parsedMsg);
                }
                p.sendMessage(ColorUtils.colorString(parsedMsg));
            });
        } else if (cleanAction.startsWith("[title]")) {
            String raw = cleanAction.replaceFirst("\\[title]\\s*", "");
            int startQuote = raw.indexOf('"');
            int endQuote = raw.lastIndexOf('"');
            String titleStr = raw;
            String times = "20;40;20";
            if (startQuote != -1 && endQuote != -1 && startQuote != endQuote) {
                titleStr = raw.substring(startQuote + 1, endQuote);
                times = raw.substring(endQuote + 1).trim();
                if (times.isEmpty()) times = "20;40;20";
            }
            int fadeIn = 20, stay = 40, fadeOut = 20;
            try {
                String[] timeSplit = times.split(";");
                if (timeSplit.length >= 3) {
                    fadeIn = Integer.parseInt(timeSplit[0]);
                    stay = Integer.parseInt(timeSplit[1]);
                    fadeOut = Integer.parseInt(timeSplit[2]);
                }
            } catch (Exception ignored) {}
            
            final String rawTitleStr = titleStr;
            final int finalFadeIn = fadeIn;
            final int finalStay = stay;
            final int finalFadeOut = fadeOut;
            
            targetPlayers.forEach(p -> {
                String pTitleStr = rawTitleStr;
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    pTitleStr = PlaceholderAPI.setPlaceholders(p, pTitleStr);
                }
                pTitleStr = ColorUtils.colorString(pTitleStr);

                String title = pTitleStr;
                String subtitle = "";
                if (pTitleStr.contains("\n")) {
                    String[] split = pTitleStr.split("\n");
                    title = split[0];
                    subtitle = split.length > 1 ? split[1] : "";
                }
                p.sendTitle(title, subtitle, finalFadeIn, finalStay, finalFadeOut);
            });
        } else if (cleanAction.startsWith("[actionbar]")) {
            String msg = cleanAction.replaceFirst("\\[actionbar]\\s*", "").replace("&", "§");
            targetPlayers.forEach(p -> {
                String parsedMsg = msg;
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    parsedMsg = PlaceholderAPI.setPlaceholders(p, parsedMsg);
                }
                String finalMsg = ColorUtils.colorString(parsedMsg);
                p.sendActionBar(ColorUtils.colorString(finalMsg));
            });
        } else if (cleanAction.startsWith("[sound]")) {
            String soundStr = cleanAction.replaceFirst("\\[sound]\\s*", "");
            try {
                String[] split = soundStr.split(" ");
                Sound sound = Sounds.parseSound(split[0]);
                if (sound != null) {
                    float volume = split.length > 1 ? Float.parseFloat(split[1]) : 1f;
                    float pitch = split.length > 2 ? Float.parseFloat(split[2]) : 1f;
                    targetPlayers.forEach(p -> p.playSound(p.getLocation(), sound, volume, pitch));
                }
            } catch (Exception ignored) {}
        }
    }
    
    private boolean isResetting = false;

    public void resetMine() {
        if (pos1 == null || pos2 == null || world == null) return;
        if (isResetting) return;
        
        MinePreResetEvent preResetEvent = new MinePreResetEvent(this);
        Bukkit.getPluginManager().callEvent(preResetEvent);
        if (preResetEvent.isCancelled()) return;

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        int XY1 = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);

        List<MineBlock> targetBlocks = getCurrentBlocks();
        if (targetBlocks.isEmpty()) return;
        
        double totalChance = targetBlocks.stream().mapToDouble(MineBlock::chance).sum();
        
        isResetting = true;
        physicalBlocksCount = 0;
        
        for (PeriodicAction pa : periodicActions) {
            pa.active = true;
            pa.durationLeft = pa.initialDuration;
            pa.counter = pa.interval;
        }
        
        Map<Integer, Material> limitedPlacements = new HashMap<>();
        Random rnd = new Random();
        
        for (MineBlock mb : targetBlocks) {
            if (mb.min() >= 0 && mb.max() >= mb.min()) {
                int count = mb.min() + (mb.max() > mb.min() ? rnd.nextInt(mb.max() - mb.min() + 1) : 0);
                for (int i = 0; i < count; i++) {
                    int attempts = 0;
                    while (attempts < 50) {
                        int rIdx = rnd.nextInt(XY1);
                        if (!limitedPlacements.containsKey(rIdx)) {
                            limitedPlacements.put(rIdx, mb.material());
                            break;
                        }
                        attempts++;
                    }
                }
            }
        }

        resetTask = new BukkitRunnable() {
            int x = minX;
            int y = minY;
            int z = minZ;
            int currentIndex = 0;

            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                int blocksSet = 0;
                
                while (x <= maxX) {
                    while (y <= maxY) {
                        while (z <= maxZ) {
                            Material mat;
                            if (limitedPlacements.containsKey(currentIndex)) {
                                mat = limitedPlacements.get(currentIndex);
                            } else {
                                mat = getRandomBlock(targetBlocks, totalChance);
                            }
                            if (mat != null) {
                                Block b = world.getBlockAt(x, y, z);
                                if (b.getType() != mat) {
                                    b.setType(mat, false);
                                }
                                if (mat != Material.AIR) {
                                    physicalBlocksCount++;
                                }
                            }
                            z++;
                            currentIndex++;
                            blocksSet++;

                            if (blocksSet > 10000 || (System.currentTimeMillis() - startTime) > 10) {
                                return;
                            }
                        }
                        z = minZ;
                        y++;
                    }
                    y = minY;
                    x++;
                }

                maxPhysicalBlocksCount = physicalBlocksCount;
                isResetting = false;

                Bukkit.getPluginManager().callEvent(new MineResetEvent(Mine.this));
                
                this.cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    public List<MineBlock> getCurrentBlocks() {
        if (mode == MineMode.BLOCKS) {
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
        if (!holoEnable) return;
        plugin.getHologramManager().create(this);
    }
    
    public void updateHologram() {
        if (!holoEnable) return;
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


    public int getPhysicalBlocksCount() { return physicalBlocksCount; }
    public int getMaxPhysicalBlocksCount() { return maxPhysicalBlocksCount; }
    public void decrementPhysicalBlocks() { if (physicalBlocksCount > 0) physicalBlocksCount--; }

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
    public boolean isStopped() { return stopped; }
    public void setStopped(boolean stopped) { this.stopped = stopped; }

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

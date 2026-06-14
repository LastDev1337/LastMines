package ru.last.mines.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;
import ru.last.mines.LastMines;
import ru.last.mines.api.events.*;

import java.util.*;

public class Mine {
    private final LastMines plugin;
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
    
    private final boolean permEnable;
    private final String permValue;
    private final List<String> permMessages;
    
    private final boolean onlineEnable;
    private final int onlineMin;
    private final int onlineMax;
    
    private final boolean tpEnable;
    private final Location tpPos;

    private final boolean holoEnable;
    private final String holoProvider;
    private final Location holoOffset;
    private final List<String> holoTexts;
    private final YamlMap holoMap;

    private final int resetTime;
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
                
                List<MineBlock> rBlocks = new ArrayList<>();
                for (Object blockValRaw : getListOrEmpty(rarityMap.get("blocks"))) {
                    YamlMap blockMap = YamlValue.wrap(blockValRaw).asYamlMap().getOrThrow();
                    rBlocks.add(parseBlock(blockMap));
                }
                rarities.add(new MineRarity(rId, rChance, rName, rBlocks));
            }
            if (!rarities.isEmpty()) {
                this.currentRarity = rollRarity();
                this.nextRarity = rollRarity();
            }
        }
        
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?i)^(?:\\[update[^\\]]*\\]\\s*)?(\\[actionbar\\]|\\[message\\]|\\[title\\]|\\[sound\\])\\s*(?:\\[radius:-?\\d+\\]\\s*)?(\\d+)\\s+(true|false)\\s+(.*)");

        for (Object actionValRaw : getListOrEmpty(map.get("actions"))) {
            String act = YamlValue.wrap(actionValRaw).asString("");
            
            java.util.regex.Matcher m = p.matcher(act);
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
                    int time = ru.last.mines.utils.TimeUtils.parseToSeconds(timeStr);
                    updateActions.computeIfAbsent(time, k -> new ArrayList<>()).add(act);
                } catch (Exception ignored) {}
            } else if (act.contains("[update]")) { resetActions.add(act); }
        }
        
        YamlMap permMap = map.get("permissions").asYamlMap().orDefault(new YamlMap());
        this.permEnable = permMap.get("enable").asBool(false);
        this.permValue = permMap.get("value").asString("");
        this.permMessages = getListOrEmpty(permMap.get("messages")).stream().map(v -> YamlValue.wrap(v).asString("")).toList();
        
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
        this.holoOffset = parseLocation(world, holoMap.get("offset").asString("0;0;0"));
        this.holoTexts = getListOrEmpty(holoMap.get("texts")).stream().map(v -> YamlValue.wrap(v).asString("")).toList();

        this.resetTime = ru.last.mines.utils.TimeUtils.parseToSeconds(map.get("reset_time").asString("5m"));
        this.timeLeft = this.resetTime;

        startTasks();
        createHologram();
        resetMine();
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

        List<String> drops = getListOrEmpty(blockMap.get("drop")).stream()
                .map(v -> YamlValue.wrap(v).asString("").replace("minecraft:", "").toUpperCase())
                .toList();
        return new MineBlock(Material.matchMaterial(blockId), chance, min, max, drops);
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
        return rarities.get(0);
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
            return new Location(w, Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]));
        } catch (Exception e) { return new Location(w, 0, 100, 0); }
    }

    public void startTasks() {
        if (task != null) task.cancel();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private org.bukkit.scheduler.BukkitTask resetTask;

    public void stopTasks() {
        if (task != null) task.cancel();
        if (resetTask != null) resetTask.cancel();
    }
    
    private void tick() {
        if (onlineEnable) {
            int online = Bukkit.getOnlinePlayers().size();
            if (online < onlineMin || online > onlineMax) return;
        }

        for (int i = 0; i < periodicActions.size(); i++) {
            PeriodicAction pa = periodicActions.get(i);
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
        
        str = str.replace("%time%", ru.last.mines.utils.TimeFormatter.format(timeLeft * 1000L, "default"));
        str = str.replace("%time_detail%", ru.last.mines.utils.TimeFormatter.format(timeLeft * 1000L, "detail"));
        str = str.replace("%time:detail%", ru.last.mines.utils.TimeFormatter.format(timeLeft * 1000L, "detail"));
        str = str.replace("%time_clock%", ru.last.mines.utils.TimeFormatter.format(timeLeft * 1000L, "clock"));
        str = str.replace("%time:clock%", ru.last.mines.utils.TimeFormatter.format(timeLeft * 1000L, "clock"));
        
        return str;
    }

    @SuppressWarnings("deprecation")
    private void executeAction(String action) {
        action = replacePlaceholders(action);
        
        int radius = -2; // Default is -2 (no radius specified)
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
                    return distance == 0; // Exactly inside the AABB
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
                p.sendMessage(ru.last.mines.utils.ColorUtils.colorString(parsedMsg));
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
                if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    pTitleStr = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, pTitleStr);
                }
                pTitleStr = ru.last.mines.utils.ColorUtils.colorString(pTitleStr);
                
                String title = pTitleStr;
                String subtitle = "";
                if (pTitleStr.contains("\\n")) {
                    String[] split = pTitleStr.split("\\\\n");
                    title = split[0];
                    subtitle = split.length > 1 ? split[1] : "";
                } else if (pTitleStr.contains("\n")) {
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
                if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    parsedMsg = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, parsedMsg);
                }
                String finalMsg = ru.last.mines.utils.ColorUtils.colorString(parsedMsg);
                p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(finalMsg));
            });
        } else if (cleanAction.startsWith("[sound]")) {
            String soundStr = cleanAction.replaceFirst("\\[sound]\\s*", "");
            try {
                String[] split = soundStr.split(" ");
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(split[0].toUpperCase());
                float volume = split.length > 1 ? Float.parseFloat(split[1]) : 1f;
                float pitch = split.length > 2 ? Float.parseFloat(split[2]) : 1f;
                targetPlayers.forEach(p -> p.playSound(p.getLocation(), sound, volume, pitch));
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
        
        maxPhysicalBlocksCount = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);

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
        
        java.util.Map<Integer, Material> limitedPlacements = new java.util.HashMap<>();
        int volume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        java.util.Random rnd = new java.util.Random();
        
        for (MineBlock mb : targetBlocks) {
            if (mb.min() >= 0 && mb.max() >= mb.min()) {
                int count = mb.min() + (mb.max() > mb.min() ? rnd.nextInt(mb.max() - mb.min() + 1) : 0);
                for (int i = 0; i < count; i++) {
                    int attempts = 0;
                    while (attempts < 50) {
                        int rIdx = rnd.nextInt(volume);
                        if (!limitedPlacements.containsKey(rIdx)) {
                            limitedPlacements.put(rIdx, mb.material());
                            break;
                        }
                        attempts++;
                    }
                }
            }
        }

        resetTask = new org.bukkit.scheduler.BukkitRunnable() {
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
                                    if (mat != Material.AIR) {
                                        physicalBlocksCount++;
                                    }
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
        if (!holoEnable) return;
        plugin.getHologramManager().delete(this);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getTimeLeft() { return timeLeft; }
    public void setTimeLeft(int timeLeft) { this.timeLeft = timeLeft; }
    public Location getHoloOffset() { return holoOffset; }
    public List<String> getHoloTexts() { return holoTexts.stream().map(this::replacePlaceholders).toList(); }
    public String getHoloProvider() { return holoProvider; }
    
    public int getPhysicalBlocksCount() { return physicalBlocksCount; }
    public int getMaxPhysicalBlocksCount() { return maxPhysicalBlocksCount; }
    public void decrementPhysicalBlocks() { if (physicalBlocksCount > 0) physicalBlocksCount--; }
    public void setPhysicalBlocksCount(int count) { this.physicalBlocksCount = count; }

    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    
    public Location getTpPos() { return tpPos; }
    public boolean isTpEnable() { return tpEnable; }
    public int getResetTime() { return resetTime; }
    
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

    public boolean isHoloEnable() { return !holoEnable; }
    public YamlMap getHoloMap() { return holoMap; }
    public boolean isPermEnable() { return permEnable; }
    public String getPermValue() { return permValue; }
    public List<String> getPermMessages() { return permMessages; }

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

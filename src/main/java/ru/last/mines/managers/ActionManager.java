package ru.last.mines.managers;

import dev.by1337.yaml.YamlMap;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import org.bukkit.Material;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.last.mines.api.*;
import ru.last.mines.api.events.*;
import ru.last.mines.gui.ActionsRegister;
import ru.last.mines.models.*;
import ru.last.mines.LastMines;
import ru.last.mines.utils.ColorUtils;

public class ActionManager {

    private final LastMines plugin;

    public ActionManager(LastMines plugin) { this.plugin = plugin; }

    public void registerBMenuActions() {
        if (Bukkit.getPluginManager().getPlugin("BMenu") != null) {
            ActionsRegister.register(plugin, this);
        }
    }

    public void openGui(Player player, String mineId) { LastMinesProvider.getApi().openGui(player, mineId); }

    public void resetMine(String mineId) { LastMinesProvider.getApi().resetMine(mineId); }

    public void teleportToMine(Player player, String mineId) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine != null) {
            if (!mine.isTpEnable() || mine.getTpPos() == null) {
                player.sendMessage(ColorUtils.colorString("Телепортация на эту автошахту отключена или позиция не установлена!"));
                return;
            }
            MineTeleportEvent apiEvent = new MineTeleportEvent(mine, player);
            Bukkit.getPluginManager().callEvent(apiEvent);
            if (apiEvent.isCancelled()) return;

            player.teleport(mine.getTpPos());
            plugin.getConfigManager().getMessages().getTeleported().send(player, "%mine%", mineId);
        }
    }

    public void deleteMine(String mineId) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine != null) {
            MineDeleteEvent apiEvent = new MineDeleteEvent(mine);
            Bukkit.getPluginManager().callEvent(apiEvent);
            if (apiEvent.isCancelled()) return;

            plugin.getMineManager().getMines().remove(mineId);
            File file = new File(plugin.getDataFolder() + "/mines", mineId + ".yml");
            if (file.exists()) file.delete();
            mine.stopTasks();
            mine.deleteHologram();
        }
    }

    public void updateMine(String mineId, String[] opts) {
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "lastmines update " + mineId + " " + String.join(" ", opts));
    }

    public void editBlock(String mineId, String blockMat, String action, String value) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        Material mat = Material.matchMaterial(blockMat);
        if (mat == null) return;
        
        List<MineBlock> blocks = mine.getCurrentBlocks();
        MineBlock target = null;
        for (MineBlock mb : blocks) {
            if (mb.material() == mat) {
                target = mb;
                break;
            }
        }
        
        if ("chance".equalsIgnoreCase(action)) {
            double change = 0;
            try { change = Double.parseDouble(value); } catch(Exception ignored) {}
            if (target != null) {
                double newChance = Math.max(0, target.chance() + change);
                MineBlock newBlock = new MineBlock(target.material(), newChance, target.min(), target.max(), target.drops());
                blocks.remove(target);
                blocks.add(newBlock);
            } else if (change > 0) {
                blocks.add(new MineBlock(mat, change, 0, 0, Collections.emptyList()));
            }
        } else if ("remove".equalsIgnoreCase(action) && target != null) {
            blocks.remove(target);
        }
        mine.save();
    }

    public void editRarity(String mineId, String rarityId, String action, String value) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null || mine.getRarities() == null) return;
        
        List<MineRarity> rarities = mine.getRarities();
        MineRarity target = null;
        for (MineRarity mr : rarities) {
            if (mr.id().equalsIgnoreCase(rarityId)) {
                target = mr;
                break;
            }
        }
        
        if ("chance".equalsIgnoreCase(action) && target != null) {
            double change = 0;
            try { change = Double.parseDouble(value); } catch(Exception ignored) {}
            double newChance = Math.max(0, target.chance() + change);
            MineRarity newRarity = new MineRarity(target.id(), newChance, target.name(), target.blocks(), target.icon());
            rarities.remove(target);
            rarities.add(newRarity);
        } else if ("remove".equalsIgnoreCase(action) && target != null) {
            rarities.remove(target);
        }
        mine.save();
    }

    public void editHolo(String mineId, int index, String action, String value) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        
        List<String> current = mine.getRawHoloTexts();
        if (current == null) return;
        
        List<String> lines = new ArrayList<>(current);
        if (index < 0 || index >= lines.size()) return;
        
        if ("remove".equalsIgnoreCase(action)) {
            lines.remove(index);
        } else if ("set".equalsIgnoreCase(action)) {
            lines.set(index, value);
        }
        mine.setHoloTexts(lines);
        mine.save();
        mine.deleteHologram();
        mine.createHologram();
    }

    public void addHoloLine(String mineId, String line) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;

        List<String> current = mine.getRawHoloTexts();
        List<String> lines = new ArrayList<>(current == null ? List.of() : current);
        lines.add(line);
        mine.setHoloTexts(lines);
        mine.save();
        mine.deleteHologram();
        mine.createHologram();
    }

    public void toggleStop(String mineId) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        mine.setStopped(!mine.isStopped());
        mine.save();
    }

    public void editResetTime(String mineId, String deltaStr) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        int delta;
        try { delta = Integer.parseInt(deltaStr); } catch (Exception e) { return; }
        mine.setResetTime(mine.getResetTime() + delta);
        mine.save();
    }

    public void toggleEnchantRequirement(String mineId) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        mine.setEnchantEnable(!mine.isEnchantEnable());
        mine.save();
    }

    public void editEnchant(String mineId, String enchantKey, String deltaStr) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        int delta;
        try { delta = Integer.parseInt(deltaStr); } catch (Exception e) { return; }

        String key = enchantKey.toLowerCase();
        java.util.Map<String, Integer> enchants = mine.getRequiredEnchants();
        int updated = Math.max(0, enchants.getOrDefault(key, 0) + delta);
        if (updated == 0) {
            enchants.remove(key);
        } else {
            enchants.put(key, updated);
        }
        mine.save();
    }

    public void setBlockDrops(String mineId, String blockMat, List<DropItem> drops) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        Material mat = Material.matchMaterial(blockMat);
        if (mat == null) return;

        List<MineBlock> blocks = mine.getCurrentBlocks();
        for (MineBlock mb : blocks) {
            if (mb.material() == mat) {
                MineBlock updated = new MineBlock(mb.material(), mb.chance(), mb.min(), mb.max(), drops);
                blocks.remove(mb);
                blocks.add(updated);
                break;
            }
        }
        mine.save();
    }

    public void editDropItem(String mineId, String blockMat, String dropMat, String action, String value) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        Material mat = Material.matchMaterial(blockMat);
        Material dMat = Material.matchMaterial(dropMat);
        if (mat == null || dMat == null) return;

        List<MineBlock> blocks = mine.getCurrentBlocks();
        for (MineBlock mb : blocks) {
            if (mb.material() != mat) continue;

            List<DropItem> drops = new ArrayList<>(mb.drops());
            DropItem target = null;
            for (DropItem d : drops) {
                if (d.material() == dMat) {
                    target = d;
                    break;
                }
            }
            if (target == null) return;

            if ("remove".equalsIgnoreCase(action)) {
                drops.remove(target);
            } else if ("chance".equalsIgnoreCase(action)) {
                double delta = 0;
                try { delta = Double.parseDouble(value); } catch (Exception ignored) {}
                double newChance = Math.clamp(target.chance() + delta, 0, 100);
                drops.remove(target);
                drops.add(new DropItem(target.material(), newChance, target.fortune()));
            } else if ("fortune".equalsIgnoreCase(action)) {
                drops.remove(target);
                drops.add(new DropItem(target.material(), target.chance(), !target.fortune()));
            }

            MineBlock updated = new MineBlock(mb.material(), mb.chance(), mb.min(), mb.max(), drops);
            blocks.remove(mb);
            blocks.add(updated);
            break;
        }
        mine.save();
    }

    public void editPerm(String mineId, String action, String value) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;

        if ("enable".equalsIgnoreCase(action)) {
            mine.setPermEnable(Boolean.parseBoolean(value));
        } else if ("value".equalsIgnoreCase(action)) {
            mine.setPermValue(value);
        }
        mine.save();
    }

    public void addPermMessage(String mineId, String actionStr) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        List<String> messages = new ArrayList<>(mine.getPermMessages());
        messages.add(actionStr);
        mine.setPermMessages(messages);
        mine.save();
    }

    public void removePermMessage(String mineId, int index) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        List<String> messages = new ArrayList<>(mine.getPermMessages());
        if (index < 0 || index >= messages.size()) return;
        messages.remove(index);
        mine.setPermMessages(messages);
        mine.save();
    }

    public void addRarity(String mineId, String rarityId) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        for (MineRarity r : mine.getRarities()) {
            if (r.id().equalsIgnoreCase(rarityId)) return;
        }
        mine.getRarities().add(new MineRarity(rarityId, 10.0, rarityId, new ArrayList<>(), null));
        mine.save();
    }

    public void addAction(String mineId, String actionStr) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        List<String> actions = new ArrayList<>(mine.getRawActions());
        actions.add(actionStr);
        mine.reparseActions(actions);
        mine.save();
    }

    public void removeAction(String mineId, int index) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        List<String> actions = new ArrayList<>(mine.getRawActions());
        if (index < 0 || index >= actions.size()) return;
        actions.remove(index);
        mine.reparseActions(actions);
        mine.save();
    }

    public void toggleOnlineEnable(String mineId) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        mine.setOnlineEnable(!mine.isOnlineEnable());
        mine.save();
    }

    public void editOnlineMin(String mineId, String deltaStr) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        try { mine.setOnlineMin(mine.getOnlineMin() + Integer.parseInt(deltaStr)); } catch (Exception ignored) { return; }
        mine.save();
    }

    public void editOnlineMax(String mineId, String deltaStr) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        try { mine.setOnlineMax(mine.getOnlineMax() + Integer.parseInt(deltaStr)); } catch (Exception ignored) { return; }
        mine.save();
    }

    public void toggleTeleportEnable(String mineId) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        mine.setTpEnable(!mine.isTpEnable());
        mine.save();
    }

    public void setTeleportToPlayer(String mineId, Player player) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        if (mine.getWorld() != null && !mine.getWorld().equals(player.getWorld())) {
            player.sendMessage(ColorUtils.colorString("&cШахта " + mineId + " находится в мире " + mine.getWorld().getName() + ", а вы в другом мире!"));
            return;
        }
        mine.setTpPos(player.getLocation().clone());
        mine.save();
        player.sendMessage(ColorUtils.colorString("&aТочка телепортации шахты " + mineId + " установлена на вашу позицию."));
    }

    public void toggleHoloEnable(String mineId) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        boolean newState = !mine.isHoloEnable();
        mine.setHoloEnable(newState);
        mine.save();
        if (newState) mine.createHologram(); else mine.deleteHologram();
    }

    private static final List<String> HOLO_PROVIDERS = List.of("Vanilla", "DecentHolograms", "FancyHolograms");

    public void cycleHoloProvider(String mineId) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        int idx = HOLO_PROVIDERS.stream().map(String::toLowerCase).toList().indexOf(mine.getHoloProvider().toLowerCase());
        String next = HOLO_PROVIDERS.get((idx + 1) % HOLO_PROVIDERS.size());
        mine.deleteHologram();
        mine.setHoloProvider(next);
        mine.save();
        mine.createHologram();
    }

    public void setHoloOffsetToPlayer(String mineId, Player player) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        if (mine.getWorld() != null && !mine.getWorld().equals(player.getWorld())) {
            player.sendMessage(ColorUtils.colorString("&cШахта " + mineId + " находится в мире " + mine.getWorld().getName() + ", а вы в другом мире!"));
            return;
        }
        mine.deleteHologram();
        mine.setHoloOffset(player.getLocation().clone());
        mine.save();
        mine.createHologram();
        player.sendMessage(ColorUtils.colorString("&aПозиция голограммы шахты " + mineId + " установлена на вашу позицию."));
    }

    public void switchMineMode(String mineId, String newModeStr) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        MineMode target;
        try { target = MineMode.valueOf(newModeStr.toUpperCase()); } catch (Exception e) { return; }
        if (mine.getMode() == target) return;

        mine.save();

        File file = new File(plugin.getDataFolder(), "mines" + File.separator + mineId + ".yml");
        try {
            YamlMap map = YamlMap.load(file);
            if (target == MineMode.RARITY) {
                Object blocksRaw = map.getRaw("blocks");
                List<Object> blocksList = blocksRaw instanceof List<?> l ? new ArrayList<>(l) : new ArrayList<>();
                Map<String, Object> rarityEntry = new LinkedHashMap<>();
                rarityEntry.put("id", "default");
                rarityEntry.put("chance", 100.0);
                rarityEntry.put("name", "&fОбычная");
                rarityEntry.put("blocks", blocksList);
                map.set("rarity", List.of(rarityEntry));
                map.set("blocks", null);
            } else {
                Object rarityRaw = map.getRaw("rarity");
                List<Object> blocksList = new ArrayList<>();
                if (rarityRaw instanceof List<?> l && !l.isEmpty() && l.getFirst() instanceof Map<?, ?> firstRarity
                        && firstRarity.get("blocks") instanceof List<?> bl) {
                    blocksList = new ArrayList<>(bl);
                }
                map.set("blocks", blocksList);
                map.set("rarity", null);
            }
            map.set("mode", target.name());
            Files.writeString(file.toPath(), map.saveToString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getDebugger().error("Не удалось сменить режим шахты " + mineId, e);
            return;
        }

        plugin.getMineManager().reloadOne(mineId);
    }
}

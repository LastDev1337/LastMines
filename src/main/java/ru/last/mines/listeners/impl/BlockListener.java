package ru.last.mines.listeners.impl;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import ru.last.mines.utils.ColorUtils;
import ru.last.mines.utils.Sounds;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import ru.last.mines.api.*;
import ru.last.mines.api.events.*;
import ru.last.mines.models.*;

import java.util.concurrent.ThreadLocalRandom;

public class BlockListener implements Listener {

    @SuppressWarnings("deprecation") private static final Enchantment FORTUNE = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("fortune"));

    public BlockListener() {}

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();
        Location loc = block.getLocation();

        Mine mine = LastMinesProvider.getApi().getMineAt(loc);
        if (mine != null && mine.isEnable()) {
            MineBlockBreakEvent apiEvent = new MineBlockBreakEvent(player, mine, block);
            Bukkit.getPluginManager().callEvent(apiEvent);
            if (apiEvent.isCancelled()) {
                e.setCancelled(true);
                return;
            }

            if (mine.isPermEnable() && !player.hasPermission(mine.getPermValue())) {
                e.setCancelled(true);
                for (String msg : mine.getPermMessages()) {
                    executePlayerAction(player, msg, mine);
                }
                return;
            }

            ItemStack tool = player.getInventory().getItemInMainHand();
            if (!mine.meetsEnchantRequirements(tool)) {
                e.setCancelled(true);
                for (String msg : mine.getEnchantFailActions()) {
                    executePlayerAction(player, msg, mine);
                }
                return;
            }

            mine.decrementPhysicalBlocks();

            Material blockType = block.getType();
            for (MineBlock mb : mine.getCurrentBlocks()) {
                if (mb.material() == blockType) {
                    if (mb.drops() != null && !mb.drops().isEmpty()) {
                        e.setDropItems(false);
                        int fortuneLevel = FORTUNE != null ? tool.getEnchantmentLevel(FORTUNE) : 0;
                        for (DropItem drop : mb.drops()) {
                            if (ThreadLocalRandom.current().nextDouble(100.0) >= drop.chance()) continue;
                            int quantity = 1;
                            if (drop.fortune() && fortuneLevel > 0) {
                                quantity += ThreadLocalRandom.current().nextInt(fortuneLevel + 1);
                            }
                            loc.getWorld().dropItemNaturally(loc, new ItemStack(drop.material(), quantity));
                        }
                    }
                    break;
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void executePlayerAction(Player player, String cleanAction, Mine mine) {
        cleanAction = mine.replacePlaceholders(cleanAction);
        if (cleanAction.startsWith("[message]")) {
            String msg = cleanAction.replaceFirst("\\[message]\\s*", "").replace("\\n", "\n");
            player.sendMessage(ColorUtils.colorString(msg));
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
            String title = titleStr;
            String subtitle = "";
            if (titleStr.contains("\\n")) {
                String[] split = titleStr.split("\\\\n");
                title = split[0];
                subtitle = split.length > 1 ? split[1] : "";
            } else if (titleStr.contains("\n")) {
                String[] split = titleStr.split("\n");
                title = split[0];
                subtitle = split.length > 1 ? split[1] : "";
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
            player.sendTitle(ColorUtils.colorString(title), ColorUtils.colorString(subtitle), fadeIn, stay, fadeOut);
        } else if (cleanAction.startsWith("[actionbar]")) {
            String raw = cleanAction.replaceFirst("\\[actionbar]\\s*", "");
            String[] split = raw.split(" ", 3);
            String msg = raw;
            if (split.length >= 3) {
                msg = split[2];
            }
            player.sendActionBar(ColorUtils.colorString(msg));
        } else if (cleanAction.startsWith("[sound]")) {
            String soundStr = cleanAction.replaceFirst("\\[sound]\\s*", "");
            try {
                String[] split = soundStr.split(" ");
                Sound sound = Sounds.parseSound(split[0]);
                if (sound != null) {
                    float volume = split.length > 1 ? Float.parseFloat(split[1]) : 1f;
                    float pitch = split.length > 2 ? Float.parseFloat(split[2]) : 1f;
                    player.playSound(player.getLocation(), sound, volume, pitch);
                }
            } catch (Exception ignored) {}
        } else {
            player.sendMessage(ColorUtils.colorString(cleanAction.replace("\\n", "\n")));
        }
    }
}

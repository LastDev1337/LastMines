package ru.last.mines.listeners.impl;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import ru.last.mines.mine.Mine;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import ru.last.mines.api.*;
import ru.last.mines.api.events.*;
import ru.last.mines.mine.models.*;

import java.util.concurrent.ThreadLocalRandom;

public class BlockListener implements Listener {

    @SuppressWarnings("deprecation") private static final Enchantment FORTUNE = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("fortune"));

    public BlockListener() {}

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent e) {
        Mine mine = LastMinesProvider.getApi().getMineAt(e.getBlockPlaced().getLocation());
        if (mine != null && mine.isEnable()) {
            mine.markPlaced(e.getBlockPlaced());
        }
    }

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
                mine.runPlayerActions(player, mine.getPermMessages());
                return;
            }

            ItemStack tool = player.getInventory().getItemInMainHand();
            if (!mine.meetsEnchantRequirements(tool)) {
                e.setCancelled(true);
                mine.runPlayerActions(player, mine.getEnchantFailActions());
                return;
            }

            if (mine.consumePlaced(block)) return;

            ru.last.mines.stats.StatsService stats = ru.last.mines.LastMines.get().getStatsService();
            if (stats != null) stats.record(player, block.getType());
            mine.decrementPhysicalBlocks();

            Material blockType = block.getType();
            mine.runBreakActions(player, blockType);
            mine.checkPercentReset();
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
}

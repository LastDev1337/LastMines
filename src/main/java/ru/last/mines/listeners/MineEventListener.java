package ru.last.mines.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ru.last.mines.api.events.*;
import ru.last.mines.LastMines;

public class MineEventListener implements Listener {
    private final LastMines plugin;

    public MineEventListener(LastMines plugin) { this.plugin = plugin; }

    @EventHandler
    public void onRarityUpdate(MineUpdateRarityEvent e) {
        String mineId = e.getMine().getId();
        String oldRarityId = e.getOldRarity() != null ? e.getOldRarity().id() : "none";
        String newRarityId = e.getNewRarity() != null ? e.getNewRarity().id() : "none";
        plugin.getDebugger().info("Mine " + mineId + " updated rarity from " + oldRarityId + " to " + newRarityId);
    }
}

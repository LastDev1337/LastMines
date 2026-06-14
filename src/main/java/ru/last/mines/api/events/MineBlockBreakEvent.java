package ru.last.mines.api.events;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.last.mines.models.Mine;

import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MineBlockBreakEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final Mine mine;
    private final Block block;
    private boolean isCancelled;

    public MineBlockBreakEvent(Player player, Mine mine, Block block) {
        this.player = player;
        this.mine = mine;
        this.block = block;
        this.isCancelled = false;
    }

    public Player getPlayer() { return player; }

    public Mine getMine() { return mine; }

    public Block getBlock() { return block; }

    @Override
    public boolean isCancelled() { return isCancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.isCancelled = cancel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }
}

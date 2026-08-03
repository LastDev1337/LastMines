package ru.last.mines.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.last.mines.models.Mine;
import org.jetbrains.annotations.NotNull;

public class MineTeleportEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Mine mine;
    private final Player player;
    private boolean cancelled = false;

    public MineTeleportEvent(Mine mine, Player player) {
        this.mine = mine;
        this.player = player;
    }

    public Mine getMine() { return mine; }
    public Player getPlayer() { return player; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public @NotNull HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}

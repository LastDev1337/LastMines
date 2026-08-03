package ru.last.mines.api.events;

import org.jetbrains.annotations.NotNull;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.last.mines.models.Mine;

public class MineDeleteEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Mine mine;
    private boolean cancelled = false;

    public MineDeleteEvent(Mine mine) { this.mine = mine; }

    public Mine getMine() { return mine; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

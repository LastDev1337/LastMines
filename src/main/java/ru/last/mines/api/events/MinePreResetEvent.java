package ru.last.mines.api.events;

import org.jetbrains.annotations.NotNull;
import ru.last.mines.models.Mine;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MinePreResetEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Mine mine;
    private boolean isCancelled;

    public MinePreResetEvent(Mine mine) {
        this.mine = mine;
        this.isCancelled = false;
    }

    public Mine getMine() { return mine; }

    @Override
    public boolean isCancelled() { return isCancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.isCancelled = cancel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }
}

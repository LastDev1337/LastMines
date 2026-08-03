package ru.last.mines.api.events;

import ru.last.mines.models.Mine;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MineResetEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Mine mine;

    public MineResetEvent(Mine mine) { this.mine = mine; }

    public Mine getMine() { return mine; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

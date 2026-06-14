package ru.last.mines.api.events;

import ru.last.mines.models.Mine;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import ru.last.mines.models.Mine;

public class MineUnloadEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Mine mine;

    public MineUnloadEvent(Mine mine) { this.mine = mine; }

    public Mine getMine() { return mine; }

    @Override
    public @NotNull HandlerList getHandlers() { return handlers; }
}

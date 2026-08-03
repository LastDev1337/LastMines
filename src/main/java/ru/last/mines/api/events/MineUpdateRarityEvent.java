package ru.last.mines.api.events;

import ru.last.mines.models.Mine;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import ru.last.mines.models.MineRarity;

public class MineUpdateRarityEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Mine mine;
    private final MineRarity oldRarity;
    private final MineRarity newRarity;

    public MineUpdateRarityEvent(Mine mine, MineRarity oldRarity, MineRarity newRarity) {
        this.mine = mine;
        this.oldRarity = oldRarity;
        this.newRarity = newRarity;
    }

    public Mine getMine() { return mine; }
    public MineRarity getOldRarity() { return oldRarity; }
    public MineRarity getNewRarity() { return newRarity; }
    @Override public @NotNull HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}

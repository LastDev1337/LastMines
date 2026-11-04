package ru.last.mines.api;

import org.bukkit.entity.Player;
import org.bukkit.Location;
import ru.last.mines.mine.Mine;

import java.util.Collection;

public interface LastMinesAPI {

    Mine getMine(String id);

    Collection<Mine> getMines();

    void openGui(Player player, String id);

    boolean isWithinRadius(Mine mine, Location location, double radius);

    Collection<Mine> getMinesInRadius(Location location, double radius);

    void resetMine(String id);

    Mine getMineAt(Location location);

    Mine getMineAt(Player player);
}

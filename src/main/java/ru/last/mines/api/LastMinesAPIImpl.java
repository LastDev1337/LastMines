package ru.last.mines.api;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import ru.last.mines.models.Mine;

import org.bukkit.Location;
import ru.last.mines.LastMines;

import java.util.Collection;

public class LastMinesAPIImpl implements LastMinesAPI {

    private final LastMines plugin;

    public LastMinesAPIImpl(LastMines plugin) { this.plugin = plugin; }

    @Override
    public Mine getMine(String id) { return plugin.getMineManager().getMine(id); }

    @Override
    public Collection<Mine> getMines() { return plugin.getMineManager().getMines().values(); }

    @Override
    public void openGui(Player player, String id) {
        if (plugin.getGuiManager() != null) {
            plugin.getGuiManager().openMenu(player, id);
        }
    }

    @Override
    public boolean isWithinRadius(Mine mine, Location location, double radius) {
        if (mine.getPos1() == null || mine.getPos2() == null) return false;
        if (location.getWorld() == null || !location.getWorld().equals(mine.getPos1().getWorld())) return false;

        Location center = new Location(location.getWorld(), 
            (mine.getPos1().getX() + mine.getPos2().getX()) / 2.0, 
            (mine.getPos1().getY() + mine.getPos2().getY()) / 2.0, 
            (mine.getPos1().getZ() + mine.getPos2().getZ()) / 2.0);
        return location.distance(center) <= radius;
    }

    @Override
    public Collection<Mine> getMinesInRadius(Location location, double radius) {
        List<Mine> found = new ArrayList<>();
        for (Mine mine : getMines()) {
            if (isWithinRadius(mine, location, radius)) {
                found.add(mine);
            }
        }
        return found;
    }

    @Override
    public void resetMine(String id) {
        Mine mine = getMine(id);
        if (mine != null) {
            mine.resetMine();
        }
    }

    @Override
    public Mine getMineAt(Location location) {
        for (Mine mine : getMines()) {
            if (mine.getPos1() != null && mine.getPos2() != null) {
                double minX = Math.min(mine.getPos1().getX(), mine.getPos2().getX());
                double maxX = Math.max(mine.getPos1().getX(), mine.getPos2().getX());
                double minY = Math.min(mine.getPos1().getY(), mine.getPos2().getY());
                double maxY = Math.max(mine.getPos1().getY(), mine.getPos2().getY());
                double minZ = Math.min(mine.getPos1().getZ(), mine.getPos2().getZ());
                double maxZ = Math.max(mine.getPos1().getZ(), mine.getPos2().getZ());

                if (location.getWorld() != null && location.getWorld().equals(mine.getPos1().getWorld())) {
                    if (location.getX() >= minX && location.getX() <= maxX &&
                        location.getY() >= minY && location.getY() <= maxY &&
                        location.getZ() >= minZ && location.getZ() <= maxZ) { return mine; }
                }
            }
        }
        return null;
    }

    @Override
    public Mine getMineAt(Player player) { return getMineAt(player.getLocation()); }
}

package ru.last.mines.hooks;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WEHook {

    public static Location[] getSelection(Player player) {
        try {
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
            if (session != null && session.getSelectionWorld() != null) {
                Region region = session.getSelection(session.getSelectionWorld());
                if (region != null) {
                    BlockVector3 min = region.getMinimumPoint();
                    BlockVector3 max = region.getMaximumPoint();
                    Location loc1 = new Location(player.getWorld(), min.getX(), min.getY(), min.getZ());
                    Location loc2 = new Location(player.getWorld(), max.getX(), max.getY(), max.getZ());
                    return new Location[]{loc1, loc2};
                }
            }
        } catch (IncompleteRegionException | NoClassDefFoundError ignored) {}
        return null;
    }
}

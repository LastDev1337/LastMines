package ru.last.mines.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.jetbrains.annotations.NotNull;
import org.bukkit.entity.Player;
import ru.last.mines.models.*;
import ru.last.mines.LastMines;
import ru.last.mines.utils.time.TimeFormatter;

public class PlaceHook extends PlaceholderExpansion {

    private final LastMines plugin;

    public PlaceHook(LastMines plugin) { this.plugin = plugin; }

    @Override public @NotNull String getIdentifier() { return "lastmines"; }
    @Override public @NotNull String getAuthor() { return "Last"; }
    @SuppressWarnings("deprecation") @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player != null && params.contains("{MINE_ID}")) {
            String guiMineId = plugin.getGuiManager().getViewingMines().get(player.getUniqueId());
            if (guiMineId != null) {
                params = params.replace("{MINE_ID}", guiMineId);
            }
        }
        
        if (params.startsWith("time_clock_")) {
            String id = params.replace("time_clock_", "");
            Mine mine = plugin.getMineManager().getMine(id);
            if (mine != null) return TimeFormatter.format(mine.getTimeLeft() * 1000L, "clock");
        } else if (params.startsWith("time_detail_")) {
            String id = params.replace("time_detail_", "");
            Mine mine = plugin.getMineManager().getMine(id);
            if (mine != null) return TimeFormatter.format(mine.getTimeLeft() * 1000L, "detail");
        } else if (params.startsWith("time_")) {
            String id = params.replace("time_", "");
            Mine mine = plugin.getMineManager().getMine(id);
            if (mine != null) return TimeFormatter.format(mine.getTimeLeft() * 1000L, "default");
        } else if (params.startsWith("rarity_current_")) {
            String id = params.replace("rarity_current_", "");
            Mine mine = plugin.getMineManager().getMine(id);
            if (mine != null) return mine.replacePlaceholders("%rarity_current%");
        } else if (params.startsWith("rarity_next_")) {
            String id = params.replace("rarity_next_", "");
            Mine mine = plugin.getMineManager().getMine(id);
            if (mine != null) return mine.replacePlaceholders("%rarity_next%");
        } else if (params.startsWith("blocks_current_count_")) {
            String id = params.replace("blocks_current_count_", "");
            Mine mine = plugin.getMineManager().getMine(id);
            if (mine != null) return String.valueOf(mine.getPhysicalBlocksCount());
        } else if (params.startsWith("blocks_max_count_")) {
            String id = params.replace("blocks_max_count_", "");
            Mine mine = plugin.getMineManager().getMine(id);
            if (mine != null) return String.valueOf(mine.getMaxPhysicalBlocksCount());
        } else if (params.startsWith("blocks_")) {
            String id = params.replace("blocks_", "");
            Mine mine = plugin.getMineManager().getMine(id);
            if (mine != null) return String.valueOf(mine.getCurrentBlocks().size());
        } else if (params.equals("count")) { return String.valueOf(plugin.getMineManager().getMines().size()); }
        
        return null;
    }
}

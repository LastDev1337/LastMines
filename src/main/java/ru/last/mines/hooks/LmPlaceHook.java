package ru.last.mines.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.last.mines.LastMines;

public class LmPlaceHook extends PlaceholderExpansion {

    private final LastMines plugin;

    public LmPlaceHook(LastMines plugin) { this.plugin = plugin; }

    @Override public @NotNull String getIdentifier() { return "lm"; }
    @Override public @NotNull String getAuthor() { return "Last"; }
    @SuppressWarnings("deprecation") @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        String mapped = params;
        String lower = params.toLowerCase(java.util.Locale.ROOT);
        if (lower.equals("blocks_total")) mapped = "stats_total";
        else if (lower.equals("rank")) mapped = "stats_rank";
        else if (lower.startsWith("rank_")) mapped = "stats_" + params;
        else if (lower.startsWith("blocks_")) mapped = "stats_" + params.substring("blocks_".length());
        return StatsPlaceholders.resolve(plugin, player, mapped);
    }
}

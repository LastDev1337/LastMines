package ru.last.mines.hooks;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.last.mines.LastMines;
import ru.last.mines.stats.PlayerStats;
import ru.last.mines.stats.StatsService;
import ru.last.mines.stats.TopEntry;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class StatsPlaceholders {

    private static final Pattern TOP = Pattern.compile("^top_(\\d+)_(name|value)(?:_(.+))?$", Pattern.CASE_INSENSITIVE);

    private StatsPlaceholders() {}

    static String resolve(LastMines plugin, Player player, String params) {
        StatsService stats = plugin.getStatsService();
        if (stats == null) return null;
        String p = params.toLowerCase(Locale.ROOT);

        Matcher m = TOP.matcher(params);
        if (m.matches()) {
            boolean name = m.group(2).equalsIgnoreCase("name");
            Material material = null;
            if (m.group(3) != null) {
                material = material(m.group(3));
                if (material == null) return name ? "-" : "0";
            }
            List<TopEntry> top = stats.cachedTop(material);
            int place = parseInt(m.group(1));
            if (place < 1 || place > top.size()) return name ? "-" : "0";
            TopEntry e = top.get(place - 1);
            return name ? e.name() : String.valueOf(e.value());
        }

        if (p.equals("stats_total")) {
            PlayerStats s = player == null ? null : stats.cachedStats(player.getUniqueId());
            return s == null ? "0" : String.valueOf(s.total());
        }
        if (p.equals("stats_rank") || p.startsWith("stats_rank_")) {
            Material material = null;
            if (p.length() > "stats_rank_".length()) {
                material = material(params.substring("stats_rank_".length()));
                if (material == null) return "-";
            }
            if (player == null) return "-";
            List<TopEntry> top = stats.cachedTop(material);
            for (int i = 0; i < top.size(); i++) {
                if (top.get(i).uuid().equals(player.getUniqueId())) return String.valueOf(i + 1);
            }
            return "-";
        }
        if (p.startsWith("stats_")) {
            Material material = material(params.substring("stats_".length()));
            if (material == null) return null;
            PlayerStats s = player == null ? null : stats.cachedStats(player.getUniqueId());
            return s == null ? "0" : String.valueOf(s.amount(material));
        }
        return null;
    }

    private static Material material(String raw) {
        return Material.matchMaterial(raw.trim().toUpperCase(Locale.ROOT));
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }
}

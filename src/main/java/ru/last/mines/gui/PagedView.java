package ru.last.mines.gui;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PagedView {
    private final Map<UUID, Integer> pageByPlayer = new HashMap<>();
    private final Map<UUID, Integer> maxPageByPlayer = new HashMap<>();

    public void changePage(Player player, int delta) {
        UUID uuid = player.getUniqueId();
        int current = pageByPlayer.getOrDefault(uuid, 0);
        int max = maxPageByPlayer.getOrDefault(uuid, 0);
        pageByPlayer.put(uuid, Math.clamp(current + delta, 0, max));
    }

    public int resolvePage(UUID uuid, int itemCount, int pageSize) {
        int maxPage = Math.max(0, (itemCount - 1) / Math.max(1, pageSize));
        maxPageByPlayer.put(uuid, maxPage);
        int page = Math.min(pageByPlayer.getOrDefault(uuid, 0), maxPage);
        pageByPlayer.put(uuid, page);
        return page;
    }

    public int maxPage(UUID uuid) {
        return maxPageByPlayer.getOrDefault(uuid, 0);
    }
}

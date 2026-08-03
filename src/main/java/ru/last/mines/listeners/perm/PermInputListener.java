package ru.last.mines.listeners.perm;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import ru.last.mines.LastMines;
import ru.last.mines.api.LastMinesProvider;
import ru.last.mines.models.Mine;

import java.util.*;

public class PermInputListener implements Listener {

    private final Map<UUID, InputSession> activeSessions = new HashMap<>();

    public void startSession(Player player, String mineId, String type) {
        UUID uuid = player.getUniqueId();
        if (activeSessions.containsKey(uuid)) {
            activeSessions.get(uuid).task.cancel();
        }

        BukkitTask task = Bukkit.getScheduler().runTaskLater(LastMines.get(), () -> {
            if (activeSessions.remove(uuid) != null) {
                LastMines.get().getConfigManager().getMessages().getInputTimeout().send(player);
            }
        }, 20 * 60L);

        activeSessions.put(uuid, new InputSession(mineId, type, task));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        InputSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) return;

        event.setCancelled(true);
        session.task.cancel();

        String msg = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        Bukkit.getScheduler().runTask(LastMines.get(), () -> {
            Mine mine = LastMinesProvider.getApi().getMine(session.mineId);
            if (mine == null) return;

            if ("value".equalsIgnoreCase(session.type)) {
                mine.setPermValue(msg);
                player.sendMessage("§aУспешно: §7Право доступа для шахты §e" + session.mineId + " §7изменено на §f" + msg + "§7!");
            }

            mine.save();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lastmines gui " + session.mineId + " permissions " + player.getName());
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        InputSession session = activeSessions.remove(event.getPlayer().getUniqueId());
        if (session != null) {
            session.task.cancel();
        }
    }

    private record InputSession(String mineId, String type, BukkitTask task) {}
}

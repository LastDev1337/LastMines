package ru.last.mines.listeners.text;

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
import ru.last.mines.utils.ColorUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class TextInputListener implements Listener {

    private record InputSession(Consumer<String> onSubmit, BukkitTask task) {}

    private final Map<UUID, InputSession> activeSessions = new HashMap<>();

    public void startSession(Player player, String prompt, Consumer<String> onSubmit) {
        UUID uuid = player.getUniqueId();
        InputSession previous = activeSessions.remove(uuid);
        if (previous != null) previous.task().cancel();

        player.sendMessage(ColorUtils.colorString(prompt));

        BukkitTask task = Bukkit.getScheduler().runTaskLater(LastMines.get(), () -> {
            if (activeSessions.remove(uuid) != null) {
                LastMines.get().getConfigManager().getMessages().getInputTimeout().send(player);
            }
        }, 20 * 60L);

        activeSessions.put(uuid, new InputSession(onSubmit, task));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        InputSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) return;

        event.setCancelled(true);
        session.task().cancel();

        String msg = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Bukkit.getScheduler().runTask(LastMines.get(), () -> session.onSubmit().accept(msg));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        InputSession session = activeSessions.remove(event.getPlayer().getUniqueId());
        if (session != null) session.task().cancel();
    }
}

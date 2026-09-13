package ru.last.mines.listeners.limit;

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
import ru.last.mines.models.MineBlock;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LimitInputListener implements Listener {

    private final Map<UUID, InputSession> activeSessions = new ConcurrentHashMap<>();

    public void startSession(Player player, String mineId, String material) {
        UUID uuid = player.getUniqueId();
        if (activeSessions.containsKey(uuid)) {
            activeSessions.get(uuid).task.cancel();
        }

        LastMines.get().getConfigManager().getMessages().getInputLimit().send(player);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(LastMines.get(), () -> {
            if (activeSessions.remove(uuid) != null) {
                LastMines.get().getConfigManager().getMessages().getInputTimeout().send(player);
            }
        }, 20 * 60L);

        activeSessions.put(uuid, new InputSession(mineId, material, task));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        InputSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) return;

        event.setCancelled(true);
        session.task.cancel();

        String msg = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        try {
            int limit = Integer.parseInt(msg);
            if (limit < 1 || limit > 64) throw new NumberFormatException();

            Bukkit.getScheduler().runTask(LastMines.get(), () -> {
                Mine mine = LastMinesProvider.getApi().getMine(session.mineId);
                if (mine == null) return;

                List<MineBlock> blocks = mine.getCurrentBlocks();
                MineBlock target = null;
                for (MineBlock mb : blocks) {
                    if (mb.material().name().equals(session.material)) {
                        target = mb;
                        break;
                    }
                }

                if (target != null) {
                    MineBlock newBlock = new MineBlock(target.material(), target.chance(), target.min(), limit, target.drops());
                    blocks.remove(target);
                    blocks.add(newBlock);
                }

                mine.save();
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lastmines gui " + session.mineId + " block_settings " + player.getName() + " " + session.material);
            });

        } catch (NumberFormatException e) {
            LastMines.get().getConfigManager().getMessages().getInvalidNumber().send(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        InputSession session = activeSessions.remove(event.getPlayer().getUniqueId());
        if (session != null) {
            session.task.cancel();
        }
    }

    private record InputSession(String mineId, String material, BukkitTask task) {}
}

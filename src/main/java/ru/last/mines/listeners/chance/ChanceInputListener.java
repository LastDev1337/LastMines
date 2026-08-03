package ru.last.mines.listeners.chance;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

import java.util.*;

public class ChanceInputListener implements Listener {

    private final Map<UUID, InputSession> activeSessions = new HashMap<>();

    public void startSession(Player player, String mineId, String material) {
        UUID uuid = player.getUniqueId();
        if (activeSessions.containsKey(uuid)) {
            activeSessions.get(uuid).task.cancel();
        }

        LastMines.get().getConfigManager().getMessages().getInputChance().send(player);

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
            double chance = Double.parseDouble(msg);
            if (chance < 0 || chance > 100) throw new NumberFormatException();

            Bukkit.getScheduler().runTask(LastMines.get(), () -> {
                Mine mine = LastMinesProvider.getApi().getMine(session.mineId);
                if (mine == null) return;

                List<MineBlock> blocks = mine.getCurrentBlocks();
                double total = 0;
                MineBlock target = null;
                for (MineBlock mb : blocks) {
                    if (mb.material().name().equals(session.material)) {
                        target = mb;
                    } else {
                        total += mb.chance();
                    }
                }

                if (total + chance > 100.0) {
                    LastMines.get().getConfigManager().getMessages().getChanceExceeded().send(player);
                    return;
                }

                if (target != null) {
                    MineBlock newBlock = new MineBlock(target.material(), chance, target.min(), target.max(), target.drops());
                    blocks.remove(target);
                    blocks.add(newBlock);
                } else {
                    Material mat = Material.matchMaterial(session.material);
                    if (mat != null) {
                        blocks.add(new MineBlock(mat, chance, 0, 0, Collections.emptyList()));
                    }
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

package ru.last.mines.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.last.mines.api.*;
import ru.last.mines.api.events.*;
import ru.last.mines.models.*;
import ru.last.mines.LastMines;
import dev.by1337.bmenu.command.ExecuteContext;
import dev.by1337.bmenu.menu.command.MenuCommands;
import dev.by1337.cmd.Command;
import dev.by1337.cmd.argument.ArgumentString;
import dev.by1337.cmd.argument.ArgumentStrings;

public class ActionManager {

    private final LastMines plugin;

    public ActionManager(LastMines plugin) { this.plugin = plugin; }

    public void registerBMenuActions() {
        try {
            Command<ExecuteContext> root = MenuCommands.getCommands();

            root.sub(new Command<ExecuteContext>("[tp_mine]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentString<>("player"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [tp_mine] <mine> <player>");
                        String playerName = (String) args.getOrThrow("player", "Use: [tp_mine] <mine> <player>");
                        Player player = Bukkit.getPlayer(playerName);
                        if (player != null) teleportToMine(player, mine);
                    })
            );

            root.sub(new Command<ExecuteContext>("[reset_mine]")
                    .argument(new ArgumentString<>("mine"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [reset_mine] <mine>");
                        resetMine(mine);
                    })
            );

            root.sub(new Command<ExecuteContext>("[delete_mine]")
                    .argument(new ArgumentString<>("mine"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [delete_mine] <mine>");
                        deleteMine(mine);
                    })
            );

            root.sub(new Command<ExecuteContext>("[update_mine]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentStrings<>("opts"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [update_mine] <mine> [rarity] [next]");
                        String opts = args.containsKey("opts") ? (String) args.get("opts") : "";
                        updateMine(mine, opts.split(" "));
                    })
            );

        } catch (Exception ex) { 
            plugin.getDebugLogger().error("Failed to register custom BMenu actions in ActionManager", ex); 
        }
    }

    public void openGui(Player player, String mineId) { LastMinesProvider.getApi().openGui(player, mineId); }

    public void resetMine(String mineId) { LastMinesProvider.getApi().resetMine(mineId); }

    public void teleportToMine(Player player, String mineId) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine != null) {
            if (!mine.isTpEnable() || mine.getTpPos() == null) {
                player.sendMessage("§cТелепортация на эту автошахту отключена или позиция не установлена!");
                return;
            }
            MineTeleportEvent apiEvent = new MineTeleportEvent(mine, player);
            Bukkit.getPluginManager().callEvent(apiEvent);
            if (apiEvent.isCancelled()) return;

            player.teleport(mine.getTpPos());
            plugin.getConfigManager().getMessages().getTeleported().send(player, "%mine%", mineId);
        }
    } 
    public void deleteMine(String mineId) {
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine != null) {
            MineDeleteEvent apiEvent = new MineDeleteEvent(mine);
            Bukkit.getPluginManager().callEvent(apiEvent);
            if (apiEvent.isCancelled()) return;

            plugin.getMineManager().getMines().remove(mineId);
            java.io.File file = new java.io.File(plugin.getDataFolder() + "/mines", mineId + ".yml");
            if (file.exists()) file.delete();
            mine.stopTasks();
            mine.deleteHologram();
        }
    }

    public void updateMine(String mineId, String[] opts) {
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "lastmines update " + mineId + " " + String.join(" ", opts));
    }
}

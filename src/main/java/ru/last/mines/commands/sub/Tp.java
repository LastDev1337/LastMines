package ru.last.mines.commands.sub;

import dev.by1337.cmd.Command;
import dev.by1337.cmd.argument.ArgumentString;
import dev.laststudio.lib.api.command.CommandService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.last.mines.LastMines;
import ru.last.mines.commands.SuggestingArgument;
import ru.last.mines.models.Mine;
import ru.last.mines.utils.ColorUtils;

public final class Tp {
    private Tp() {}

    public static Command<CommandSender> build(LastMines plugin, CommandService cs) {
        return cs.command("teleport")
                .argument(new SuggestingArgument("id", () -> plugin.getMineManager().getMines().keySet()))
                .argument(new ArgumentString<CommandSender>("player"))
                .executor((sender, args) -> {
                    String id = (String) args.get("id");
                    if (id == null) {
                        sender.sendMessage(ColorUtils.colorString(plugin.getConfigManager().getMessages().getPrefix() + "Использование: /lastmines tp <id> [player]"));
                        return;
                    }
                    Mine mine = plugin.getMineManager().getMine(id);
                    if (mine == null) {
                        plugin.getConfigManager().getMessages().getMineNotFound().send(sender);
                        return;
                    }

                    String playerName = (String) args.get("player");
                    Player target = null;
                    if (playerName != null) {
                        target = Bukkit.getPlayer(playerName);
                    } else if (sender instanceof Player player) {
                        target = player;
                    }

                    if (target == null) {
                        sender.sendMessage(ColorUtils.colorString("<red>Игрок не найден или команда должна выполняться от имени игрока."));
                        return;
                    }

                    plugin.getActionManager().teleportToMine(target, id);
                });
    }
}

package ru.last.mines.commands.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.last.mines.commands.*;
import ru.last.mines.models.*;
import ru.last.mines.LastMines;

@SubCommand(name = "teleport", aliases = {"tp"})
public class Tp extends AbstractSubCommand {

    public Tp(LastMines plugin) { super(plugin); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessages().getPrefix() + "Использование: /lastmines tp <id> [player]");
            return;
        }

        String id = args[1];
        Mine mine = plugin.getMineManager().getMine(id);
        if (mine == null) {
            plugin.getConfigManager().getMessages().getMineNotFound().send(sender);
            return;
        }

        Player target = null;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
        } else if (sender instanceof Player player) { target = player; }

        if (target == null) {
            sender.sendMessage("§cИгрок не найден или команда должна выполняться от имени игрока.");
            return;
        }

        plugin.getActionManager().teleportToMine(target, id);
    }
}

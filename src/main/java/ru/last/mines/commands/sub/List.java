package ru.last.mines.commands.sub;

import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import ru.last.mines.api.*;
import ru.last.mines.commands.*;
import ru.last.mines.models.*;
import ru.last.mines.LastMines;

@SubCommand(name = "list")
public class List extends AbstractSubCommand {

    public List(LastMines plugin) { super(plugin); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        java.util.Collection<Mine> mines = plugin.getMineManager().getMines().values();

        if (args.length > 1 && sender instanceof Player player) {
            try {
                double radius = Double.parseDouble(args[1]);
                mines = LastMinesProvider.getApi().getMinesInRadius(player.getLocation(), radius);
            } catch (NumberFormatException ignored) {}
        }

        if (mines.isEmpty()) {
            plugin.getConfigManager().getMessages().getListEmpty().send(sender);
            return;
        }

        plugin.getConfigManager().getMessages().getListHeader().send(sender);
        for (Mine mine : mines) {
            plugin.getConfigManager().getMessages().getListFormat().send(sender, "%mine%", mine.getId(), "%time%", String.valueOf(mine.getTimeLeft()));
        }
    }
}

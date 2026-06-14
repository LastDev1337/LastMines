package ru.last.mines.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.last.mines.LastMines;
import ru.last.mines.api.*;
import ru.last.mines.commands.*;
import ru.last.mines.models.*;

import java.util.Collection;

@SubCommand(name = "near", aliases = {"n", "radius"})
public class Near extends AbstractSubCommand {

    public Near(LastMines plugin) { super(plugin); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько для игроков!");
            return;
        }

        Mine currentMine = LastMinesProvider.getApi().getMineAt(player);
        if (currentMine != null) {
            player.sendMessage("§aВы находитесь внутри шахты: §e" + currentMine.getId());
        }

        double radius = 50.0;
        if (args.length > 1) {
            try {
                radius = Double.parseDouble(args[1]);
            } catch (Exception ignored) {}
        }

        Collection<Mine> nearMines = LastMinesProvider.getApi().getMinesInRadius(player.getLocation(), radius);
        if (nearMines.isEmpty()) {
            player.sendMessage("§cВ радиусе " + radius + " блоков нет шахт.");
        } else {
            player.sendMessage("§aШахты в радиусе " + radius + " блоков:");
            for (Mine m : nearMines) {
                player.sendMessage("§8- §e" + m.getId());
            }
        }
    }
}

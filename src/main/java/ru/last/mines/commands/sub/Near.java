package ru.last.mines.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.last.mines.LastMines;
import ru.last.mines.api.*;
import ru.last.mines.commands.*;
import ru.last.mines.models.*;
import ru.last.mines.utils.ColorUtils;

import java.util.Collection;

@SubCommand(name = "near", aliases = {"n", "radius"})
public class Near extends AbstractSubCommand {

    public Near(LastMines plugin) { super(plugin); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getConfigManager().getMessages().getOnlyPlayers().send(sender);
            return;
        }

        Mine currentMine = LastMinesProvider.getApi().getMineAt(player);
        if (currentMine != null) {
            player.sendMessage(ColorUtils.colorString("<green>Вы находитесь внутри шахты: <yellow>" + currentMine.getId()));
        }

        double radius = 50.0;
        if (args.length > 1) {
            try {
                radius = Double.parseDouble(args[1]);
            } catch (Exception ignored) {}
        }

        Collection<Mine> nearMines = LastMinesProvider.getApi().getMinesInRadius(player.getLocation(), radius);
        if (nearMines.isEmpty()) {
            player.sendMessage(ColorUtils.colorString("<red>В радиусе " + radius + " блоков нет шахт."));
        } else {
            player.sendMessage(ColorUtils.colorString("<green>Шахты в радиусе " + radius + " блоков:"));
            for (Mine m : nearMines) {
                player.sendMessage(ColorUtils.colorString("&8- &e" + m.getId()));
            }
        }
    }
}

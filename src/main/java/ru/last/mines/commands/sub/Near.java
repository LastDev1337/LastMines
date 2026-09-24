package ru.last.mines.commands.sub;

import dev.by1337.cmd.Command;
import dev.by1337.cmd.argument.ArgumentString;
import dev.laststudio.lib.api.command.CommandService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.last.mines.LastMines;
import ru.last.mines.api.LastMinesProvider;
import ru.last.mines.models.Mine;
import ru.last.mines.utils.ColorUtils;

import java.util.Collection;

public final class Near {
    private Near() {}

    public static Command<CommandSender> build(LastMines plugin, CommandService cs) {
        return cs.command("near")
                .argument(new ArgumentString<CommandSender>("radius"))
                .executor((sender, args) -> {
                    if (!(sender instanceof Player player)) {
                        plugin.getConfigManager().getMessages().getOnlyPlayers().send(sender);
                        return;
                    }

                    Mine currentMine = LastMinesProvider.getApi().getMineAt(player);
                    if (currentMine != null) {
                        player.sendMessage(ColorUtils.colorString("<green>Вы находитесь внутри шахты: <yellow>" + currentMine.getId()));
                    }

                    double radius = 50.0;
                    String radiusStr = (String) args.get("radius");
                    if (radiusStr != null) {
                        try {
                            radius = Double.parseDouble(radiusStr);
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
                });
    }
}

package ru.last.mines.commands.sub;

import dev.by1337.cmd.Command;
import dev.by1337.cmd.argument.ArgumentString;
import dev.laststudio.lib.api.command.CommandService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.last.mines.LastMines;
import ru.last.mines.api.LastMinesProvider;
import ru.last.mines.models.Mine;

import java.util.Collection;

public final class List {
    private List() {}

    public static Command<CommandSender> build(LastMines plugin, CommandService cs) {
        return cs.command("list")
                .argument(new ArgumentString<CommandSender>("radius"))
                .executor((sender, args) -> {
                    Collection<Mine> mines = plugin.getMineManager().getMines().values();

                    String radiusStr = (String) args.get("radius");
                    if (radiusStr != null && sender instanceof Player player) {
                        try {
                            double radius = Double.parseDouble(radiusStr);
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
                });
    }
}

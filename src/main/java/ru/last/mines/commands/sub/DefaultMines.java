package ru.last.mines.commands.sub;

import dev.by1337.cmd.Command;
import dev.laststudio.lib.api.command.CommandService;
import org.bukkit.command.CommandSender;
import ru.last.mines.LastMines;
import ru.last.mines.utils.ColorUtils;

public final class DefaultMines {
    private DefaultMines() {}

    public static Command<CommandSender> build(LastMines plugin, CommandService cs) {
        return cs.command("defaultmine")
                .executor(sender -> {
                    plugin.getMineManager().extractDefaultMines();
                    sender.sendMessage(ColorUtils.colorString(plugin.getConfigManager().getMessages().getPrefix()
                            + "<green>Стандартные шахты (blocks, rarity) восстановлены."));
                    Reload.execute(plugin, sender);
                });
    }
}
